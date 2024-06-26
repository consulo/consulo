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
package consulo.ide.impl.idea.execution.process;

import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.KillableProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.OSProcessHandler;
import consulo.process.internal.UnixProcessManager;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * This process handler supports the "soft-kill" feature (see {@link KillableProcessHandlerImpl}).
 * At first "stop" button send SIGINT signal to process, if it still hangs user can terminate it recursively with SIGKILL signal.
 * <p>
 * Soft kill works on Unix, and also on Windows if a mediator process was used.
 */
public class KillableProcessHandlerImpl extends OSProcessHandler implements KillableProcessHandler {

  private static final Logger LOG = Logger.getInstance(KillableProcessHandlerImpl.class);
  private static final Key<Boolean> MEDIATOR_KEY = Key.create("KillableProcessHandlerImpl.Mediator.Process");

  private boolean myShouldKillProcessSoftly = true;
  private final boolean myMediatedProcess;

  public KillableProcessHandlerImpl(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    super(commandLine);
    myMediatedProcess = MEDIATOR_KEY.get(commandLine) == Boolean.TRUE;
  }

  /**
   * Starts a process with a {@link RunnerMediator mediator} when {@code withMediator} is set to {@code true} and the platform is Windows.
   */
  public KillableProcessHandlerImpl(@Nonnull GeneralCommandLine commandLine, boolean withMediator) throws ExecutionException {
    this(mediate(commandLine, withMediator));
  }

  /**
   * {@code commandLine} must not be not empty (for correct thread attribution in the stacktrace)
   */
  public KillableProcessHandlerImpl(@Nonnull Process process, /*@NotNull*/ String commandLine) {
    super(process, commandLine);
    myMediatedProcess = false;
  }

  /**
   * {@code commandLine} must not be not empty (for correct thread attribution in the stacktrace)
   */
  public KillableProcessHandlerImpl(@Nonnull Process process, /*@NotNull*/ String commandLine, @Nonnull Charset charset) {
    super(process, commandLine, charset);
    myMediatedProcess = false;
  }

  @Nonnull
  protected static GeneralCommandLine mediate(@Nonnull GeneralCommandLine commandLine, boolean withMediator) {
    if (withMediator && Platform.current().os().isWindows() && MEDIATOR_KEY.get(commandLine) == null) {
      boolean mediatorInjected = RunnerMediator.injectRunnerCommand(commandLine);
      MEDIATOR_KEY.set(commandLine, mediatorInjected);
    }
    return commandLine;
  }

  /**
   * @return true, if graceful process termination should be attempted first
   */
  public boolean shouldKillProcessSoftly() {
    return myShouldKillProcessSoftly;
  }

  /**
   * Sets whether the process will be terminated gracefully.
   *
   * @param shouldKillProcessSoftly true, if graceful process termination should be attempted first (i.e. soft kill)
   */
  public void setShouldKillProcessSoftly(boolean shouldKillProcessSoftly) {
    myShouldKillProcessSoftly = shouldKillProcessSoftly;
  }

  /**
   * This method shouldn't be overridden, see shouldKillProcessSoftly
   */
  private boolean  canKillProcessSoftly() {
    if (processCanBeKilledByOS(myProcess)) {
      if (Platform.current().os().isWindows()) {
        // runnerw.exe can send Ctrl+C events to a wrapped process
        return myMediatedProcess;
      }
      else if (Platform.current().os().isUnix()) {
        // 'kill -SIGINT <pid>' will be executed
        return true;
      }
    }
    return false;
  }

  @Override
  protected void destroyProcessImpl() {
    // Don't close streams, because a process may survive graceful termination.
    // Streams will be closed after the process is really terminated.
    try {
      myProcess.getOutputStream().flush();
    }
    catch (IOException e) {
      LOG.warn(e);
    }
    finally {
      doDestroyProcess();
    }
  }

  @Override
  protected void notifyProcessTerminated(int exitCode) {
    try {
      super.closeStreams();
    }
    finally {
      super.notifyProcessTerminated(exitCode);
    }
  }

  @Override
  protected void doDestroyProcess() {
    boolean gracefulTerminationAttempted = shouldKillProcessSoftly() && canKillProcessSoftly() && destroyProcessGracefully();
    if (!gracefulTerminationAttempted) {
      // execute default process destroy
      super.doDestroyProcess();
    }
  }

  protected boolean destroyProcessGracefully() {
    if (Platform.current().os().isWindows() && myMediatedProcess) {
      return RunnerMediator.destroyProcess(myProcess, true);
    }
    else if (Platform.current().os().isUnix()) {
      return UnixProcessManager.sendSigIntToProcessTree(myProcess);
    }
    return false;
  }

  @Override
  public boolean canKillProcess() {
    return processCanBeKilledByOS(getProcess());
  }

  @Override
  public void killProcess() {
    // execute 'kill -SIGKILL <pid>' on Unix
    killProcessTree(getProcess());
  }
}
