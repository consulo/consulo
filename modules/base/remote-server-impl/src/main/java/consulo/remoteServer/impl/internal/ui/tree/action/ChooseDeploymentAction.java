// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import static consulo.remoteServer.impl.internal.ui.tree.ServersTreeActionUtils.getRemoteServerTarget;

public class ChooseDeploymentAction extends DumbAwareAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(getRemoteServerTarget(e) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ServersTreeStructure.RemoteServerNode node = getRemoteServerTarget(e);
        if (node != null) {
            node.deploy(e);
        }
    }

//    @Override
//    public @NotNull ActionUpdateThread getActionUpdateThread() {
//        return ActionUpdateThread.BGT;
//    }
}
