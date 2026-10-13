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

import consulo.execution.configuration.CommandLineState;
import consulo.content.platform.PlatformTargetManager;
import consulo.execution.process.ProcessTerminatedListener;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.lang.StringUtil;

import java.nio.file.Path;

public class SandExecutableRunState extends CommandLineState {
    private final SandExecutableConfiguration myConfiguration;

    public SandExecutableRunState(ExecutionEnvironment environment, SandExecutableConfiguration configuration) {
        super(environment);
        myConfiguration = configuration;
    }

    @Override
    protected ProcessHandler startProcess() throws ExecutionException {
        Platform platform = myConfiguration.resolvePlatform(
            getEnvironment().getProject().getApplication().getInstance(PlatformTargetManager.class)
        );
        String workingDirectory = myConfiguration.getWorkingDirectory();

        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.withPlatform(platform);
        commandLine.withExecutablePath(resolveExecutable(platform, myConfiguration.getExecutable()));
        commandLine.getParametersList().addParametersString(StringUtil.notNullize(myConfiguration.getProgramParameters()));
        commandLine.withWorkingDirectory(workingDirectory == null ? null : platform.fs().getPath(workingDirectory));
        commandLine.withEnvironment(myConfiguration.getEnvs());
        commandLine.withParentEnvironmentType(
            myConfiguration.isPassParentEnvs()
                ? GeneralCommandLine.ParentEnvironmentType.CONSOLE
                : GeneralCommandLine.ParentEnvironmentType.NONE
        );

        ProcessHandlerBuilderFactory builderFactory =
            getEnvironment().getProject().getApplication().getInstance(ProcessHandlerBuilderFactory.class);
        ProcessHandler processHandler = builderFactory.newBuilder(commandLine).killable().build();
        ProcessTerminatedListener.attach(processHandler);
        return processHandler;
    }

    private static Path resolveExecutable(Platform platform, String executable) throws ExecutionException {
        Path path = platform.fs().getPath(executable);
        if (path.isAbsolute() || path.getNameCount() > 1) {
            return path;
        }

        Path found = platform.findExecutableInPath(executable);
        if (found == null) {
            throw new ExecutionException("Cannot find '" + executable + "' in PATH at " + platform.getName());
        }
        return found;
    }
}
