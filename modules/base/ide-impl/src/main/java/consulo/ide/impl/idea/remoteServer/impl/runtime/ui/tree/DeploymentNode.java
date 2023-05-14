package consulo.ide.impl.idea.remoteServer.impl.runtime.ui.tree;

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
