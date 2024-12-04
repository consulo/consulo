// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.remoteServer.impl.internal.ui.tree.DeploymentNode;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

import static consulo.remoteServer.impl.internal.util.ApplicationActionUtils.getDeploymentTarget;

/**
 * @author michael.golubev
 */
@ActionImpl(id = "Servers.Deploy", parents = @ActionParentRef(@ActionRef(id = "RunDashboardContentToolbar")))
public class DeployAction extends DumbAwareAction {
    public DeployAction() {
        super(RemoteServerLocalize.actionServersDeployText(), RemoteServerLocalize.actionServersDeployDescription(), PlatformIconGroup.nodesDeploy());
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        DeploymentNode node = getDeploymentTarget(e);
        Presentation presentation = e.getPresentation();
        boolean visible = node != null && node.isDeployActionVisible();
        presentation.setVisible(visible);
        presentation.setEnabled(visible && node.isDeployActionEnabled());
        if (node != null && node.isDeployed()) {
            presentation.setTextValue(RemoteServerLocalize.actionPresentationDeployactionText());
            presentation.setDescriptionValue(RemoteServerLocalize.actionPresentationDeployactionDescription());
        }
        else {
            presentation.setTextValue(getTemplatePresentation().getTextValue());
            presentation.setDescriptionValue(getTemplatePresentation().getDescriptionValue());
        }
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DeploymentNode node = getDeploymentTarget(e);
        if (node != null) {
            node.deploy();
        }
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}

