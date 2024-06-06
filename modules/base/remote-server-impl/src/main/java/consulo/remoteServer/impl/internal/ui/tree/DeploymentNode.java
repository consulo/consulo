package consulo.remoteServer.impl.internal.ui.tree;

import jakarta.annotation.Nonnull;

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
