package org.jetbrains.ide;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class PooledThreadExecutor implements Executor {
  public static final PooledThreadExecutor INSTANCE = new PooledThreadExecutor();

  @Override
  public void execute(@NotNull Runnable command) {
    ApplicationManager.getApplication().executeOnPooledThread(command);
  }
}