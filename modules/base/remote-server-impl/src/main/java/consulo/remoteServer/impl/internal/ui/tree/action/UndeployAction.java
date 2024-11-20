// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.remoteServer.impl.internal.ui.tree.DeploymentNode;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import static consulo.remoteServer.impl.internal.util.ApplicationActionUtils.getDeploymentTarget;

/**
 * @author michael.golubev
 */
public class UndeployAction extends DumbAwareAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        DeploymentNode node = getDeploymentTarget(e);
        boolean visible = node != null;
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible && node.isUndeployActionEnabled());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DeploymentNode node = getDeploymentTarget(e);
        if (node != null) {
            node.undeploy();
        }
    }

//    @Override
//    public @NotNull ActionUpdateThread getActionUpdateThread() {
//        return ActionUpdateThread.BGT;
//    }
}
