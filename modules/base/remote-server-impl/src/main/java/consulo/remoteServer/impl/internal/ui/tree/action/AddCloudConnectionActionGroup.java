// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.impl.internal.ui.DefaultRemoteServersServiceViewContributor;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

import static consulo.remoteServer.impl.internal.ui.RemoteServersServiceViewContributor.addNewRemoteServer;

@ActionImpl(id = "RemoteServers.AddCloudConnectionGroup", parents = @ActionParentRef(@ActionRef(id = "ServiceView.AddService")))
public class AddCloudConnectionActionGroup extends ActionGroup implements DumbAware {
    public AddCloudConnectionActionGroup() {
        super(RemoteServerLocalize.groupRemoteserversAddcloudconnectiongroupText());
        //getTemplatePresentation().setHideGroupIfEmpty(true);
    }

    @Override
    public boolean isPopup() {
        return true;
    }

    @Override
    @Nonnull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        List<ServerType> serverTypes = ContainerUtil.filter(ServerType.EP_NAME.getExtensionList(),
            type -> type.getCustomToolWindowId() == null &&
                type.createDefaultConfiguration().getCustomToolWindowId() == null);
        AnAction[] actions = new AnAction[serverTypes.size()];
        for (int i = 0; i < serverTypes.size(); i++) {
            actions[i] = new AddCloudConnectionAction(serverTypes.get(i));
        }
        return actions;
    }

    private static class AddCloudConnectionAction extends DumbAwareAction {
        private final ServerType<?> myServerType;

        AddCloudConnectionAction(ServerType<?> serverType) {
            super(serverType.getPresentableName(),
                RemoteServerLocalize.addcloudconnectionactionDescription(serverType.getPresentableName()),
                serverType.getIcon()
            );
            myServerType = serverType;
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
            if (e.getPlace().equals(ActionPlaces.ACTION_SEARCH)) {
                e.getPresentation().setTextValue(RemoteServerLocalize.newCloudConnectionConfigurableTitle(myServerType.getPresentableName()));
            }
            else {
                e.getPresentation().setTextValue(myServerType.getPresentableName());
            }
        }

        @Override
        @Nonnull
        public ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            if (project == null) {
                return;
            }

            addNewRemoteServer(project, myServerType, DefaultRemoteServersServiceViewContributor.class);
        }
    }
}
