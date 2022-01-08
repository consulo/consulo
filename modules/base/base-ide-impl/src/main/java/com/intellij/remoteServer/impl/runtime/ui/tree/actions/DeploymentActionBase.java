package com.intellij.remoteServer.impl.runtime.ui.tree.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.remoteServer.impl.runtime.ui.ServersToolWindowContent;
import com.intellij.remoteServer.impl.runtime.ui.tree.DeploymentNode;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * @author nik
 */
public abstract class DeploymentActionBase extends ServersTreeActionBase {
  public DeploymentActionBase(String text, String description, Image icon) {
    super(text, description, icon);
  }

  protected abstract void perform(DeploymentNode node);

  protected abstract boolean isApplicable(DeploymentNode node);

  @Override
  public void doActionPerformed(@Nonnull ServersToolWindowContent content) {
    for (DeploymentNode node : content.getSelectedDeploymentNodes()) {
      if (isApplicable(node)) {
        perform(node);
      }
    }
  }

  @Override
  protected boolean isEnabled(@Nonnull ServersToolWindowContent content, AnActionEvent e) {
    Set<?> selectedElements = content.getBuilder().getSelectedElements();
    if (selectedElements.isEmpty() || selectedElements.size() != content.getSelectedDeploymentNodes().size()) {
      return false;
    }

    for (DeploymentNode node : content.getSelectedDeploymentNodes()) {
      if (!isApplicable(node)) {
        return false;
      }
    }
    return true;
  }
}
