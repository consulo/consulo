// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.EmptyRunnable;
import jakarta.annotation.Nonnull;

import static consulo.remoteServer.impl.internal.ui.tree.ServersTreeActionUtils.getRemoteServerTarget;

@ActionImpl(id = "RemoteServers.ConnectServer")
public class RemoteServerConnectAction extends DumbAwareAction {
    public RemoteServerConnectAction() {
        super(RemoteServerLocalize.actionRemoteserversConnectserverText(), RemoteServerLocalize.actionRemoteserversConnectserverDescription(), PlatformIconGroup.actionsExecute());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        ServersTreeStructure.RemoteServerNode node = getRemoteServerTarget(e);
        boolean visible = node != null;
        e.getPresentation().setVisible(visible);
        e.getPresentation().setEnabled(visible && !node.isConnected());
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ServersTreeStructure.RemoteServerNode node = getRemoteServerTarget(e);
        if (node != null) {
            ServerConnectionManager.getInstance().getOrCreateConnection(node.getValue()).connect(EmptyRunnable.INSTANCE);
        }
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
