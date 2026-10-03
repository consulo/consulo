/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ide.run;

import consulo.execution.DefaultExecutionResult;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.process.ExecutionException;
import consulo.process.NopProcessHandler;
import consulo.process.ProcessOutputType;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class SandProfileState implements RunProfileState {
    private final ExecutionEnvironment myEnvironment;
    private final SandExecutorSettings mySettings;

    public SandProfileState(ExecutionEnvironment environment, SandExecutorSettings settings) {
        myEnvironment = environment;
        mySettings = settings;
    }

    @Override
    public ExecutionResult execute(Executor executor, ProgramRunner runner) throws ExecutionException {
        NopProcessHandler processHandler = new NopProcessHandler();
        ConsoleView console = TextConsoleBuilderFactory.getInstance().createBuilder(myEnvironment.getProject()).getConsole();
        console.attachToProcess(processHandler);

        LocalizeValue message = LocalizeValue.localizeTODO(
            "Profiling '" + myEnvironment.getRunProfile().getName() + "' in " + mySettings.getModeName().get() + " mode\n"
        );
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(ProcessEvent event) {
                processHandler.notifyTextAvailable(message.get(), ProcessOutputType.STDOUT);
                processHandler.destroyProcess();
            }
        });
        return new DefaultExecutionResult(console, processHandler);
    }
}
