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
package consulo.remoteServer.agent.impl.internal.process;

import consulo.annotation.component.ExtensionImpl;
import consulo.remoteServer.agent.impl.internal.RemoteAgentConnection;
import consulo.remoteServer.agent.impl.internal.platform.RemotePlatform;
import consulo.platform.Platform;
import consulo.remoteServer.agent.impl.internal.protocol.ExecStart;
import consulo.process.ExecutionException;
import consulo.process.KillableProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.PlatformProcessHandlerFactory;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ExtensionImpl
public class RemoteAgentProcessHandlerFactory implements PlatformProcessHandlerFactory {
    @Override
    public boolean isApplicable(Platform platform) {
        return platform instanceof RemotePlatform;
    }

    @Override
    public KillableProcessHandler createProcessHandler(GeneralCommandLine commandLine) throws ExecutionException {
        RemotePlatform platform = (RemotePlatform) commandLine.getPlatform();
        RemoteAgentConnection connection = platform.getConnection();

        Path executable = commandLine.getExecutable();
        if (executable == null) {
            throw new ExecutionException("Executable is not specified");
        }
        String exePath = executable.toString();
        List<String> parameters = commandLine.getParametersList().getList();
        Path workingDirectory = commandLine.getWorkingDirectory();
        String workingDirectoryPath = workingDirectory == null ? "" : workingDirectory.toString();
        Map<String, String> environment = new LinkedHashMap<>(commandLine.getEnvironment());

        ExecStart start = ExecStart.newBuilder()
            .setCommand(exePath)
            .addAllArguments(parameters)
            .setWorkingDirectory(workingDirectoryPath)
            .putAllEnvironment(environment)
            .build();

        RemoteAgentProcessHandler handler =
            new RemoteAgentProcessHandler(connection, commandLine.getCommandLineString(), platform.getConsoleCharset());
        handler.start(start);
        return handler;
    }
}
