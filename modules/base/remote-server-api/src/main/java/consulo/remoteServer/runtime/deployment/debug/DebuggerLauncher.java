package consulo.remoteServer.runtime.deployment.debug;

import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ExecutionException;
import consulo.remoteServer.configuration.RemoteServer;


/**
 * @author nik
 */
public interface DebuggerLauncher<D extends DebugConnectionData> {
  void startDebugSession(D info, ExecutionEnvironment executionEnvironment, RemoteServer<?> server) throws ExecutionException;
}
