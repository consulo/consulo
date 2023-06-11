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
package consulo.process.internal;

import consulo.container.boot.ContainerPathManager;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sudo builder which convert {@link GeneralCommandLine}
 */
public class SudoBuilder {
  public static GeneralCommandLine sudoCommand(@Nonnull GeneralCommandLine commandLine,
                                                @Nonnull String prompt) throws ExecutionException, IOException {
    Platform platform = commandLine.getPlatform();

    if (platform.user().superUser()) {
      return commandLine;
    }

    SystemExecutableInfo info = SystemExecutableInfo.get(platform);

    List<String> command = new ArrayList<>();
    command.add(commandLine.getExePath());
    command.addAll(commandLine.getParametersList().getList());

    PlatformOperatingSystem os = platform.os();
    GeneralCommandLine sudoCommandLine;
    if (os.isMac()) {
      String escapedCommandLine = StringUtil.join(command, SudoBuilder::escapeAppleScriptArgument, " & \" \" & ");
      String escapedScript = "tell current application\n" +
        "   activate\n" +
        "   do shell script " + escapedCommandLine + " with administrator privileges without altering line endings\n" +
        "end tell";
      sudoCommandLine = new GeneralCommandLine(info.getOsascriptPath(), "-e", escapedScript);
    }
    else if (info.hasGkSudo.get()) {
      List<String> sudoCommand = new ArrayList<>();
      sudoCommand.addAll(Arrays.asList("gksudo", "--message", prompt, "--"));
      sudoCommand.addAll(command);
      sudoCommandLine = new GeneralCommandLine(sudoCommand);
    }
    else if (info.hasKdeSudo.get()) {
      List<String> sudoCommand = new ArrayList<>();
      sudoCommand.addAll(Arrays.asList("kdesudo", "--comment", prompt, "--"));
      sudoCommand.addAll(command);
      sudoCommandLine = new GeneralCommandLine(sudoCommand);
    }
    else if (info.hasPkExec.get()) {
      command.add(0, "pkexec");
      sudoCommandLine = new GeneralCommandLine(command);
    }
    else if (os.isUnix() && info.hasTerminalApp()) {
      String escapedCommandLine = StringUtil.join(command, SudoBuilder::escapeUnixShellArgument, " ");
      File script = createTempExecutableScript(
        "sudo", ".sh",
        "#!/bin/sh\n" +
          "echo " + escapeUnixShellArgument(prompt) + "\n" +
          "echo\n" +
          "sudo -- " + escapedCommandLine + "\n" +
          "STATUS=$?\n" +
          "echo\n" +
          "read -p \"Press Enter to close this window...\" TEMP\n" +
          "exit $STATUS\n");
      sudoCommandLine = new GeneralCommandLine(info.getTerminalCommand("Install", script.getAbsolutePath()));
    }
    else {
      throw new UnsupportedSystemException(platform);
    }

    return sudoCommandLine
      .withWorkDirectory(commandLine.getWorkDirectory())
      .withEnvironment(commandLine.getEnvironment())
      .withParentEnvironmentType(commandLine.getParentEnvironmentType())
      .withRedirectErrorStream(commandLine.isRedirectErrorStream());
  }

  @Nonnull
  private static String escapeAppleScriptArgument(@Nonnull String arg) {
    return "quoted form of \"" + arg.replace("\"", "\\\"") + "\"";
  }

  @Nonnull
  public static File createTempExecutableScript(@Nonnull String prefix,
                                                @Nonnull String suffix,
                                                @Nonnull String content) throws IOException, ExecutionException {
    File tempDir = new File(ContainerPathManager.get().getTempPath());
    File tempFile = FileUtil.createTempFile(tempDir, prefix, suffix, true, true);
    FileUtil.writeToFile(tempFile, content.getBytes(StandardCharsets.UTF_8));
    if (!tempFile.setExecutable(true, true)) {
      throw new ExecutionException("Failed to make temp file executable: " + tempFile);
    }
    return tempFile;
  }

  @Nonnull
  public static String escapeUnixShellArgument(@Nonnull String arg) {
    return "'" + arg.replace("'", "'\"'\"'") + "'";
  }
}
