package com.intellij.remoteServer.runtime.deployment.debug;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.remoteServer.configuration.RemoteServer;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface DebuggerLauncher<D extends DebugConnectionData> {
  void startDebugSession(@Nonnull D info, @Nonnull ExecutionEnvironment executionEnvironment, RemoteServer<?> server) throws ExecutionException;
}
