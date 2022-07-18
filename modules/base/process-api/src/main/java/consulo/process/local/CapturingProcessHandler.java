// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.process.local;

import consulo.application.progress.ProgressIndicator;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.OSProcessHandler;
import consulo.util.lang.DeprecatedMethodException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;

/**
 * Utility class for running an external process and capturing its standard output and error streams.
 *
 * @author yole
 */
public final class CapturingProcessHandler extends OSProcessHandler {
  private final CapturingProcessRunner myProcessRunner;

  public CapturingProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    super(commandLine);
    myProcessRunner = new CapturingProcessRunner(this, this::createProcessAdapter);
  }

  /**
   * {@code commandLine} must not be not empty (for correct thread attribution in the stacktrace)
   */
  public CapturingProcessHandler(@Nonnull Process process, @Nullable Charset charset, /*@NotNull*/ String commandLine) {
    super(process, commandLine, charset);
    myProcessRunner = new CapturingProcessRunner(this, this::createProcessAdapter);
  }

  protected CapturingProcessAdapter createProcessAdapter(ProcessOutput processOutput) {
    return new CapturingProcessAdapter(processOutput);
  }

  @Override
  public Charset getCharset() {
    return myCharset != null ? myCharset : super.getCharset();
  }

  @Nonnull
  public ProcessOutput runProcess() {
    return myProcessRunner.runProcess();
  }

  /**
   * Starts process with specified timeout
   *
   * @param timeoutInMilliseconds non-positive means infinity
   */
  public ProcessOutput runProcess(int timeoutInMilliseconds) {
    return myProcessRunner.runProcess(timeoutInMilliseconds);
  }

  /**
   * Starts process with specified timeout
   *
   * @param timeoutInMilliseconds non-positive means infinity
   * @param destroyOnTimeout      whether to kill the process after timeout passes
   */
  public ProcessOutput runProcess(int timeoutInMilliseconds, boolean destroyOnTimeout) {
    return myProcessRunner.runProcess(timeoutInMilliseconds, destroyOnTimeout);
  }

  @Nonnull
  public ProcessOutput runProcessWithProgressIndicator(@Nonnull ProgressIndicator indicator) {
    return myProcessRunner.runProcess(indicator);
  }

  @Nonnull
  public ProcessOutput runProcessWithProgressIndicator(@Nonnull ProgressIndicator indicator, int timeoutInMilliseconds) {
    return myProcessRunner.runProcess(indicator, timeoutInMilliseconds);
  }

  @Nonnull
  public ProcessOutput runProcessWithProgressIndicator(@Nonnull ProgressIndicator indicator, int timeoutInMilliseconds, boolean destroyOnTimeout) {
    return myProcessRunner.runProcess(indicator, timeoutInMilliseconds, destroyOnTimeout);
  }
}