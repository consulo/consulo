// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import static consulo.remoteServer.impl.internal.ui.tree.ServersTreeActionUtils.getRemoteServerTarget;

public class ChooseDeploymentWithDebugAction extends DumbAwareAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        ServersTreeStructure.RemoteServerNode node = getRemoteServerTarget(e);
        e.getPresentation().setEnabledAndVisible(node != null && node.getServer().getType().createDebugConnector() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ServersTreeStructure.RemoteServerNode node = getRemoteServerTarget(e);
        if (node != null) {
            node.deployWithDebug(e);
        }
    }

//    @Override
//    public @NotNull ActionUpdateThread getActionUpdateThread() {
//        return ActionUpdateThread.BGT;
//    }
}
