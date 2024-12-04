// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import static consulo.remoteServer.impl.internal.ui.tree.ServersTreeActionUtils.getRemoteServerTarget;

@ActionImpl(id = "RemoteServers.ChooseServerDeployment")
public class ChooseDeploymentAction extends DumbAwareAction {
    public ChooseDeploymentAction() {
        super(RemoteServerLocalize.actionRemoteserversChooseserverdeploymentDescription(), RemoteServerLocalize.actionRemoteserversChooseserverdeploymentwithdebugDescription(), PlatformIconGroup.nodesDeploy());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(getRemoteServerTarget(e) != null);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ServersTreeStructure.RemoteServerNode node = getRemoteServerTarget(e);
        if (node != null) {
            node.deploy(e);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
