/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.execution.process;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.platform.Platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Utility class to start a process with a runner mediator (runnerw.exe) injected into a command line,
 * which adds a capability to terminate process tree gracefully by sending it a Ctrl+Break through stdin.
 *
 * @author traff
 */
public class RunnerMediator {
  private static final Logger LOG = Logger.getInstance(RunnerMediator.class);

  private static final char IAC = (char)5;
  private static final char BRK = (char)3;
  private static final char C = (char)5;
  private static final String RUNNERW_BASENAME = "runnerw";
  private static final String CONSULO_RUNNERW = "CONSULO_RUNNERW";

  /**
   * Creates default runner mediator
   */
  public static RunnerMediator getInstance() {
    return new RunnerMediator();
  }

  /**
   * Sends sequence of two chars(codes 5 and {@code event}) to a process output stream
   */
  private static void sendCtrlEventThroughStream(@Nonnull final Process process, final char event) {
    OutputStream os = process.getOutputStream();
    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    PrintWriter pw = new PrintWriter(os);
    pw.print(IAC);
    pw.print(event);
    pw.flush();
  }

  /**
   * In case of windows creates process with runner mediator(runnerw.exe) injected to command line string, which adds a capability
   * to terminate process tree gracefully with ctrl+break.
   *
   * Returns appropriate process handle, which in case of Unix is able to terminate whole process tree by sending sig_kill
   *
   */
  public ProcessHandler createProcess(@Nonnull final GeneralCommandLine commandLine) throws ExecutionException {
    return createProcess(commandLine, false);
  }

  public ProcessHandler createProcess(@Nonnull final GeneralCommandLine commandLine, final boolean useSoftKill) throws ExecutionException {
    if (SystemInfo.isWindows) {
      injectRunnerCommand(commandLine);
    }

    return new CustomDestroyProcessHandler(commandLine, useSoftKill);
  }

  @Nullable
  public static String getRunnerPath() {
    if (!Platform.current().os().isWindows()) {
      throw new IllegalStateException("There is no need of runner under unix based OS");
    }
    final String path = System.getenv(CONSULO_RUNNERW);
    if (path != null) {
      if (new File(path).exists()) {
        return path;
      }
      LOG.warn("Cannot locate " + RUNNERW_BASENAME + " by " + CONSULO_RUNNERW + " environment variable (" + path + ")");
    }
    File runnerw = new File(ContainerPathManager.get().getBinPath(), Platform.current().mapWindowsExecutable(RUNNERW_BASENAME, "exe"));
    if (runnerw.exists()) {
      return runnerw.getPath();
    }
    LOG.warn("Cannot locate " + RUNNERW_BASENAME + " by default path (" + runnerw.getAbsolutePath() + ")");
    return null;
  }

  static boolean injectRunnerCommand(@Nonnull GeneralCommandLine commandLine) {
    final String path = getRunnerPath();
    if (path != null) {
      commandLine.getParametersList().addAt(0, commandLine.getExePath());
      commandLine.setExePath(path);
      return true;
    }
    return false;
  }

  /**
   * Destroys process tree: in case of windows via imitating ctrl+break, in case of unix via sending sig_kill to every process in tree.
   * @param process to kill with all sub-processes.
   */
  public static boolean destroyProcess(@Nonnull final Process process) {
    return destroyProcess(process, false);
  }

  /**
   * Destroys process tree: in case of windows via imitating ctrl+c, in case of unix via sending sig_int to every process in tree.
   * @param process to kill with all sub-processes.
   */
  static boolean destroyProcess(@Nonnull final Process process, final boolean softKill) {
    try {
      if (SystemInfo.isWindows) {
        sendCtrlEventThroughStream(process, softKill ? C : BRK);
        return true;
      }
      else if (SystemInfo.isUnix) {
        if (softKill) {
          return UnixProcessManager.sendSigIntToProcessTree(process);
        }
        else {
          return UnixProcessManager.sendSigKillToProcessTree(process);
        }
      }
      else {
        return false;
      }
    }
    catch (Exception e) {
      LOG.error("Couldn't terminate the process", e);
      return false;
    }
  }

  public static class CustomDestroyProcessHandler extends ColoredProcessHandler {
    private final boolean mySoftKill;

    public CustomDestroyProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
      this(commandLine, false);
    }

    public CustomDestroyProcessHandler(@Nonnull GeneralCommandLine commandLine, final boolean softKill) throws ExecutionException {
      super(commandLine);
      mySoftKill = softKill;
    }

    @Override
    protected boolean shouldDestroyProcessRecursively(){
      return true;
    }

    @Override
    protected void destroyProcessImpl() {
      if (!RunnerMediator.destroyProcess(getProcess(), mySoftKill)) {
        super.destroyProcessImpl();
      }
    }
  }
}
