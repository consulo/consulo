package com.intellij.remoteServer.impl.runtime;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.remoteServer.runtime.RemoteOperationCallback;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import org.jetbrains.annotations.NotNull;

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
  public void execute(@NotNull Runnable command) {
    myTaskExecutor.execute(command);
  }

  @Override
  public void submit(@NotNull Runnable command) {
    execute(command);
  }

  @Override
  public void submit(@NotNull final ThrowableRunnable<?> command, @NotNull final RemoteOperationCallback callback) {
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
