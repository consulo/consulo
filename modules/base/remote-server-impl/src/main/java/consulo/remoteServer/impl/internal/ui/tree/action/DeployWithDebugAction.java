// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.remoteServer.impl.internal.ui.tree.DeploymentNode;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import static consulo.remoteServer.impl.internal.util.ApplicationActionUtils.getDeploymentTarget;

@ActionImpl(id = "Servers.DeployWithDebug")
public class DeployWithDebugAction extends DumbAwareAction {
    public DeployWithDebugAction() {
        super(RemoteServerLocalize.actionServersDeploywithdebugText(),
            RemoteServerLocalize.actionServersDeploywithdebugDescription(),
            ExecutionDebugIconGroup.actionStartdebugger()
        );
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        DeploymentNode node = getDeploymentTarget(e);
        boolean visible = node != null && node.isDeployActionVisible() && node.isDebugActionVisible();
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible && node.isDeployActionEnabled());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DeploymentNode node = getDeploymentTarget(e);
        if (node != null) {
            node.deployWithDebug();
        }
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
