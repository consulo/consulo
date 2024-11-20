// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.remoteServer.impl.internal.ui.tree.DeploymentNode;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import org.jetbrains.annotations.NotNull;

import static consulo.remoteServer.impl.internal.util.ApplicationActionUtils.getDeploymentTarget;

/**
 * @author michael.golubev
 */
public class DeployAction extends DumbAwareAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        DeploymentNode node = getDeploymentTarget(e);
        Presentation presentation = e.getPresentation();
        boolean visible = node != null && node.isDeployActionVisible();
        presentation.setVisible(visible);
        presentation.setEnabled(visible && node.isDeployActionEnabled());
        if (node != null && node.isDeployed()) {
            presentation.setText(CloudBundle.messagePointer("action.presentation.DeployAction.text"));
            presentation.setDescription(CloudBundle.messagePointer("action.presentation.DeployAction.description"));
        }
        else {
            presentation.setText(getTemplatePresentation().getText());
            presentation.setDescription(getTemplatePresentation().getDescription());
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DeploymentNode node = getDeploymentTarget(e);
        if (node != null) {
            node.deploy();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}

