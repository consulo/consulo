/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.test.sm;

import consulo.application.dumb.DumbAware;
import consulo.disposer.Disposer;
import consulo.execution.action.Location;
import consulo.execution.configuration.CommandLineState;
import consulo.execution.configuration.ModuleRunConfiguration;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.TestLocationProvider;
import consulo.execution.test.sm.runner.*;
import consulo.execution.test.sm.ui.SMTRunnerConsoleView;
import consulo.execution.test.sm.ui.SMTRunnerUIActionsHandler;
import consulo.execution.test.sm.ui.SMTestRunnerResultsForm;
import consulo.execution.test.sm.ui.statistic.StatisticsPanel;
import consulo.execution.test.ui.BaseTestsOutputConsoleView;
import consulo.execution.ui.console.ConsoleView;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.io.URLUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Roman Chernyatchik
 */
public class SMTestRunnerConnectionUtil {
    private static final String TEST_RUNNER_DEBUG_MODE_PROPERTY = "idea.smrunner.debug";

    @SuppressWarnings("deprecation")
    public static class CompositeTestLocationProvider implements SMTestLocator {
        private final TestLocationProvider myPrimaryLocator;
        private final List<TestLocationProvider> myLocators;

        public CompositeTestLocationProvider(@Nullable TestLocationProvider primaryLocator) {
            myPrimaryLocator = primaryLocator;
            myLocators = TestLocationProvider.EP_NAME.getExtensionList();
        }

        @Nonnull
        @Override
        public List<Location> getLocation(
            @Nonnull String protocol,
            @Nonnull String path,
            @Nonnull Project project,
            @Nonnull GlobalSearchScope scope
        ) {
            boolean isDumbMode = DumbService.isDumb(project);

            if (myPrimaryLocator != null && (!isDumbMode || myPrimaryLocator instanceof DumbAware)) {
                List<Location> locations = myPrimaryLocator.getLocation(protocol, path, project);
                if (!locations.isEmpty()) {
                    return locations;
                }
            }

            if (URLUtil.FILE_PROTOCOL.equals(protocol)) {
                List<Location> locations = FileUrlProvider.INSTANCE.getLocation(protocol, path, project, scope);
                if (!locations.isEmpty()) {
                    return locations;
                }
            }

            for (TestLocationProvider provider : myLocators) {
                if (!isDumbMode || provider instanceof DumbAware) {
                    List<Location> locations = provider.getLocation(protocol, path, project);
                    if (!locations.isEmpty()) {
                        return locations;
                    }
                }
            }

            return Collections.emptyList();
        }
    }

    private SMTestRunnerConnectionUtil() {
    }

    /**
     * Creates Test Runner console component with test tree, console, statistics tabs
     * and attaches it to given Process handler.
     * <p>
     * You can use this method in run configuration's CommandLineState. You should
     * just override "execute" method of your custom command line state and return
     * test runner's console.
     * <p>
     * E.g: <pre>{@code
     * public class MyCommandLineState extends CommandLineState {
     * <p>
     *   // ...
     * <p>
     *   @Override
     *   public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
     *     ProcessHandler processHandler = startProcess();
     *     RunConfiguration runConfiguration = getConfiguration();
     *     ExecutionEnvironment environment = getEnvironment();
     *     TestConsoleProperties properties = new SMTRunnerConsoleProperties(runConfiguration, "xUnit", executor)
     *     ConsoleView console = SMTestRunnerConnectionUtil.createAndAttachConsole("xUnit", processHandler, properties, environment);
     *     return new DefaultExecutionResult(console, processHandler, createActions(console, processHandler));
     *   }
     * }
     * }</pre>
     * <p>
     * NB: For debug purposes please enable "debug mode". In this mode test runner will also validate
     * consistency of test events communication protocol and throw assertion errors. To enable debug mode
     * please set system property idea.smrunner.debug=true
     *
     * @param testFrameworkName Is used to store(project level) latest value of testTree/consoleTab splitter and other settings
     *                          and also will be mentioned in debug diagnostics
     * @param processHandler    Process handler
     * @param consoleProperties Console properties for test console actions
     * @return Console view
     * @throws ExecutionException If IDEA cannot execute process this exception will
     *                            be caught and shown in error message box
     */
    @Nonnull
    public static BaseTestsOutputConsoleView createAndAttachConsole(
        @Nonnull String testFrameworkName,
        @Nonnull ProcessHandler processHandler,
        @Nonnull TestConsoleProperties consoleProperties
    )
        throws ExecutionException {
        BaseTestsOutputConsoleView console = createConsole(testFrameworkName, consoleProperties);
        console.attachToProcess(processHandler);
        return console;
    }

    @Nonnull
    public static BaseTestsOutputConsoleView createConsole(
        @Nonnull String testFrameworkName,
        @Nonnull TestConsoleProperties consoleProperties
    ) {
        String splitterPropertyName = getSplitterPropertyName(testFrameworkName);
        SMTRunnerConsoleView consoleView = new SMTRunnerConsoleView(consoleProperties, splitterPropertyName);
        initConsoleView(consoleView, testFrameworkName);
        return consoleView;
    }

    @Nonnull
    public static String getSplitterPropertyName(@Nonnull String testFrameworkName) {
        return testFrameworkName + ".Splitter.Proportion";
    }

    public static void initConsoleView(@Nonnull SMTRunnerConsoleView consoleView, @Nonnull String testFrameworkName) {
        consoleView.addAttachToProcessListener(processHandler -> {
            TestConsoleProperties properties = consoleView.getProperties();

            TestProxyPrinterProvider printerProvider = null;
            if (properties instanceof SMTRunnerConsoleProperties runnerConsoleProperties) {
                TestProxyFilterProvider filterProvider = runnerConsoleProperties.getFilterProvider();
                if (filterProvider != null) {
                    printerProvider = new TestProxyPrinterProvider(consoleView, filterProvider);
                }
            }

            SMTestLocator testLocator = FileUrlProvider.INSTANCE;
            if (properties instanceof SMTRunnerConsoleProperties runnerConsoleProperties) {
                SMTestLocator customLocator = runnerConsoleProperties.getTestLocator();
                if (customLocator != null) {
                    testLocator = new CombinedTestLocator(customLocator);
                }
            }

            boolean idBasedTestTree = false;
            if (properties instanceof SMTRunnerConsoleProperties runnerConsoleProperties) {
                idBasedTestTree = runnerConsoleProperties.isIdBasedTestTree();
            }

            SMTestRunnerResultsForm resultsForm = consoleView.getResultsViewer();
            attachEventsProcessors(
                properties,
                resultsForm,
                resultsForm.getStatisticsPane(),
                processHandler,
                testFrameworkName,
                testLocator,
                idBasedTestTree,
                printerProvider
            );
        });
        consoleView.setHelpId("reference.runToolWindow.testResultsTab");
        consoleView.initUI();
    }

    /**
     * In debug mode SM Runner will check events consistency. All errors will be reported using IDEA errors logger.
     * This mode must be disabled in production. The most widespread false positives were detected when you debug tests.
     * In such cases Test Framework may fire events several times, etc.
     *
     * @return true if in debug mode, otherwise false.
     */
    public static boolean isInDebugMode() {
        return Boolean.valueOf(System.getProperty(TEST_RUNNER_DEBUG_MODE_PROPERTY));
    }

    private static ProcessHandler attachEventsProcessors(
        TestConsoleProperties consoleProperties,
        SMTestRunnerResultsForm resultsViewer,
        StatisticsPanel statisticsPane,
        ProcessHandler processHandler,
        String testFrameworkName,
        @Nullable SMTestLocator locator,
        boolean idBasedTestTree,
        @Nullable TestProxyPrinterProvider printerProvider
    ) {
        // build messages consumer
        OutputToGeneralTestEventsConverter outputConsumer;
        if (consoleProperties instanceof SMCustomMessagesParsing customMessagesParsing) {
            outputConsumer = customMessagesParsing.createTestEventsConverter(testFrameworkName, consoleProperties);
        }
        else {
            outputConsumer = new OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties);
        }

        // events processor
        GeneralTestEventsProcessor eventsProcessor;
        if (idBasedTestTree) {
            eventsProcessor = new GeneralIdBasedToSMTRunnerEventsConvertor(
                consoleProperties.getProject(),
                resultsViewer.getTestsRootNode(),
                testFrameworkName
            );
        }
        else {
            eventsProcessor =
                new GeneralToSMTRunnerEventsConvertor(consoleProperties.getProject(), resultsViewer.getTestsRootNode(), testFrameworkName);
        }

        if (locator != null) {
            eventsProcessor.setLocator(locator);
        }

        if (printerProvider != null) {
            eventsProcessor.setPrinterProvider(printerProvider);
        }

        // UI actions
        SMTRunnerUIActionsHandler uiActionsHandler = new SMTRunnerUIActionsHandler(consoleProperties);

        // subscribe to events

        // subscribes event processor on output consumer events
        outputConsumer.setProcessor(eventsProcessor);
        // subscribes result viewer on event processor
        eventsProcessor.addEventsListener(resultsViewer);
        // subscribes test runner's actions on results viewer events
        resultsViewer.addEventsListener(uiActionsHandler);

        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(ProcessEvent event) {
                outputConsumer.flushBufferOnProcessTermination(event.getExitCode());
                eventsProcessor.onFinishTesting();

                Disposer.dispose(eventsProcessor);
                Disposer.dispose(outputConsumer);
            }

            @Override
            public void startNotified(ProcessEvent event) {
                eventsProcessor.onStartTesting();
                outputConsumer.onStartTesting();
            }

            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                outputConsumer.process(event.getText(), outputType);
            }
        });

        return processHandler;
    }

    private static class CombinedTestLocator implements SMTestLocator, DumbAware {
        private final SMTestLocator myLocator;

        public CombinedTestLocator(SMTestLocator locator) {
            myLocator = locator;
        }

        @Nonnull
        @Override
        public List<Location> getLocation(
            @Nonnull String protocol,
            @Nonnull String path,
            @Nonnull Project project,
            @Nonnull GlobalSearchScope scope
        ) {
            if (URLUtil.FILE_PROTOCOL.equals(protocol)) {
                return FileUrlProvider.INSTANCE.getLocation(protocol, path, project, scope);
            }
            else if (!DumbService.isDumb(project) || DumbService.isDumbAware(myLocator)) {
                return myLocator.getLocation(protocol, path, project, scope);
            }
            else {
                return Collections.emptyList();
            }
        }
    }

    /**
     * @deprecated use {@link #createAndAttachConsole(String, ProcessHandler, TestConsoleProperties)} (to be removed in IDEA 16)
     */
    @SuppressWarnings({"unused", "deprecation"})
    public static BaseTestsOutputConsoleView createAndAttachConsole(
        @Nonnull String testFrameworkName,
        @Nonnull ProcessHandler processHandler,
        @Nonnull TestConsoleProperties consoleProperties,
        ExecutionEnvironment environment
    ) throws ExecutionException {
        BaseTestsOutputConsoleView console = createConsole(testFrameworkName, consoleProperties, environment);
        console.attachToProcess(processHandler);
        return console;
    }

    /**
     * @deprecated use {@link SMTestRunnerConnectionUtil#createConsole(String, TestConsoleProperties)} instead (to be removed in IDEA 16)
     */
    @SuppressWarnings({"unused", "deprecation"})
    public static BaseTestsOutputConsoleView createConsole(
        @Nonnull String testFrameworkName,
        @Nonnull TestConsoleProperties consoleProperties,
        ExecutionEnvironment environment
    ) {
        return createConsoleWithCustomLocator(testFrameworkName, consoleProperties, environment, null);
    }

    /**
     * @deprecated use {@link #createConsole(String, TestConsoleProperties)} (to be removed in IDEA 16)
     */
    @SuppressWarnings({"unused", "deprecation"})
    public static BaseTestsOutputConsoleView createConsoleWithCustomLocator(
        @Nonnull String testFrameworkName,
        @Nonnull TestConsoleProperties consoleProperties,
        ExecutionEnvironment environment,
        @Nullable TestLocationProvider locator
    ) {
        return createConsoleWithCustomLocator(testFrameworkName, consoleProperties, environment, locator, false, null);
    }

    /**
     * @deprecated use {@link #createConsole(String, TestConsoleProperties)} (to be removed in IDEA 16)
     */
    @SuppressWarnings({"unused", "deprecation"})
    public static SMTRunnerConsoleView createConsoleWithCustomLocator(
        @Nonnull String testFrameworkName,
        @Nonnull TestConsoleProperties consoleProperties,
        ExecutionEnvironment environment,
        @Nullable TestLocationProvider locator,
        boolean idBasedTreeConstruction,
        @Nullable TestProxyFilterProvider filterProvider
    ) {
        String splitterPropertyName = getSplitterPropertyName(testFrameworkName);
        SMTRunnerConsoleView consoleView = new SMTRunnerConsoleView(consoleProperties, splitterPropertyName);
        initConsoleView(consoleView, testFrameworkName, locator, idBasedTreeConstruction, filterProvider);
        return consoleView;
    }

    /**
     * @deprecated use {@link #initConsoleView(SMTRunnerConsoleView, String)} (to be removed in IDEA 16)
     */
    @SuppressWarnings({"unused", "deprecation"})
    public static void initConsoleView(
        @Nonnull SMTRunnerConsoleView consoleView,
        @Nonnull String testFrameworkName,
        @Nullable TestLocationProvider locator,
        boolean idBasedTreeConstruction,
        @Nullable TestProxyFilterProvider filterProvider
    ) {
        consoleView.addAttachToProcessListener(processHandler -> {
            TestConsoleProperties properties = consoleView.getProperties();

            SMTestLocator testLocator = new CompositeTestLocationProvider(locator);

            TestProxyPrinterProvider printerProvider = null;
            if (filterProvider != null) {
                printerProvider = new TestProxyPrinterProvider(consoleView, filterProvider);
            }

            SMTestRunnerResultsForm resultsForm = consoleView.getResultsViewer();
            attachEventsProcessors(
                properties,
                resultsForm,
                resultsForm.getStatisticsPane(),
                processHandler,
                testFrameworkName,
                testLocator,
                idBasedTreeConstruction,
                printerProvider
            );
        });
        consoleView.setHelpId("reference.runToolWindow.testResultsTab");
        consoleView.initUI();
    }

    /**
     * @deprecated use {@link #createAndAttachConsole(String, ProcessHandler, TestConsoleProperties)} (to be removed in IDEA 16)
     */
    @SuppressWarnings({"unused", "deprecation"})
    public static ConsoleView createAndAttachConsole(
        @Nonnull String testFrameworkName,
        @Nonnull ProcessHandler processHandler,
        @Nonnull CommandLineState commandLineState,
        @Nonnull ModuleRunConfiguration config,
        @Nonnull Executor executor
    ) throws ExecutionException {
        TestConsoleProperties consoleProperties = new SMTRunnerConsoleProperties(config, testFrameworkName, executor);
        return createAndAttachConsole(testFrameworkName, processHandler, consoleProperties, commandLineState.getEnvironment());
    }

    /**
     * @deprecated use {@link #createConsole(String, TestConsoleProperties)} (to be removed in IDEA 16)
     */
    @SuppressWarnings({"unused", "deprecation"})
    public static ConsoleView createConsole(
        @Nonnull String testFrameworkName,
        @Nonnull CommandLineState commandLineState,
        @Nonnull ModuleRunConfiguration config,
        @Nonnull Executor executor
    )
        throws ExecutionException {
        TestConsoleProperties consoleProperties = new SMTRunnerConsoleProperties(config, testFrameworkName, executor);
        return createConsole(testFrameworkName, consoleProperties, commandLineState.getEnvironment());
    }
}
