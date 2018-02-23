package com.intellij.remoteServer.impl.runtime.ui.tree;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
public interface DeploymentNode {
  @Nonnull
  ServerNode getServerNode();

  boolean isUndeployActionEnabled();
  void undeploy();

  boolean isEditConfigurationActionEnabled();
  void editConfiguration();
}
