// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.process.internal;

import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.ProcessHandlerFeature;
import consulo.process.ProcessOutputTypes;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.process.io.BaseDataReader;
import consulo.process.io.BaseInputStreamReader;
import consulo.process.io.BaseOutputReader;
import consulo.process.io.ProcessIOExecutorService;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class BaseOSProcessHandler extends LocalProcessHandler<Process> {
  private static class POSIXImpl implements ProcessHandlerFeature.POSIX {
    private final Process process;

    private POSIXImpl(Process process) {
      this.process = process;
    }

    @Override
    public void sendSignal(int signal) {
      UnixProcessManager.sendSignal((int)process.pid(), signal);
    }
  }

  private static final Logger LOG = Logger.getInstance(BaseOSProcessHandler.class);
  private final AtomicLong mySleepStart = new AtomicLong(System.currentTimeMillis());
  private final Throwable myProcessStart;

  /**
   * {@code commandLine} must not be empty (for correct thread attribution in the stacktrace)
   */
  public BaseOSProcessHandler(@Nonnull Process process, /*@NotNull*/ String commandLine, @Nullable Charset charset) {
    super(process, commandLine, charset);
    myProcessStart = new Throwable("Process creation:");

    // TODO [VISTALL] we need use platform from GeneralCommandLine
    if (Platform.current().os().isUnix()) {
      registerFeature(ProcessHandlerFeature.POSIX.class, new POSIXImpl(process));
    }
  }

  @Override
  @Nonnull
  public Future<?> executeTask(@Nonnull Runnable task) {
    return ProcessIOExecutorService.INSTANCE.submit(task);
  }

  /**
   * Override this method to fine-tune {@link BaseOutputReader} behavior.
   */
  @Nonnull
  protected BaseOutputReader.Options readerOptions() {
    if (Boolean.getBoolean("output.reader.blocking.mode")) {
      return BaseOutputReader.Options.BLOCKING;
    }
    else {
      return BaseOutputReader.Options.NON_BLOCKING;
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

    addProcessListener(new ProcessListener() {
      @Override
      public void startNotified(@Nonnull final ProcessEvent event) {
        try {
          BaseDataReader stdOutReader = createOutputDataReader();
          BaseDataReader stdErrReader = processHasSeparateErrorStream() ? createErrorDataReader() : null;

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