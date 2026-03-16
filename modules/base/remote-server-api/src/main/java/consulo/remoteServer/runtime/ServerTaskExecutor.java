package consulo.remoteServer.runtime;

import consulo.util.lang.function.ThrowableRunnable;

import java.util.concurrent.Executor;

/**
 * @author nik
 */
public interface ServerTaskExecutor extends Executor {
  void submit(Runnable command);

  void submit(ThrowableRunnable<?> command, RemoteOperationCallback callback);
}
