package consulo.remoteServer.runtime.deployment.debug;

import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ExecutionException;
import consulo.remoteServer.configuration.RemoteServer;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public interface DebuggerLauncher<D extends DebugConnectionData> {
  void startDebugSession(@Nonnull D info, @Nonnull ExecutionEnvironment executionEnvironment, RemoteServer<?> server) throws ExecutionException;
}
