// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.project.Project;
import consulo.remoteServer.CloudBundle;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.impl.internal.ui.DefaultRemoteServersServiceViewContributor;
import consulo.ui.ex.action.*;
import consulo.util.collection.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static consulo.remoteServer.impl.internal.ui.RemoteServersServiceViewContributor.addNewRemoteServer;

public class AddCloudConnectionActionGroup extends ActionGroup {
    public AddCloudConnectionActionGroup() {
        //getTemplatePresentation().setHideGroupIfEmpty(true);
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
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
            super(serverType.getPresentableName(), CloudBundle.message("AddCloudConnectionAction.description", serverType.getPresentableName()),
                serverType.getIcon());
            myServerType = serverType;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            if (e.getPlace().equals(ActionPlaces.ACTION_SEARCH)) {
                e.getPresentation().setText(CloudBundle.message("new.cloud.connection.configurable.title", myServerType.getPresentableName()));
            }
            else {
                e.getPresentation().setTextValue(myServerType.getPresentableName());
            }
        }

//        @Override
//        public @NotNull ActionUpdateThread getActionUpdateThread() {
//            return ActionUpdateThread.EDT;
//        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            if (project == null) return;

            addNewRemoteServer(project, myServerType, DefaultRemoteServersServiceViewContributor.class);
        }
    }
}
