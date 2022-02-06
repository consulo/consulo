package com.intellij.remoteServer.runtime.deployment.debug;

import consulo.process.ExecutionException;
import consulo.execution.runner.ExecutionEnvironment;
import com.intellij.remoteServer.configuration.RemoteServer;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface DebuggerLauncher<D extends DebugConnectionData> {
  void startDebugSession(@Nonnull D info, @Nonnull ExecutionEnvironment executionEnvironment, RemoteServer<?> server) throws ExecutionException;
}
