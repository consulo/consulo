// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

import static consulo.execution.service.ServiceViewActionUtils.getTarget;

@ActionImpl(id = "RemoteServers.EditDeploymentConfig")
public class DeploymentConfigAction extends DumbAwareAction {
    public DeploymentConfigAction() {
        super(RemoteServerLocalize.actionRemoteserversEditserverconfigText(), RemoteServerLocalize.actionRemoteserversEditserverconfigDescription(), PlatformIconGroup.actionsEdit());
    }
    
    @Override
    public void update(@Nonnull AnActionEvent e) {
        ServersTreeStructure.DeploymentNodeImpl node = getTarget(e, ServersTreeStructure.DeploymentNodeImpl.class);
        e.getPresentation().setEnabledAndVisible(node != null && node.isEditConfigurationActionVisible());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ServersTreeStructure.DeploymentNodeImpl node = getTarget(e, ServersTreeStructure.DeploymentNodeImpl.class);
        if (node != null) {
            node.editConfiguration();
        }
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
