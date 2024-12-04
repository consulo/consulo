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

import static consulo.remoteServer.impl.internal.ui.tree.ServersTreeActionUtils.getRemoteServerTarget;

@ActionImpl(id = "RemoteServers.ChooseServerDeploymentWithDebug")
public class ChooseDeploymentWithDebugAction extends DumbAwareAction {
    public ChooseDeploymentWithDebugAction() {
        super(RemoteServerLocalize.actionRemoteserversChooseserverdeploymentwithdebugText(), RemoteServerLocalize.actionRemoteserversChooseserverdeploymentwithdebugDescription(), PlatformIconGroup.actionsStartdebugger());
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        ServersTreeStructure.RemoteServerNode node = getRemoteServerTarget(e);
        e.getPresentation().setEnabledAndVisible(node != null && node.getServer().getType().createDebugConnector() != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ServersTreeStructure.RemoteServerNode node = getRemoteServerTarget(e);
        if (node != null) {
            node.deployWithDebug(e);
        }
    }

    @Override
    @Nonnull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
