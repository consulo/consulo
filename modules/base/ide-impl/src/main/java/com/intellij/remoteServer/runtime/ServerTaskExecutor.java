package com.intellij.remoteServer.runtime;

import consulo.application.util.function.ThrowableRunnable;
import javax.annotation.Nonnull;

import java.util.concurrent.Executor;

/**
 * @author nik
 */
public interface ServerTaskExecutor extends Executor {
  void submit(@Nonnull Runnable command);
  void submit(@Nonnull ThrowableRunnable<?> command, @Nonnull RemoteOperationCallback callback);
}
