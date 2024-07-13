/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.desktop.application.util;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import consulo.application.ApplicationProperties;
import consulo.component.util.NativeFileLoader;
import consulo.container.boot.ContainerPathManager;
import consulo.platform.Platform;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.TimeoutUtil;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class Restarter {
  private Restarter() {
  }

  private static int getRestartCode() {
    String s = System.getProperty("consulo.restart.code");
    if (s == null) {
      // obsolete option
      s = System.getProperty("jb.restart.code");
    }

    if (s != null) {
      try {
        return Integer.parseInt(s);
      }
      catch (NumberFormatException ignore) {
      }
    }
    return 0;
  }

  public static boolean isSupported() {
    return (getRestartCode() != 0 || Platform.current().os().isWindows() || Platform.current().os().isMac()) && !ApplicationProperties.isInSandbox();
  }

  public static int scheduleRestart(@Nonnull String... beforeRestart) throws IOException {
    try {
      int restartCode = getRestartCode();
      if (restartCode != 0) {
        runCommand(beforeRestart);
        return restartCode;
      }
      else if (Platform.current().os().isWindows()) {
        restartOnWindows(beforeRestart);
        return 0;
      }
      else if (Platform.current().os().isMac()) {
        restartOnMac(beforeRestart);
        return 0;
      }
    }
    catch (Throwable t) {
      throw new IOException("Cannot restart application: " + t.getMessage(), t);
    }

    runCommand(beforeRestart);
    throw new IOException("Cannot restart application: not supported.");
  }

  private static void runCommand(String... beforeRestart) throws IOException {
    if (beforeRestart.length == 0) return;

    try {
      Process process = new ProcessBuilder(beforeRestart).inheritIO().start();

      process.waitFor();
    }
    catch (InterruptedException ignore) {
    }
  }

  private static void restartOnWindows(@Nonnull final String... beforeRestart) throws IOException {
    Kernel32 kernel32 = Native.load("kernel32", Kernel32.class);
    Shell32 shell32 = Native.load("shell32", Shell32.class);

    final int pid = kernel32.GetCurrentProcessId();
    final IntByReference argc = new IntByReference();
    Pointer argv_ptr = shell32.CommandLineToArgvW(kernel32.GetCommandLineW(), argc);
    final String[] argv = argv_ptr.getWideStringArray(0, argc.getValue());
    kernel32.LocalFree(argv_ptr);

    String restarterExe = Platform.current().mapAnyExecutableName("restarter");
    File restarterFilePath = NativeFileLoader.findExecutable(restarterExe);

    doScheduleRestart(restarterFilePath, ContainerPathManager.get().getAppHomeDirectory(), commands -> {
      Collections.addAll(commands, String.valueOf(pid), String.valueOf(beforeRestart.length));
      Collections.addAll(commands, beforeRestart);
      Collections.addAll(commands, String.valueOf(argc.getValue()));
      Collections.addAll(commands, argv);
    });

    // Since the process ID is passed through the command line, we want to make sure that we don't exit before the "restarter"
    // process has a chance to open the handle to our process, and that it doesn't wait for the termination of an unrelated
    // process which happened to have the same process ID.
    TimeoutUtil.sleep(500);
  }

  private static void restartOnMac(@Nonnull final String... beforeRestart) throws IOException {
    File distributionDirectory = ContainerPathManager.get().getAppHomeDirectory();

    File appDirectory = distributionDirectory.getParentFile();
    if (!StringUtil.endsWithIgnoreCase(appDirectory.getName(), ".app")) {
      throw new IOException("Application bundle not found: " + appDirectory.getPath());
    }

    String restarterExecutable = Platform.current().mapAnyExecutableName("restarter");
    File restarterFilePath = NativeFileLoader.findExecutable(restarterExecutable);

    doScheduleRestart(restarterFilePath, appDirectory, commands -> {
      Collections.addAll(commands, appDirectory.getPath());
      Collections.addAll(commands, beforeRestart);
    });
  }

  private static void doScheduleRestart(File restarterFile, File workingDirectory, Consumer<List<String>> argumentsBuilder) throws IOException {
    List<String> commands = new ArrayList<>();
    commands.add(createTempExecutable(restarterFile).getPath());
    argumentsBuilder.accept(commands);
    Runtime.getRuntime().exec(ArrayUtil.toStringArray(commands), null, workingDirectory);
  }

  public static File createTempExecutable(File executable) throws IOException {
    String ext = FileUtil.getExtension(executable.getName());
    File copy = FileUtil.createTempFile(FileUtil.getNameWithoutExtension(executable.getName()), StringUtil.isEmptyOrSpaces(ext) ? ".tmp" : ("." + ext), false);
    FileUtil.copy(executable, copy, FilePermissionCopier.BY_NIO2);
    if (!copy.setExecutable(executable.canExecute())) throw new IOException("Cannot make file executable: " + copy);
    return copy;
  }

  private interface Kernel32 extends StdCallLibrary {
    int GetCurrentProcessId();

    WString GetCommandLineW();

    Pointer LocalFree(Pointer pointer);
  }

  private interface Shell32 extends StdCallLibrary {
    Pointer CommandLineToArgvW(WString command_line, IntByReference argc);
  }
}