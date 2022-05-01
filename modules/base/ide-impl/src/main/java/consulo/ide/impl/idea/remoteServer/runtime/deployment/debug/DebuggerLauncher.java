package consulo.ide.impl.idea.remoteServer.runtime.deployment.debug;

import consulo.process.ExecutionException;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.ide.impl.idea.remoteServer.configuration.RemoteServer;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface DebuggerLauncher<D extends DebugConnectionData> {
  void startDebugSession(@Nonnull D info, @Nonnull ExecutionEnvironment executionEnvironment, RemoteServer<?> server) throws ExecutionException;
}
