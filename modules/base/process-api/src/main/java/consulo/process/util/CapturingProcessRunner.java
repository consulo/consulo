/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.process.util;

import consulo.application.progress.ProgressIndicator;
import consulo.logging.Logger;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessListener;
import consulo.process.internal.RawExitCodeGetterProcessHandler;

import java.util.function.Function;

public final class CapturingProcessRunner {
  
  private final ProcessOutput myOutput;
  
  private final ProcessHandler myProcessHandler;
  
  private static final Logger LOG = Logger.getInstance(CapturingProcessRunner.class);

  public CapturingProcessRunner(ProcessHandler processHandler) {
    this(processHandler, CapturingProcessAdapter::new);
  }

  public CapturingProcessRunner(ProcessHandler processHandler, Function<? super ProcessOutput, ? extends ProcessListener> processAdapterProducer) {
    myOutput = new ProcessOutput();
    myProcessHandler = processHandler;
    myProcessHandler.addProcessListener(processAdapterProducer.apply(myOutput));
  }

  
  public ProcessOutput runProcess() {
    myProcessHandler.startNotify();
    if (myProcessHandler.waitFor()) {
      setErrorCodeIfNotYetSet();
    }
    else {
      LOG.info("runProcess: exit value unavailable");
    }
    return myOutput;
  }

  
  public ProcessOutput runProcess(int timeoutInMilliseconds) {
    return runProcess(timeoutInMilliseconds, true);
  }

  
  public ProcessOutput runProcess(int timeoutInMilliseconds, boolean destroyOnTimeout) {
    // keep in sync with runProcessWithProgressIndicator
    if (timeoutInMilliseconds <= 0) {
      return runProcess();
    }
    else {
      myProcessHandler.startNotify();
      if (myProcessHandler.waitFor(timeoutInMilliseconds)) {
        setErrorCodeIfNotYetSet();
      }
      else {
        if (destroyOnTimeout) {
          myProcessHandler.destroyProcess();
        }
        myOutput.setTimeout();
      }
      return myOutput;
    }
  }

  
  public ProcessOutput runProcess(ProgressIndicator indicator) {
    return runProcess(indicator, -1);
  }

  
  public ProcessOutput runProcess(ProgressIndicator indicator, int timeoutInMilliseconds) {
    return runProcess(indicator, timeoutInMilliseconds, true);
  }

  
  public ProcessOutput runProcess(ProgressIndicator indicator, int timeoutInMilliseconds, boolean destroyOnTimeout) {
    // keep in sync with runProcess
    if (timeoutInMilliseconds <= 0) {
      timeoutInMilliseconds = Integer.MAX_VALUE;
    }

    int WAIT_INTERVAL = 10;
    int waitingTime = 0;
    boolean setExitCode = true;

    myProcessHandler.startNotify();
    while (!myProcessHandler.waitFor(WAIT_INTERVAL)) {
      waitingTime += WAIT_INTERVAL;

      boolean timeout = waitingTime >= timeoutInMilliseconds;
      boolean canceled = indicator.isCanceled();

      if (canceled || timeout) {
        boolean destroying = canceled || destroyOnTimeout;
        setExitCode = destroying;

        if (destroying && !myProcessHandler.isProcessTerminating() && !myProcessHandler.isProcessTerminated()) {
          myProcessHandler.destroyProcess();
        }

        if (canceled) {
          myOutput.setCancelled();
        }
        else {
          myOutput.setTimeout();
        }
        break;
      }
    }
    if (setExitCode) {
      if (myProcessHandler.waitFor()) {
        setErrorCodeIfNotYetSet();
      }
      else {
        LOG.info("runProcess: exit value unavailable");
      }
    }
    return myOutput;
  }

  private void setErrorCodeIfNotYetSet() {
    // if exit code was set on processTerminated, no need to rewrite it
    // WinPtyProcess returns -2 if pty is already closed
    if (!myOutput.isExitCodeSet()) {
      myOutput.setExitCode(((RawExitCodeGetterProcessHandler)myProcessHandler).getRawExitCode());
    }
  }
}

