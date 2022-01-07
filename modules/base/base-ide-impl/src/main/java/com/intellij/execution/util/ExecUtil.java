/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.execution.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.containers.ContainerUtil;
import consulo.container.boot.ContainerPathManager;
import consulo.platform.Platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExecUtil {
  private static final NotNullLazyValue<Boolean> hasGkSudo = new NotNullLazyValue<Boolean>() {
    @Nonnull
    @Override
    protected Boolean compute() {
      return new File("/usr/bin/gksudo").canExecute();
    }
  };

  private static final NotNullLazyValue<Boolean> hasKdeSudo = new NotNullLazyValue<Boolean>() {
    @Nonnull
    @Override
    protected Boolean compute() {
      return new File("/usr/bin/kdesudo").canExecute();
    }
  };

  private static final NotNullLazyValue<Boolean> hasPkExec = new NotNullLazyValue<Boolean>() {
    @Nonnull
    @Override
    protected Boolean compute() {
      return new File("/usr/bin/pkexec").canExecute();
    }
  };

  private static final NotNullLazyValue<Boolean> hasGnomeTerminal = new NotNullLazyValue<Boolean>() {
    @Nonnull
    @Override
    protected Boolean compute() {
      return new File("/usr/bin/gnome-terminal").canExecute();
    }
  };

  private static final NotNullLazyValue<Boolean> hasKdeTerminal = new NotNullLazyValue<Boolean>() {
    @Nonnull
    @Override
    protected Boolean compute() {
      return new File("/usr/bin/konsole").canExecute();
    }
  };

  private static final NotNullLazyValue<Boolean> hasXTerm = new NotNullLazyValue<Boolean>() {
    @Nonnull
    @Override
    protected Boolean compute() {
      return new File("/usr/bin/xterm").canExecute();
    }
  };

  private ExecUtil() { }

  @Nonnull
  public static String loadTemplate(@Nonnull ClassLoader loader, @Nonnull String templateName, @Nullable Map<String, String> variables) throws IOException {
    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed") InputStream stream = loader.getResourceAsStream(templateName);
    if (stream == null) {
      throw new IOException("Template '" + templateName + "' not found by " + loader);
    }

    String template = FileUtil.loadTextAndClose(new InputStreamReader(stream, CharsetToolkit.UTF8));
    if (variables == null || variables.size() == 0) {
      return template;
    }

    StringBuilder buffer = new StringBuilder(template);
    for (Map.Entry<String, String> var : variables.entrySet()) {
      String name = var.getKey();
      int pos = buffer.indexOf(name);
      if (pos >= 0) {
        buffer.replace(pos, pos + name.length(), var.getValue());
      }
    }
    return buffer.toString();
  }

  @Nonnull
  public static File createTempExecutableScript(@Nonnull String prefix, @Nonnull String suffix, @Nonnull String content) throws IOException, ExecutionException {
    File tempDir = new File(ContainerPathManager.get().getTempPath());
    File tempFile = FileUtil.createTempFile(tempDir, prefix, suffix, true, true);
    FileUtil.writeToFile(tempFile, content.getBytes(CharsetToolkit.UTF8));
    if (!tempFile.setExecutable(true, true)) {
      throw new ExecutionException("Failed to make temp file executable: " + tempFile);
    }
    return tempFile;
  }

  @Nonnull
  public static String getOsascriptPath() {
    return "/usr/bin/osascript";
  }

  @Nonnull
  public static String getOpenCommandPath() {
    return "/usr/bin/open";
  }

  @Nonnull
  public static String getWindowsShellName() {
    return SystemInfo.isWin2kOrNewer ? "cmd.exe" : "command.com";
  }

  @Nonnull
  public static ProcessOutput execAndGetOutput(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    return new CapturingProcessHandler(commandLine).runProcess();
  }

  @Nullable
  public static String execAndReadLine(@Nonnull GeneralCommandLine commandLine) {
    try {
      return readFirstLine(commandLine.createProcess().getInputStream(), commandLine.getCharset());
    }
    catch (ExecutionException ignored) {
      return null;
    }
  }

  @Nullable
  public static String readFirstLine(@Nonnull InputStream stream, @Nullable Charset cs) {
    try {
      BufferedReader reader = new BufferedReader(cs == null ? new InputStreamReader(stream) : new InputStreamReader(stream, cs));
      try {
        return reader.readLine();
      }
      finally {
        reader.close();
      }
    }
    catch (IOException ignored) {
      return null;
    }
  }

  /**
   * Run the command with superuser privileges using safe escaping and quoting.
   *
   * No shell substitutions, input/output redirects, etc. in the command are applied.
   *
   * @param commandLine the command line to execute
   * @param prompt the prompt string for the users
   * @return the results of running the process
   */
  @Nonnull
  public static Process sudo(@Nonnull GeneralCommandLine commandLine, @Nonnull String prompt) throws ExecutionException, IOException {
    return sudoCommand(commandLine, prompt).createProcess();
  }

  @Nonnull
  private static GeneralCommandLine sudoCommand(@Nonnull GeneralCommandLine commandLine, @Nonnull String prompt) throws ExecutionException, IOException {
    if(Platform.current().user().superUser()) {
      return commandLine;
    }

    List<String> command = ContainerUtil.newArrayList();
    command.add(commandLine.getExePath());
    command.addAll(commandLine.getParametersList().getList());

    GeneralCommandLine sudoCommandLine;
    if (SystemInfo.isMac) {
      String escapedCommandLine = StringUtil.join(command, ExecUtil::escapeAppleScriptArgument, " & \" \" & ");
      String escapedScript = "tell current application\n" +
                             "   activate\n" +
                             "   do shell script " + escapedCommandLine + " with administrator privileges without altering line endings\n" +
                             "end tell";
      sudoCommandLine = new GeneralCommandLine(getOsascriptPath(), "-e", escapedScript);
    }
    else if (hasGkSudo.getValue()) {
      List<String> sudoCommand = ContainerUtil.newArrayList();
      sudoCommand.addAll(Arrays.asList("gksudo", "--message", prompt, "--"));
      sudoCommand.addAll(command);
      sudoCommandLine = new GeneralCommandLine(sudoCommand);
    }
    else if (hasKdeSudo.getValue()) {
      List<String> sudoCommand = ContainerUtil.newArrayList();
      sudoCommand.addAll(Arrays.asList("kdesudo", "--comment", prompt, "--"));
      sudoCommand.addAll(command);
      sudoCommandLine = new GeneralCommandLine(sudoCommand);
    }
    else if (hasPkExec.getValue()) {
      command.add(0, "pkexec");
      sudoCommandLine = new GeneralCommandLine(command);
    }
    else if (SystemInfo.isUnix && hasTerminalApp()) {
      String escapedCommandLine = StringUtil.join(command, ExecUtil::escapeUnixShellArgument, " ");
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
      sudoCommandLine = new GeneralCommandLine(getTerminalCommand("Install", script.getAbsolutePath()));
    }
    else {
      throw new UnsupportedSystemException();
    }

    return sudoCommandLine
            .withWorkDirectory(commandLine.getWorkDirectory())
            .withEnvironment(commandLine.getEnvironment())
            .withParentEnvironmentType(commandLine.getParentEnvironmentType())
            .withRedirectErrorStream(commandLine.isRedirectErrorStream());
  }

  @Nonnull
  public static ProcessOutput sudoAndGetOutput(@Nonnull GeneralCommandLine commandLine, @Nonnull String prompt) throws IOException, ExecutionException {
    return execAndGetOutput(sudoCommand(commandLine, prompt));
  }

  @Nonnull
  private static String escapeAppleScriptArgument(@Nonnull String arg) {
    return "quoted form of \"" + arg.replace("\"", "\\\"") + "\"";
  }

  @Nonnull
  public static String escapeUnixShellArgument(@Nonnull String arg) {
    return "'" + arg.replace("'", "'\"'\"'") + "'";
  }

  public static boolean hasTerminalApp() {
    return SystemInfo.isWindows || SystemInfo.isMac || hasKdeTerminal.getValue() || hasGnomeTerminal.getValue() || hasXTerm.getValue();
  }

  @Nonnull
  public static List<String> getTerminalCommand(@Nullable String title, @Nonnull String command) {
    if (SystemInfo.isWindows) {
      title = title != null ? title.replace("\"", "'") : "";
      return Arrays.asList(getWindowsShellName(), "/c", "start", GeneralCommandLine.inescapableQuote(title), command);
    }
    else if (SystemInfo.isMac) {
      return Arrays.asList(getOpenCommandPath(), "-a", "Terminal", command);
    }
    else if (hasKdeTerminal.getValue()) {
      return Arrays.asList("/usr/bin/konsole", "-e", command);
    }
    else if (hasGnomeTerminal.getValue()) {
      return title != null ? Arrays.asList("/usr/bin/gnome-terminal", "-t", title, "-x", command)
                           : Arrays.asList("/usr/bin/gnome-terminal", "-x", command);
    }
    else if (hasXTerm.getValue()) {
      return title != null ? Arrays.asList("/usr/bin/xterm", "-T", title, "-e", command)
                           : Arrays.asList("/usr/bin/xterm", "-e", command);
    }

    throw new UnsupportedSystemException();
  }

  public static class UnsupportedSystemException extends UnsupportedOperationException {
    public UnsupportedSystemException() {
      super("Unsupported OS/desktop: " + SystemInfo.OS_NAME + '/' + SystemInfo.SUN_DESKTOP);
    }
  }

  @Nonnull
  public static ProcessOutput execAndGetOutput(@Nonnull GeneralCommandLine commandLine, int timeoutInMilliseconds) throws ExecutionException {
    return new CapturingProcessHandler(commandLine).runProcess(timeoutInMilliseconds);
  }

  // deprecated stuff

  /** @deprecated use {@code new GeneralCommandLine(command).createProcess().waitFor()} (to be removed in IDEA 16) */
  public static int execAndGetResult(String... command) throws ExecutionException, InterruptedException {
    assert command != null && command.length > 0;
    return new GeneralCommandLine(command).createProcess().waitFor();
  }

  /** @deprecated use {@code new GeneralCommandLine(command).createProcess().waitFor()} (to be removed in IDEA 16) */
  public static int execAndGetResult(@Nonnull List<String> command) throws ExecutionException, InterruptedException {
    return new GeneralCommandLine(command).createProcess().waitFor();
  }

  /** @deprecated use {@link #execAndGetOutput(GeneralCommandLine)} instead (to be removed in IDEA 16) */
  public static ProcessOutput execAndGetOutput(@Nonnull List<String> command, @Nullable String workDir) throws ExecutionException {
    GeneralCommandLine commandLine = new GeneralCommandLine(command).withWorkDirectory(workDir);
    return new CapturingProcessHandler(commandLine).runProcess();
  }

  /** @deprecated use {@link #execAndReadLine(GeneralCommandLine)} instead (to be removed in IDEA 16) */
  public static String execAndReadLine(String... command) {
    return execAndReadLine(new GeneralCommandLine(command));
  }

  /** @deprecated use {@link #execAndReadLine(GeneralCommandLine)} instead (to be removed in IDEA 16) */
  public static String execAndReadLine(@Nullable Charset charset, String... command) {
    GeneralCommandLine commandLine = new GeneralCommandLine(command);
    if (charset != null) commandLine = commandLine.withCharset(charset);
    return execAndReadLine(commandLine);
  }
}