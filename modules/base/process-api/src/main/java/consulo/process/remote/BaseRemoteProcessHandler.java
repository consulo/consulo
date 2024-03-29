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
package consulo.process.remote;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.process.CommandLineUtil;
import consulo.process.ProcessOutputTypes;
import consulo.process.TaskExecutor;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.process.io.BaseOutputReader;
import consulo.process.util.ProcessWaitFor;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

/**
 * @author traff
 */
public class BaseRemoteProcessHandler<T extends RemoteProcess> extends AbstractRemoteProcessHandler<T> implements TaskExecutor {
  private static final Logger LOG = Logger.getInstance(BaseRemoteProcessHandler.class);

  protected final String myCommandLine;
  protected final ProcessWaitFor myWaitFor;
  protected final Charset myCharset;
  protected T myProcess;

  public BaseRemoteProcessHandler(@Nonnull T process, String commandLine, @Nullable Charset charset) {
    myProcess = process;
    myCommandLine = commandLine;
    myWaitFor = new ProcessWaitFor(process, this, CommandLineUtil.extractPresentableName(commandLine));
    myCharset = charset;
    if (StringUtil.isEmpty(commandLine)) {
      LOG.warn(new IllegalArgumentException("Must specify non-empty 'commandLine' parameter"));
    }
  }

  @Override
  public T getProcess() {
    return myProcess;
  }

  @Override
  protected void destroyProcessImpl() {
    if (!myProcess.killProcessTree()) {
      baseDestroyProcessImpl();
    }
  }

  @Override
  public void startNotify() {
    notifyTextAvailable(myCommandLine + '\n', ProcessOutputTypes.SYSTEM);

    addProcessListener(new ProcessListener() {
      @Override
      public void startNotified(final ProcessEvent event) {
        try {
          final RemoteOutputReader stdoutReader =
            new RemoteOutputReader(myProcess.getInputStream(), getCharset(), myProcess, myCommandLine) {
              @Override
              protected void onTextAvailable(@Nonnull String text) {
                notifyTextAvailable(text, ProcessOutputTypes.STDOUT);
              }

              @Nonnull
              @Override
              protected Future<?> executeOnPooledThread(@Nonnull Runnable runnable) {
                return BaseRemoteProcessHandler.executeOnPooledThread(runnable);
              }
            };

          final RemoteOutputReader stderrReader =
            new RemoteOutputReader(myProcess.getErrorStream(), getCharset(), myProcess, myCommandLine) {
              @Override
              protected void onTextAvailable(@Nonnull String text) {
                notifyTextAvailable(text, ProcessOutputTypes.STDERR);
              }

              @Nonnull
              @Override
              protected Future<?> executeOnPooledThread(@Nonnull Runnable runnable) {
                return BaseRemoteProcessHandler.executeOnPooledThread(runnable);
              }
            };

          myWaitFor.setTerminationCallback(exitCode -> {
            try {
              try {
                stderrReader.waitFor();
                stdoutReader.waitFor();
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

  protected void onOSProcessTerminated(final int exitCode) {
    notifyProcessTerminated(exitCode);
  }

  protected void baseDestroyProcessImpl() {
    try {
      closeStreams();
    }
    finally {
      doDestroyProcess();
    }
  }

  protected void doDestroyProcess() {
    getProcess().destroy();
  }

  @Override
  protected void detachProcessImpl() {
    final Runnable runnable = () -> {
      closeStreams();

      myWaitFor.detach();
      notifyProcessDetached();
    };

    executeOnPooledThread(runnable);
  }

  protected void closeStreams() {
    try {
      myProcess.getOutputStream().close();
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  @Override
  public boolean detachIsDefault() {
    return false;
  }

  @Override
  public OutputStream getProcessInput() {
    return myProcess.getOutputStream();
  }

  @Nullable
  public Charset getCharset() {
    return myCharset;
  }

  @Nonnull
  private static Future<?> executeOnPooledThread(@Nonnull Runnable task) {
    return AppExecutorUtil.getAppExecutorService().submit(task);
  }

  @Nonnull
  @Override
  public Future<?> executeTask(@Nonnull Runnable task) {
    return executeOnPooledThread(task);
  }

  private abstract static class RemoteOutputReader extends BaseOutputReader {
    @Nonnull
    private final RemoteProcess myRemoteProcess;
    private boolean myClosed;

    RemoteOutputReader(@Nonnull InputStream inputStream,
                       Charset charset,
                       @Nonnull RemoteProcess remoteProcess,
                       @Nonnull String commandLine) {
      super(inputStream, charset);

      myRemoteProcess = remoteProcess;

      start(CommandLineUtil.extractPresentableName(commandLine));
    }

    @Override
    protected void doRun() {

      try {
        setClosed(false);
        while (true) {
          final boolean read = readAvailable();

          if (myRemoteProcess.isDisconnected()) {
            myReader.close();
            break;
          }

          if (isStopped) {
            break;
          }

          Thread.sleep(mySleepingPolicy.getTimeToSleep(read)); // give other threads a chance
        }
      }
      catch (InterruptedException ignore) {
      }
      catch (Exception e) {
        LOG.warn(e);
      }
      finally {
        setClosed(true);
      }
    }

    protected synchronized void setClosed(boolean closed) {
      myClosed = closed;
    }

    @Override
    public void waitFor() throws InterruptedException {
      while (!isClosed()) {
        Thread.sleep(100);
      }
    }

    private synchronized boolean isClosed() {
      return myClosed;
    }
  }

  @Nullable
  public String getCommandLine() {
    return myCommandLine;
  }
}