package com.intellij.remoteServer.impl.runtime.ui.tree.actions;

import com.intellij.icons.AllIcons;
import com.intellij.remoteServer.impl.runtime.ui.tree.ServerNode;
import javax.annotation.Nonnull;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public class DeployAllAction extends ServerActionBase {

  public DeployAllAction() {
    super("Deploy All", "Deploy all the artifacts of the selected server", AllIcons.Nodes.Deploy);
  }

  @Override
  protected void performAction(@Nonnull ServerNode serverNode) {
    if (serverNode.isDeployAllEnabled()) {
      serverNode.deployAll();
    }
  }

  @Override
  protected boolean isEnabledForServer(@Nonnull ServerNode serverNode) {
    return serverNode.isDeployAllEnabled();
  }
}
