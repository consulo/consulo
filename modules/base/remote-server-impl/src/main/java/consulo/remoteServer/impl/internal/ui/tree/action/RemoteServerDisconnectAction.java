// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

import static consulo.remoteServer.impl.internal.ui.tree.ServersTreeActionUtils.getRemoteServerTarget;

@ActionImpl(id = "RemoteServers.DisconnectServer")
public class RemoteServerDisconnectAction extends DumbAwareAction {
    public RemoteServerDisconnectAction() {
        super(RemoteServerLocalize.actionRemoteserversDisconnectserverText(), RemoteServerLocalize.actionRemoteserversDisconnectserverDescription(), PlatformIconGroup.actionsSuspend());
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        ServersTreeStructure.RemoteServerNode node = getRemoteServerTarget(e);
        boolean visible = node != null;
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible && node.isConnected());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ServersTreeStructure.RemoteServerNode node = getRemoteServerTarget(e);
        ServerConnection<?> connection = node == null ? null : ServerConnectionManager.getInstance().getConnection(node.getValue());
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
