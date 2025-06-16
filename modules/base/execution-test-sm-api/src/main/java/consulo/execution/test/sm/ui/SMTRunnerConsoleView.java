/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.execution.test.sm.ui;

import consulo.application.Application;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.*;
import consulo.execution.test.sm.SMRunnerUtil;
import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.execution.test.ui.BaseTestsOutputConsoleView;
import consulo.execution.test.ui.TestResultsPanel;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.process.ProcessHandler;
import consulo.util.collection.Lists;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Roman Chernyatchik
 */
public class SMTRunnerConsoleView extends BaseTestsOutputConsoleView {
    private SMTestRunnerResultsForm myResultsViewer;
    @Nullable
    private final String mySplitterProperty;
    private final List<AttachToProcessListener> myAttachToProcessListeners = Lists.newLockFreeCopyOnWriteList();

    /**
     * @deprecated
     */
    public SMTRunnerConsoleView(final TestConsoleProperties consoleProperties, final ExecutionEnvironment environment) {
        this(consoleProperties, environment, null);
    }

    /**
     * @param splitterProperty Key to store(project level) latest value of testTree/consoleTab splitter. E.g. "RSpec.Splitter.Proportion"
     * @deprecated
     */
    @SuppressWarnings("UnusedParameters")
    public SMTRunnerConsoleView(
        final TestConsoleProperties consoleProperties,
        final ExecutionEnvironment environment,
        @Nullable final String splitterProperty
    ) {
        super(consoleProperties, null);
        mySplitterProperty = splitterProperty;
    }

    public SMTRunnerConsoleView(final TestConsoleProperties consoleProperties) {
        this(consoleProperties, (String) null);
    }

    /**
     * @param splitterProperty Key to store(project level) latest value of testTree/consoleTab splitter. E.g. "RSpec.Splitter.Proportion"
     */
    public SMTRunnerConsoleView(
        final TestConsoleProperties consoleProperties,
        @Nullable final String splitterProperty
    ) {
        super(consoleProperties, null);
        mySplitterProperty = splitterProperty;
    }

    @Override
    protected TestResultsPanel createTestResultsPanel() {
        // Results View
        myResultsViewer = new SMTestRunnerResultsForm(
            getConsole().getComponent(),
            getConsole().createConsoleActions(),
            myProperties,
            mySplitterProperty
        );
        return myResultsViewer;
    }

    @Override
    public void initUI() {
        super.initUI();

        // Console
        myResultsViewer.addEventsListener(new TestResultsViewer.SMEventsAdapter() {
            @Override
            public void onSelected(
                @Nullable final SMTestProxy selectedTestProxy,
                @Nonnull final TestResultsViewer viewer,
                @Nonnull final TestFrameworkRunningModel model
            ) {
                if (selectedTestProxy == null) {
                    return;
                }

                // print selected content
                SMRunnerUtil.runInEventDispatchThread(
                    () -> getPrinter().updateOnTestSelected(selectedTestProxy),
                    Application.get().getNoneModalityState()
                );
            }
        });
    }

    public SMTestRunnerResultsForm getResultsViewer() {
        return myResultsViewer;
    }

    /**
     * Prints a given string of a given type on the root node.
     * Note: it's a permanent printing, as opposed to calling the same method on {@link #getConsole()} instance.
     *
     * @param s           given string
     * @param contentType given type
     */
    @Override
    public void print(@Nonnull final String s, @Nonnull final ConsoleViewContentType contentType) {
        myResultsViewer.getRoot().addLast(printer -> printer.print(s, contentType));
    }

    /**
     * Prints a given hyperlink on the root node.
     * Note: it's a permanent printing, as opposed to calling the same method on {@link #getConsole()} instance.
     *
     * @param hyperlinkText hyperlink text
     * @param info          HyperlinkInfo
     */
    @Override
    public void printHyperlink(String hyperlinkText, HyperlinkInfo info) {
        myResultsViewer.getRoot().addLast(new HyperLink(hyperlinkText, info));
    }

    @Override
    public void attachToProcess(ProcessHandler processHandler) {
        super.attachToProcess(processHandler);
        for (AttachToProcessListener listener : myAttachToProcessListeners) {
            listener.onAttachToProcess(processHandler);
        }
    }

    public void addAttachToProcessListener(@Nonnull AttachToProcessListener listener) {
        myAttachToProcessListeners.add(listener);
    }

    public void remoteAttachToProcessListener(@Nonnull AttachToProcessListener listener) {
        myAttachToProcessListeners.remove(listener);
    }

    @Override
    public void dispose() {
        myAttachToProcessListeners.clear();
        super.dispose();
    }
}
