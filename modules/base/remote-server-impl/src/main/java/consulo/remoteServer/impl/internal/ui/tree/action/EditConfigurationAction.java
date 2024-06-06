package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import consulo.remoteServer.impl.internal.ui.ServersToolWindowContent;
import consulo.remoteServer.impl.internal.ui.tree.DeploymentNode;
import consulo.remoteServer.impl.internal.ui.tree.ServerNode;
import jakarta.annotation.Nonnull;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public class EditConfigurationAction extends ServersTreeActionBase {
  public EditConfigurationAction() {
    super("Edit Configuration", "Edit configuration of the selected server", AllIcons.Actions.EditSource);
  }

  @Override
  protected void doActionPerformed(@Nonnull ServersToolWindowContent content) {
    Set<DeploymentNode> deploymentNodes = content.getSelectedDeploymentNodes();
    Set<ServerNode> serverNodes = content.getSelectedServerNodes();
    if (deploymentNodes.size() == 1) {
      deploymentNodes.iterator().next().editConfiguration();
    }
    else {
      serverNodes.iterator().next().editConfiguration();
    }
  }

  @Override
  protected boolean isEnabled(@Nonnull ServersToolWindowContent content, AnActionEvent e) {
    Set<DeploymentNode> deploymentNodes = content.getSelectedDeploymentNodes();
    Set<ServerNode> serverNodes = content.getSelectedServerNodes();
    if (deploymentNodes.size() + serverNodes.size() != 1) return false;
    if (deploymentNodes.size() == 1) {
      return deploymentNodes.iterator().next().isEditConfigurationActionEnabled();
    }
    return true;
  }
}
