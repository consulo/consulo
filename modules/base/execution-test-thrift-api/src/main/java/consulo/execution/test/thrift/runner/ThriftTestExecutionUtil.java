/*
 * Copyright 2013-2016 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.execution.test.thrift.runner;

import consulo.application.ApplicationManager;
import consulo.disposer.Disposer;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.TestLocationProvider;
import consulo.execution.test.sm.SMTestRunnerConnectionUtil;
import consulo.execution.test.sm.runner.*;
import consulo.execution.test.sm.ui.AttachToProcessListener;
import consulo.execution.test.sm.ui.SMTRunnerConsoleView;
import consulo.execution.test.sm.ui.SMTRunnerUIActionsHandler;
import consulo.execution.test.sm.ui.SMTestRunnerResultsForm;
import consulo.execution.test.sm.ui.statistic.StatisticsPanel;
import consulo.execution.test.ui.BaseTestsOutputConsoleView;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.util.dataholder.Key;
import consulo.util.io.NetUtil;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.InetSocketAddress;

/**
 * @author VISTALL
 * @since 18.05.14
 */
public class ThriftTestExecutionUtil {
  public static BaseTestsOutputConsoleView createConsoleWithCustomLocator(@Nonnull String testFrameworkName,
                                                                          @Nonnull TestConsoleProperties consoleProperties,
                                                                          @Nonnull ExecutionEnvironment environment,
                                                                          @Nonnull ThriftTestHandlerFactory factory,
                                                                          @Nullable TestLocationProvider locator) {
    return createConsoleWithCustomLocator(testFrameworkName, consoleProperties, environment, locator, factory, null);
  }

  public static SMTRunnerConsoleView createConsoleWithCustomLocator(@Nonnull String testFrameworkName,
                                                                    @Nonnull TestConsoleProperties consoleProperties,
                                                                    ExecutionEnvironment environment,
                                                                    @Nullable TestLocationProvider locator,
                                                                    ThriftTestHandlerFactory factory,
                                                                    @Nullable TestProxyFilterProvider filterProvider) {
    String splitterPropertyName = SMTestRunnerConnectionUtil.getSplitterPropertyName(testFrameworkName);
    SMTRunnerConsoleView consoleView = new SMTRunnerConsoleView(consoleProperties, splitterPropertyName);
    initConsoleView(consoleView, testFrameworkName, locator, factory, filterProvider);
    return consoleView;
  }

  public static void initConsoleView(@Nonnull final SMTRunnerConsoleView consoleView,
                                     @Nonnull final String testFrameworkName,
                                     @Nullable final TestLocationProvider locator,
                                     final ThriftTestHandlerFactory factory,
                                     @Nullable final TestProxyFilterProvider filterProvider) {
    consoleView.addAttachToProcessListener(new AttachToProcessListener() {
      @Override
      public void onAttachToProcess(@Nonnull ProcessHandler processHandler) {
        TestProxyPrinterProvider printerProvider = null;
        if (filterProvider != null) {
          printerProvider = new TestProxyPrinterProvider(consoleView, filterProvider);
        }
        SMTestRunnerResultsForm resultsForm = consoleView.getResultsViewer();
        attachEventsProcessors(consoleView.getProperties(), resultsForm, resultsForm.getStatisticsPane(), processHandler, testFrameworkName, locator, factory,
                               printerProvider);
      }
    });
    consoleView.setHelpId("reference.runToolWindow.testResultsTab");
    consoleView.initUI();
  }

  private static void attachEventsProcessors(@Nonnull TestConsoleProperties consoleProperties,
                                             SMTestRunnerResultsForm resultsViewer,
                                             StatisticsPanel statisticsPane,
                                             ProcessHandler processHandler,
                                             @Nonnull String testFrameworkName,
                                             @Nullable TestLocationProvider locator,
                                             ThriftTestHandlerFactory factory,
                                             @Nullable TestProxyPrinterProvider printerProvider) {
    //build messages consumer
    final OutputToGeneralTestEventsConverter outputConsumer;
    outputConsumer = new OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties);


    //events processor
    final GeneralTestEventsProcessor eventsProcessor =
            new GeneralToSMTRunnerEventsConvertor(consoleProperties.getProject(), resultsViewer.getTestsRootNode(), testFrameworkName);

    final TServer open = open(factory.getPort(), factory.createHandler(eventsProcessor));
    if (locator != null) {
      eventsProcessor.setLocator(new SMTestRunnerConnectionUtil.CompositeTestLocationProvider(locator));
    }
    if (printerProvider != null) {
      eventsProcessor.setPrinterProvider(printerProvider);
    }

    // ui actions
    SMTRunnerUIActionsHandler uiActionsHandler = new SMTRunnerUIActionsHandler(consoleProperties);

    // subscribe on events

    // subscribes event processor on output consumer events
    outputConsumer.setProcessor(eventsProcessor);
    // subscribes result viewer on event processor
    eventsProcessor.addEventsListener(resultsViewer);
    // subscribes test runner's actions on results viewer events
    resultsViewer.addEventsListener(uiActionsHandler);
    // subscribes statistics tab viewer on event processor
    eventsProcessor.addEventsListener(statisticsPane.createTestEventsListener());


    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(ProcessEvent event) {
        outputConsumer.flushBufferOnProcessTermination(event.getExitCode());
        try {
          open.stop();
        }
        catch (Exception ignored) {
        }
        eventsProcessor.onFinishTesting();

        Disposer.dispose(eventsProcessor);
        Disposer.dispose(outputConsumer);
      }

      @Override
      public void onTextAvailable(ProcessEvent event, Key outputType) {
        outputConsumer.process(event.getText(), outputType);
      }
    });
  }

  public static TServer open(int port, TestInterface.Iface iface) {
    TServerSocket localhost = null;
    try {
      localhost = new TServerSocket(new InetSocketAddress("localhost", port));
    }
    catch (TTransportException e) {
      throw new IllegalArgumentException(e);
    }

    TestInterface.Processor<TestInterface.Iface> processor = new TestInterface.Processor<TestInterface.Iface>(iface);

    TSimpleServer server = new TSimpleServer(new TServer.Args(localhost).processor(processor));
    ApplicationManager.getApplication().executeOnPooledThread((Runnable)server::serve);

    return server;
  }

  public static int getFreePort() {
    return NetUtil.tryToFindAvailableSocketPort();
  }
}
