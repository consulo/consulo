// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import static consulo.execution.service.ServiceViewActionUtils.getTarget;

public class DeploymentConfigAction extends DumbAwareAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        ServersTreeStructure.DeploymentNodeImpl node = getTarget(e, ServersTreeStructure.DeploymentNodeImpl.class);
        e.getPresentation().setEnabledAndVisible(node != null && node.isEditConfigurationActionVisible());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ServersTreeStructure.DeploymentNodeImpl node = getTarget(e, ServersTreeStructure.DeploymentNodeImpl.class);
        if (node != null) {
            node.editConfiguration();
        }
    }

//    @Override
//    public @NotNull ActionUpdateThread getActionUpdateThread() {
//        return ActionUpdateThread.BGT;
//    }
}
