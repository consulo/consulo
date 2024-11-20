// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui;

import consulo.application.AllIcons;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.execution.service.ServiceViewManager;
import consulo.execution.service.ServiceViewToolWindowDescriptor;
import consulo.execution.service.SimpleServiceViewDescriptor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.impl.internal.runtime.log.DeploymentLogManagerImpl;
import consulo.remoteServer.impl.internal.runtime.log.LoggingHandlerBase;
import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DefaultRemoteServersServiceViewContributor extends RemoteServersServiceViewContributor {
    private final ServiceViewDescriptor myContributorDescriptor = new DefaultRemoteServersServiceViewDescriptor();

    @Override
    public @NotNull ServiceViewDescriptor getViewDescriptor(@NotNull Project project) {
        return myContributorDescriptor;
    }

    @Override
    public boolean accept(@NotNull RemoteServer server) {
        return isDefaultRemoteServer(server);
    }

    @Override
    public void selectLog(@NotNull AbstractTreeNode deploymentNode, @NotNull String logName) {
        ServersTreeStructure.DeploymentNodeImpl node = ObjectUtil.tryCast(deploymentNode, ServersTreeStructure.DeploymentNodeImpl.class);
        if (node == null) {
            return;
        }

        ServerConnection<?> connection = node.getConnection();
        if (connection == null) {
            return;
        }

        Project project = Objects.requireNonNull(node.getProject());
        DeploymentLogManagerImpl logManager = (DeploymentLogManagerImpl) connection.getLogManager(project, node.getDeployment());
        if (logManager == null) {
            return;
        }

        for (LoggingHandlerBase loggingComponent : logManager.getAdditionalLoggingHandlers()) {
            if (logName.equals(loggingComponent.getPresentableName())) {
                ServersTreeStructure.DeploymentLogNode logNode = new ServersTreeStructure.DeploymentLogNode(project, loggingComponent, node);
                ServiceViewManager.getInstance(project).select(logNode, DefaultRemoteServersServiceViewContributor.class, true, true);
            }
        }
    }

    @Override
    public @NotNull ActionGroups getActionGroups() {
        return RemoteServersServiceViewContributor.ActionGroups.SHARED_ACTION_GROUPS;
    }

    @Override
    public AbstractTreeNode<?> createDeploymentNode(ServerConnection<?> connection,
                                                    ServersTreeStructure.RemoteServerNode serverNode,
                                                    Deployment deployment) {
        return new ServersTreeStructure.DeploymentNodeImpl(serverNode.getProject(), connection, serverNode, deployment, this);
    }

    private static boolean isDefaultRemoteServer(RemoteServer<?> server) {
        String toolWindowId = server.getConfiguration().getCustomToolWindowId();
        if (toolWindowId == null) {
            toolWindowId = server.getType().getCustomToolWindowId();
        }
        return toolWindowId == null;
    }

    private static class DefaultRemoteServersServiceViewDescriptor extends SimpleServiceViewDescriptor
        implements ServiceViewToolWindowDescriptor {

        DefaultRemoteServersServiceViewDescriptor() {
            super("Clouds", AllIcons.General.Balloon);
        }

        @Override
        public ActionGroup getToolbarActions() {
            return RemoteServersServiceViewContributor.getToolbarActions(RemoteServersServiceViewContributor.ActionGroups.SHARED_ACTION_GROUPS);
        }

        @Override
        public ActionGroup getPopupActions() {
            return RemoteServersServiceViewContributor.getPopupActions(RemoteServersServiceViewContributor.ActionGroups.SHARED_ACTION_GROUPS);
        }

        @Override
        public @NotNull String getToolWindowId() {
            return getId();
        }

        @Override
        @NotNull
        public Image getToolWindowIcon() {
            return PlatformIconGroup.toolwindowsToolwindowservices();
        }

        @Override
        public @NotNull String getStripeTitle() {
            String title = getToolWindowId();
            return title;
        }

        @Override
        public boolean isExclusionAllowed() {
            return false;
        }
    }
}
