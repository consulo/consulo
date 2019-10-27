package com.intellij.remoteServer.impl.runtime;

import com.intellij.remoteServer.runtime.RemoteOperationCallback;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import java.util.concurrent.ExecutorService;

/**
 * @author nik
 */
public class ServerTaskExecutorImpl implements ServerTaskExecutor {
  private static final Logger LOG = Logger.getInstance(ServerTaskExecutorImpl.class);
  private final ExecutorService myTaskExecutor;

  public ServerTaskExecutorImpl() {
    myTaskExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("ServerTaskExecutorImpl pool");
  }

  @Override
  public void execute(@Nonnull Runnable command) {
    myTaskExecutor.execute(command);
  }

  @Override
  public void submit(@Nonnull Runnable command) {
    execute(command);
  }

  @Override
  public void submit(@Nonnull final ThrowableRunnable<?> command, @Nonnull final RemoteOperationCallback callback) {
    execute(new Runnable() {
      @Override
      public void run() {
        try {
          command.run();
        }
        catch (Throwable e) {
          LOG.info(e);
          callback.errorOccurred(e.getMessage());
        }
      }
    });
  }
}
