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
package consulo.execution.impl.internal.terminal;

import com.jediterm.terminal.TtyConnector;
import consulo.application.Application;
import consulo.execution.terminal.TerminalSession;
import consulo.platform.Platform;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.PlatformPtyProcess;
import consulo.process.internal.PlatformPtyProcessFactory;
import consulo.util.lang.StringUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * A shell in a terminal of a machine - the local one or the one of a platform.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public class PlatformTerminalRunner implements TerminalSession {
    // the first size of the terminal - the view resizes it to its own at once
    private static final int INITIAL_COLUMNS = 80;
    private static final int INITIAL_ROWS = 24;

    private final Platform myPlatform;
    private final String myConnectorName;
    private final String myWorkingDirectory;
    private final Supplier<List<String>> myCommandGetter;

    public PlatformTerminalRunner(Platform platform,
                                  String connectorName,
                                  String workingDirectory,
                                  Supplier<List<String>> commandGetter) {
        myPlatform = platform;
        myConnectorName = connectorName;
        myWorkingDirectory = workingDirectory;
        myCommandGetter = commandGetter;
    }

    @Override
    public String getConnectorName() {
        return myConnectorName;
    }

    @Override
    public TtyConnector connect() throws ExecutionException {
        List<String> command = myCommandGetter.get();

        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.withPlatform(myPlatform);
        commandLine.withExecutablePath(myPlatform.fs().getPath(command.get(0)));
        commandLine.addParameters(command.subList(1, command.size()));
        // the rest of the environment is the one of that machine
        commandLine.getEnvironment().put("TERM", "xterm-256color");
        if (!StringUtil.isEmptyOrSpaces(myWorkingDirectory)) {
            commandLine.withWorkingDirectory(myPlatform.fs().getPath(myWorkingDirectory));
        }

        PlatformPtyProcess process;
        try {
            process = Application.get().getInstance(PlatformPtyProcessFactory.class).createPtyProcess(commandLine, INITIAL_COLUMNS, INITIAL_ROWS);
        }
        catch (consulo.process.ExecutionException e) {
            throw new ExecutionException(e.getMessage(), e);
        }
        return new PlatformPtyTtyConnector(process, StandardCharsets.UTF_8, myConnectorName);
    }

    /**
     * The command of a shell path: bash, sh and zsh start as login shells, other paths may carry their options after a space.
     */
    public static List<String> shellCommand(Platform platform, String shellPath) {
        if (platform.os().isWindows()) {
            return List.of(shellPath);
        }
        String shellName = shellPath.substring(shellPath.lastIndexOf('/') + 1);
        if (shellName.equals("bash") || shellName.equals("sh") || shellName.equals("zsh")) {
            return List.of(shellPath, "--login");
        }
        return Arrays.asList(shellPath.split(" "));
    }
}
