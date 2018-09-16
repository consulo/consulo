package com.intellij.remoteServer.impl.runtime.ui.tree;

import com.intellij.execution.Executor;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface ServerNode {
  boolean isConnected();

  boolean isStopActionEnabled();
  void stopServer();

  boolean isStartActionEnabled(@Nonnull Executor executor);
  void startServer(@Nonnull Executor executor);

  boolean isDeployAllEnabled();
  void deployAll();

  void editConfiguration();
}
