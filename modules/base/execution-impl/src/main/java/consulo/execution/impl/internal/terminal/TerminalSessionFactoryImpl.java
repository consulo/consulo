/*
 * Copyright 2013-2023 consulo.io
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

import consulo.annotation.component.ServiceImpl;
import consulo.execution.terminal.TerminalSession;
import consulo.execution.terminal.TerminalSessionFactory;
import consulo.platform.Platform;
import consulo.util.lang.StringUtil;
import jakarta.inject.Singleton;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@ServiceImpl
@Singleton
public class TerminalSessionFactoryImpl implements TerminalSessionFactory {
    @Override
    public TerminalSession create(Platform platform, String connectorName, String workingDirectory, Supplier<String> shellPathGetter) {
        return new PlatformTerminalRunner(platform, connectorName, workingDirectory, () -> {
            String shellPath = shellPathGetter.get();
            if (StringUtil.isEmptyOrSpaces(shellPath)) {
                shellPath = findDefaultShellPath(platform);
            }
            return PlatformTerminalRunner.shellCommand(platform, shellPath);
        });
    }

    @Override
    public TerminalSession create(Platform platform, String connectorName, String workingDirectory, List<String> shellCommand) {
        if (shellCommand.isEmpty()) {
            throw new IllegalArgumentException("Empty shell command");
        }
        List<String> command = List.copyOf(shellCommand);
        return new PlatformTerminalRunner(platform, connectorName, workingDirectory, () -> command);
    }

    /**
     * PowerShell on Windows; otherwise the shell of the user, zsh on a Mac, bash, at last sh - the first one which is a
     * file of that machine.
     */
    @Override
    public String findDefaultShellPath(Platform platform) {
        if (platform.os().isWindows()) {
            return "powershell.exe";
        }
        List<String> candidates = new ArrayList<>();
        String shell = platform.os().getEnvironmentVariable("SHELL");
        if (shell != null && !shell.isBlank()) {
            candidates.add(shell);
        }
        if (platform.os().isMac()) {
            candidates.add("/bin/zsh");
        }
        candidates.add("/bin/bash");
        for (String candidate : candidates) {
            if (Files.isRegularFile(platform.fs().getPath(candidate))) {
                return candidate;
            }
        }
        return "/bin/sh";
    }
}
