// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.process;

import consulo.util.dataholder.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseInputStreamReader;
import com.intellij.util.io.BaseOutputReader;
import com.intellij.util.io.BaseOutputReader.Options;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class BaseOSProcessHandler extends BaseProcessHandler<Process> {
  private static final Logger LOG = Logger.getInstance(BaseOSProcessHandler.class);
  private final AtomicLong mySleepStart = new AtomicLong(System.currentTimeMillis());
  private final Throwable myProcessStart;

  /**
   * {@code commandLine} must not be empty (for correct thread attribution in the stacktrace)
   */
  public BaseOSProcessHandler(@Nonnull Process process, /*@NotNull*/ String commandLine, @Nullable Charset charset) {
    super(process, commandLine, charset);
    myProcessStart = new Throwable("Process creation:");
  }

  /**
   * Override this method in order to execute the task with a custom pool
   *
   * @param task a task to run
   * @deprecated override {@link #executeTask(Runnable)} instead of this method
   */
  @Deprecated
  @SuppressWarnings("DeprecatedIsStillUsed")
  @Nonnull
  protected Future<?> executeOnPooledThread(@Nonnull final Runnable task) {
    return ProcessIOExecutorService.INSTANCE.submit(task);
  }

  @Override
  @Nonnull
  public Future<?> executeTask(@Nonnull Runnable task) {
    return executeOnPooledThread(task);
  }

  /**
   * Override this method to fine-tune {@link BaseOutputReader} behavior.
   */
  @Nonnull
  protected Options readerOptions() {
    if (Boolean.getBoolean("output.reader.blocking.mode")) {
      return Options.BLOCKING;
    }
    else {
      return Options.NON_BLOCKING;
    }
  }

  protected boolean processHasSeparateErrorStream() {
    return true;
  }

  @Override
  public void startNotify() {
    if (myCommandLine != null) {
      notifyTextAvailable(myCommandLine + '\n', ProcessOutputTypes.SYSTEM);
    }

    addProcessListener(new ProcessAdapter() {
      @Override
      public void startNotified(@Nonnull final ProcessEvent event) {
        try {
          Options options = readerOptions();
          BaseDataReader stdOutReader = createOutputDataReader(options.policy());
          BaseDataReader stdErrReader = processHasSeparateErrorStream() ? createErrorDataReader(options.policy()) : null;

          myWaitFor.setTerminationCallback(exitCode -> {
            try {
              // tell readers that no more attempts to read process' output should be made
              if (stdErrReader != null) stdErrReader.stop();
              stdOutReader.stop();

              try {
                if (stdErrReader != null) stdErrReader.waitFor();
                stdOutReader.waitFor();
              }
              catch (InterruptedException ignore) {
              }
            }
            finally {
              onOSProcessTerminated(exitCode);
            }
          });
        }
        finally {
          removeProcessListener(this);
        }
      }
    });

    super.startNotify();
  }

  /**
   * @deprecated override {@link #createOutputDataReader()}
   */
  @Deprecated
  //@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @SuppressWarnings({"DeprecatedIsStillUsed", "unused"})
  protected BaseDataReader createErrorDataReader(BaseDataReader.SleepingPolicy policy) {
    return createErrorDataReader();
  }

  /**
   * @deprecated override {@link #createOutputDataReader()}
   */
  @Deprecated
  //@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @SuppressWarnings({"DeprecatedIsStillUsed", "unused"})
  protected BaseDataReader createOutputDataReader(BaseDataReader.SleepingPolicy policy) {
    return createOutputDataReader();
  }

  @Nonnull
  protected BaseDataReader createErrorDataReader() {
    return new SimpleOutputReader(createProcessErrReader(), ProcessOutputTypes.STDERR, readerOptions(), "error stream of " + myPresentableName);
  }

  @Nonnull
  protected BaseDataReader createOutputDataReader() {
    return new SimpleOutputReader(createProcessOutReader(), ProcessOutputTypes.STDOUT, readerOptions(), "output stream of " + myPresentableName);
  }

  @Nonnull
  protected Reader createProcessOutReader() {
    return createInputStreamReader(myProcess.getInputStream());
  }

  @Nonnull
  protected Reader createProcessErrReader() {
    return createInputStreamReader(myProcess.getErrorStream());
  }

  @Nonnull
  private Reader createInputStreamReader(@Nonnull InputStream streamToRead) {
    Charset charset = getCharset();
    if (charset == null) charset = Charset.defaultCharset();
    return new BaseInputStreamReader(streamToRead, charset);
  }

  protected class SimpleOutputReader extends BaseOutputReader {
    private final Key<?> myProcessOutputType;

    public SimpleOutputReader(Reader reader, Key<?> outputType, Options options, @Nonnull String presentableName) {
      super(reader, options);
      myProcessOutputType = outputType;
      start(presentableName);
    }

    @Nonnull
    @Override
    protected Future<?> executeOnPooledThread(@Nonnull Runnable runnable) {
      return BaseOSProcessHandler.this.executeTask(runnable);
    }

    @Override
    protected void onTextAvailable(@Nonnull String text) {
      notifyTextAvailable(text, myProcessOutputType);
    }

    @Override
    protected void beforeSleeping(boolean hasJustReadSomething) {
      long sleepStart = mySleepStart.get();
      if (sleepStart < 0) return;

      long now = System.currentTimeMillis();
      if (hasJustReadSomething) {
        mySleepStart.set(now);
      }
      else if (TimeUnit.MILLISECONDS.toMinutes(now - sleepStart) >= 2 && mySleepStart.compareAndSet(sleepStart, -1)) { // report only once
        LOG.warn("Process hasn't generated any output for a long time.\n" +
                 "If it's a long-running mostly idle daemon process, consider overriding OSProcessHandler#readerOptions with" +
                 " 'BaseOutputReader.Options.forMostlySilentProcess()' to reduce CPU usage.\n" +
                 "Command line: " + StringUtil.trimLog(myCommandLine, 1000), myProcessStart);
      }
    }
  }

  @Override
  public String toString() {
    return myCommandLine;
  }

  @Override
  public boolean waitFor() {
    boolean result = super.waitFor();
    try {
      myWaitFor.waitFor();
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  @Override
  public boolean waitFor(long timeoutInMilliseconds) {
    long start = System.currentTimeMillis();
    boolean result = super.waitFor(timeoutInMilliseconds);
    long elapsed = System.currentTimeMillis() - start;
    try {
      result &= myWaitFor.waitFor(Math.max(0, timeoutInMilliseconds - elapsed), TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return result;
  }
}