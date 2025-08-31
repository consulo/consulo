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
package consulo.process.internal;

import consulo.logging.Logger;
import consulo.process.DefaultCharsetProvider;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.io.BaseOutputReader;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

public class OSProcessHandler extends BaseOSProcessHandler {
  private static final Logger LOG = Logger.getInstance(OSProcessHandler.class);

  public static Key<Set<File>> DELETE_FILES_ON_TERMINATION = Key.create("OSProcessHandler.FileToDelete");

  private boolean myHasErrorStream = true;
  private boolean myHasPty;
  private boolean myDestroyRecursively = true;
  private Set<File> myFilesToDelete = null;

  public OSProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    this(startProcess(commandLine), commandLine.getCommandLineString(), commandLine.getCharset());
    myHasErrorStream = !commandLine.isRedirectErrorStream();
    myFilesToDelete = commandLine.getUserData(DELETE_FILES_ON_TERMINATION);
  }

  /**
   * {@code commandLine} must not be not empty (for correct thread attribution in the stacktrace)
   */
  public OSProcessHandler(@Nonnull Process process, /*@NotNull*/ String commandLine) {
    this(process, commandLine, DefaultCharsetProvider.getInstance().getDefaultCharset());
  }

  /**
   * {@code commandLine} must not be not empty (for correct thread attribution in the stacktrace)
   */
  public OSProcessHandler(@Nonnull Process process, /*@NotNull*/ String commandLine, @Nullable Charset charset) {
    super(process, commandLine, charset);
    setHasPty(isPtyProcess(process));
  }

  private static Process startProcess(GeneralCommandLine commandLine) throws ExecutionException {
    try {
      return commandLine.createProcess();
    }
    catch (ExecutionException | RuntimeException | Error e) {
      deleteTempFiles(commandLine.getUserData(DELETE_FILES_ON_TERMINATION));
      throw e;
    }
  }

  private static void deleteTempFiles(Set<File> tempFiles) {
    if (tempFiles != null) {
      try {
        for (File file : tempFiles) {
          FileUtil.delete(file);
        }
      }
      catch (Throwable t) {
        LOG.error("failed to delete temp. files", t);
      }
    }
  }

  private static boolean isPtyProcess(Process process) {
    Class c = process.getClass();

    while (c != null) {
      if ("com.pty4j.unix.UnixPtyProcess".equals(c.getName()) || "com.pty4j.windows.WinPtyProcess".equals(c.getName())) {
        return true;
      }
      c = c.getSuperclass();
    }

    return false;
  }

  @Override
  protected void onOSProcessTerminated(int exitCode) {
    super.onOSProcessTerminated(exitCode);
    deleteTempFiles(myFilesToDelete);
  }

  @Override
  protected boolean processHasSeparateErrorStream() {
    return myHasErrorStream;
  }

  protected boolean shouldDestroyProcessRecursively() {
    // Override this method if you want to kill process recursively (whole process try) by default
    // such behaviour is better than default java one, which doesn't kill children processes
    return myDestroyRecursively;
  }

  public void setShouldDestroyProcessRecursively(boolean destroyRecursively) {
    myDestroyRecursively = destroyRecursively;
  }

  @Override
  protected void doDestroyProcess() {
    // Override this method if you want to customize default destroy behaviour, e.g.
    // if you want use some soft-kill.
    Process process = getProcess();
    if (shouldDestroyProcessRecursively() && processCanBeKilledByOS(process)) {
      killProcessTree(process);
    }
    else {
      process.destroy();
    }
  }

  public static boolean processCanBeKilledByOS(Process process) {
    return !(process instanceof SelfKiller);
  }

  /**
   * Kills the whole process tree asynchronously.
   * As a potentially time-consuming operation, it's executed asynchronously on a pooled thread.
   *
   * @param process Process
   */
  protected void killProcessTree(@Nonnull Process process) {
    executeTask(() -> killProcessTreeSync(process));
  }

  private void killProcessTreeSync(@Nonnull Process process) {
    LOG.debug("killing process tree");
    boolean destroyed = OSProcessManager.getInstance().killProcessTree(process);
    if (!destroyed) {
      if (!process.isAlive()) {
        LOG.warn("Process has been already terminated: " + myCommandLine);
      }
      else {
        LOG.warn("Cannot kill process tree. Trying to destroy process using Java API. Cmdline:\n" + myCommandLine);
        process.destroy();
      }
    }
  }

  /**
   * In case of pty this process handler will use blocking read. The value should be set before
   * startNotify invocation. It is set by default in case of using GeneralCommandLine based constructor.
   *
   * @param hasPty true if process is pty based
   */
  public void setHasPty(boolean hasPty) {
    myHasPty = hasPty;
  }

  @Nonnull
  @Override
  protected BaseOutputReader.Options readerOptions() {
    return myHasPty ? BaseOutputReader.Options.BLOCKING : super.readerOptions();  // blocking read in case of PTY-based process
  }

  /**
   * Registers a file to delete after the given command line finishes.
   * In order to have an effect, the command line has to be executed with {@link #OSProcessHandler(GeneralCommandLine)}.
   */
  public static void deleteFileOnTermination(@Nonnull GeneralCommandLine commandLine, @Nonnull File fileToDelete) {
    Set<File> set = commandLine.getUserData(DELETE_FILES_ON_TERMINATION);
    if (set == null) {
      commandLine.putUserData(DELETE_FILES_ON_TERMINATION, set = new HashSet<>());
    }
    set.add(fileToDelete);
  }
}