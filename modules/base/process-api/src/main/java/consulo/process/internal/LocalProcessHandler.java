/*
 * Copyright 2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.process.internal;

import consulo.logging.Logger;
import consulo.process.BaseProcessHandler;
import consulo.process.CommandLineUtil;
import consulo.process.NativeProcessHandler;
import consulo.process.TaskExecutor;
import consulo.process.util.ProcessWaitFor;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public abstract class LocalProcessHandler<T extends Process> extends BaseProcessHandler implements TaskExecutor, RawExitCodeGetterProcessHandler, NativeProcessHandler {
  private static final Logger LOG = Logger.getInstance(LocalProcessHandler.class);

  protected final T myProcess;
  protected final String myCommandLine;
  protected final Charset myCharset;
  protected final String myPresentableName;
  protected final ProcessWaitFor myWaitFor;

  /**
   * {@code commandLine} must not be not empty (for correct thread attribution in the stacktrace)
   */
  public LocalProcessHandler(@Nonnull T process, /*@NotNull*/ String commandLine, @Nullable Charset charset) {
    myProcess = process;
    myCommandLine = commandLine;
    myCharset = charset;
    if (StringUtil.isEmpty(commandLine)) {
      LOG.warn(new IllegalArgumentException("Must specify non-empty 'commandLine' parameter"));
    }
    myPresentableName = CommandLineUtil.extractPresentableName(StringUtil.notNullize(commandLine));
    myWaitFor = new ProcessWaitFor(process, this, myPresentableName);
  }

  @Override
  public long getId() {
    return myProcess.pid();
  }

  @Override
  @Nonnull
  public final T getProcess() {
    return myProcess;
  }

  /*@NotNull*/
  public String getCommandLine() {
    return myCommandLine;
  }

  @Override
  @Nullable
  public Charset getCharset() {
    return myCharset;
  }

  @Override
  public OutputStream getProcessInput() {
    return myProcess.getOutputStream();
  }

  protected void onOSProcessTerminated(int exitCode) {
    notifyProcessTerminated(exitCode);
  }

  protected void doDestroyProcess() {
    getProcess().destroy();
  }

  @Override
  protected void destroyProcessImpl() {
    try {
      closeStreams();
    }
    finally {
      doDestroyProcess();
    }
  }

  @Override
  protected void detachProcessImpl() {
    Runnable runnable = () -> {
      closeStreams();

      myWaitFor.detach();
      notifyProcessDetached();
    };

    executeTask(runnable);
  }

  @Override
  public boolean detachIsDefault() {
    return false;
  }

  @Override
  public int getRawExitCode() {
    return getProcess().exitValue();
  }

  protected void closeStreams() {
    try {
      myProcess.getOutputStream().close();
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }
}