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
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import static consulo.remoteServer.impl.internal.util.ApplicationActionUtils.getDeploymentTarget;

/**
 * @author michael.golubev
 */
@ActionImpl(id = "Servers.Undeploy", parents = @ActionParentRef(@ActionRef(id = "RunDashboardContentToolbar")))
public class UndeployAction extends DumbAwareAction {
    public UndeployAction() {
        super(RemoteServerLocalize.actionServersUndeployText(), RemoteServerLocalize.actionServersUndeployDescription(), PlatformIconGroup.nodesUndeploy());
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        DeploymentNode node = getDeploymentTarget(e);
        boolean visible = node != null;
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible && node.isUndeployActionEnabled());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DeploymentNode node = getDeploymentTarget(e);
        if (node != null) {
            node.undeploy();
        }
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
