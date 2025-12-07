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
package consulo.execution.configuration;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.content.scope.SearchScope;
import consulo.execution.DefaultExecutionResult;
import consulo.execution.ExecutionResult;
import consulo.execution.ExecutionTarget;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.TextConsoleBuilder;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base implementation of {@link RunProfileState}. Takes care of putting together a process and a console and wrapping them into an
 * {@link ExecutionResult}. Does not contain any logic for actually starting the process.
 *
 * @see GeneralCommandLine
 */
public abstract class CommandLineState implements RunProfileState {
    private TextConsoleBuilder myConsoleBuilder;

    private final ExecutionEnvironment myEnvironment;

    protected CommandLineState(ExecutionEnvironment environment) {
        myEnvironment = environment;
        if (myEnvironment != null) {
            Project project = myEnvironment.getProject();
            SearchScope searchScope = ExecutionSearchScopeProvider.createSearchScope(project, myEnvironment.getRunProfile());
            myConsoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, searchScope);
        }
    }

    public ExecutionEnvironment getEnvironment() {
        return myEnvironment;
    }

    public RunnerSettings getRunnerSettings() {
        return myEnvironment.getRunnerSettings();
    }

    @Nonnull
    public ExecutionTarget getExecutionTarget() {
        return myEnvironment.getExecutionTarget();
    }

    public void addConsoleFilters(Filter... filters) {
        myConsoleBuilder.filters(filters);
    }

    @Override
    @Nonnull
    public ExecutionResult execute(@Nonnull Executor executor, @Nonnull ProgramRunner runner) throws ExecutionException {
        ProcessHandler processHandler = startProcess();
        ConsoleView console = createConsole(executor);
        if (console != null) {
            console.attachToProcess(processHandler);
        }
        return new DefaultExecutionResult(console, processHandler, createActions(console, processHandler, executor));
    }

    @Nullable
    protected ConsoleView createConsole(@Nonnull Executor executor) throws ExecutionException {
        TextConsoleBuilder builder = getConsoleBuilder();
        return builder != null ? builder.getConsole() : null;
    }

    /**
     * Starts the process.
     *
     * @return the handler for the running process
     * @throws ExecutionException if the execution failed.
     * @see GeneralCommandLine
     */
    @Nonnull
    protected abstract ProcessHandler startProcess() throws ExecutionException;

    protected AnAction[] createActions(ConsoleView console, ProcessHandler processHandler) {
        return createActions(console, processHandler, null);
    }

    protected AnAction[] createActions(ConsoleView console, ProcessHandler processHandler, Executor executor) {
        if (console == null || !console.canPause() || (executor != null && !DefaultRunExecutor.EXECUTOR_ID.equals(executor.getId()))) {
            return new AnAction[0];
        }
        return new AnAction[]{new PauseOutputAction(console, processHandler)};
    }

    public TextConsoleBuilder getConsoleBuilder() {
        return myConsoleBuilder;
    }

    public void setConsoleBuilder(TextConsoleBuilder consoleBuilder) {
        myConsoleBuilder = consoleBuilder;
    }

    protected static class PauseOutputAction extends ToggleAction implements DumbAware {
        private final ConsoleView myConsole;
        private final ProcessHandler myProcessHandler;

        public PauseOutputAction(ConsoleView console, ProcessHandler processHandler) {
            super(ExecutionLocalize.runConfigurationPauseOutputActionName(), LocalizeValue.absent(), PlatformIconGroup.actionsPause());
            myConsole = console;
            myProcessHandler = processHandler;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent event) {
            return myConsole.isOutputPaused();
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent event, boolean flag) {
            myConsole.setOutputPaused(flag);
            Application.get().invokeLater(() -> update(event));
        }

        @Override
        public void update(@Nonnull AnActionEvent event) {
            super.update(event);
            Presentation presentation = event.getPresentation();
            boolean isRunning = myProcessHandler != null && !myProcessHandler.isProcessTerminated();
            if (isRunning) {
                presentation.setEnabled(true);
            }
            else {
                if (!myConsole.canPause()) {
                    presentation.setEnabled(false);
                    return;
                }
                if (!myConsole.hasDeferredOutput()) {
                    presentation.setEnabled(false);
                }
                else {
                    presentation.setEnabled(true);
                    myConsole.performWhenNoDeferredOutput(() -> update(event));
                }
            }
        }
    }
}
