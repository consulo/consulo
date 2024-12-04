// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AppUIExecutor;
import consulo.execution.service.ServiceEventListener;
import consulo.execution.service.ServiceViewManager;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.remoteServer.CloudBundle;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServerListener;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.impl.internal.ui.tree.ServerTreeNodeExpander;
import consulo.remoteServer.impl.internal.ui.tree.ServersToolWindowMessagePanel;
import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.remoteServer.impl.internal.util.CloudApplicationRuntime;
import consulo.remoteServer.runtime.*;
import consulo.remoteServer.runtime.ui.RemoteServersView;
import consulo.remoteServer.runtime.ui.ServersTreeNodeSelector;
import consulo.ui.ModalityState;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class RemoteServersDeploymentManager {
    private static final int POLL_DEPLOYMENTS_DELAY = 2000;

    public static RemoteServersDeploymentManager getInstance(Project project) {
        return project.getInstance(RemoteServersDeploymentManager.class);
    }

    private final Project myProject;
    private final ServersTreeNodeManipulator myNodeManipulator;
    private final Map<RemoteServersServiceViewContributor, Boolean> myContributors = ContainerUtil.createConcurrentWeakMap();
    private final Map<RemoteServer<?>, MessagePanel> myServerToContent = new HashMap<>();

    @Inject
    public RemoteServersDeploymentManager(@Nonnull Project project) {
        myProject = project;
        myNodeManipulator = new ServersTreeNodeManipulator(project);
        initListeners();
        RemoteServersView.getInstance(project)
            .registerTreeNodeSelector(myNodeManipulator, connection -> myContributors.keySet().stream()
                .anyMatch(contributor -> contributor.accept(connection.getServer())));
    }

    private void initListeners() {
        myProject.getMessageBus().connect().subscribe(ServerConnectionListener.class, new ServerConnectionListener() {
            private final Set<ServerConnection<?>> myConnectionsToExpand = new HashSet<>();

            @Override
            public void onConnectionCreated(@Nonnull ServerConnection<?> connection) {
                RemoteServersServiceViewContributor contributor = findContributor(connection.getServer());
                if (contributor != null) {
                    myProject.getMessageBus().syncPublisher(ServiceEventListener.TOPIC)
                        .handle(ServiceEventListener.ServiceEvent.createResetEvent(contributor.getClass()));
                }
            }

            @Override
            public void onConnectionStatusChanged(@Nonnull ServerConnection<?> connection) {
                RemoteServer<?> server = connection.getServer();
                RemoteServersServiceViewContributor contributor = findContributor(server);
                if (contributor != null) {
                    myProject.getMessageBus().syncPublisher(ServiceEventListener.TOPIC)
                        .handle(ServiceEventListener.ServiceEvent.createResetEvent(contributor.getClass()));
                    updateServerContent(myServerToContent.get(server), connection);
                    if (connection.getStatus() == ConnectionStatus.CONNECTED) {
                        // connectionStatusChanged is also called for errors, don't initiate polling once again, IDEA-259400
                        if (StringUtil.areSameInstance(connection.getStatusText(), connection.getStatus().getPresentableText())) { // effectively, checks for no error
                            myConnectionsToExpand.add(connection);
                            pollDeployments(connection);
                        }
                    }
                    else {
                        myConnectionsToExpand.remove(connection);
                    }
                }
            }

            @Override
            public void onDeploymentsChanged(@Nonnull ServerConnection<?> connection) {
                RemoteServer<?> server = connection.getServer();
                RemoteServersServiceViewContributor contributor = findContributor(server);
                if (contributor != null) {
                    ServiceEventListener.ServiceEvent event = contributor.createDeploymentsChangedEvent(connection);
                    boolean justConnected = myConnectionsToExpand.remove(connection);
                    if (event == null && justConnected) {
                        event = ServiceEventListener.ServiceEvent.createResetEvent(contributor.getClass());
                    }
                    if (event != null) {
                        myProject.getMessageBus().syncPublisher(ServiceEventListener.TOPIC).handle(event);
                    }
                    updateServerContent(myServerToContent.get(server), connection);
                    if (justConnected) {
                        ServersTreeStructure.RemoteServerNode serverNode = new ServersTreeStructure.RemoteServerNode(myProject, connection.getServer(), contributor);
                        ServiceViewManager.getInstance(myProject).expand(serverNode, contributor.getClass());
                    }
                }
            }
        });

        myProject.getMessageBus().connect().subscribe(RemoteServerListener.class, new RemoteServerListener() {
            @Override
            public void serverAdded(@Nonnull RemoteServer<?> server) {
                RemoteServersServiceViewContributor contributor = findContributor(server);
                if (contributor != null) {
                    myServerToContent.put(server, createMessagePanel());
                    myProject.getMessageBus().syncPublisher(ServiceEventListener.TOPIC)
                        .handle(ServiceEventListener.ServiceEvent.createResetEvent(contributor.getClass()));
                }
            }

            @Override
            public void serverRemoved(@Nonnull RemoteServer<?> server) {
                RemoteServersServiceViewContributor contributor = findContributor(server);
                if (contributor != null) {
                    myProject.getMessageBus().syncPublisher(ServiceEventListener.TOPIC)
                        .handle(ServiceEventListener.ServiceEvent.createResetEvent(contributor.getClass()));
                }
                myServerToContent.remove(server);
            }
        });
    }

    public void registerContributor(@Nonnull RemoteServersServiceViewContributor contributor) {
        if (myContributors.put(contributor, Boolean.TRUE) == null) {
            AppUIExecutor.onUiThread().expireWith(myProject).submit(() -> {
                for (RemoteServer<?> server : RemoteServersManager.getInstance().getServers()) {
                    if (contributor.accept(server)) {
                        myServerToContent.put(server, createMessagePanel());
                    }
                }
            });
        }
    }

    public @Nonnull ServerTreeNodeExpander getNodeExpander() {
        return myNodeManipulator;
    }

    public @Nonnull ServersTreeNodeSelector getNodeSelector() {
        return myNodeManipulator;
    }

    public JComponent getServerContent(RemoteServer<?> server) {
        MessagePanel messagePanel = myServerToContent.get(server);
        if (messagePanel == null) {
            return null;
        }

        updateServerContent(messagePanel, ServerConnectionManager.getInstance().getConnection(server));
        return messagePanel.getComponent();
    }

    private static void updateServerContent(@Nullable MessagePanel messagePanel, @Nullable ServerConnection<?> connection) {
        if (messagePanel == null) {
            return;
        }

        if (connection == null) {
            messagePanel.setEmptyText(CloudBundle.message("cloud.status.double.click.to.connect"));
        }
        else {
            String text = connection.getStatusText();
            if (text.contains("<br/>") && !text.startsWith("<html>")) {
                text = "<html><center>" + text + "</center></html>";
            }
            messagePanel.setEmptyText(text);
        }
    }

    public @Nullable RemoteServersServiceViewContributor findContributor(@Nonnull RemoteServer<?> server) {
        for (RemoteServersServiceViewContributor contributor : myContributors.keySet()) {
            if (contributor.accept(server)) {
                return contributor;
            }
        }
        return null;
    }

    private static void pollDeployments(@Nonnull ServerConnection<?> connection) {
        connection.computeDeployments(() -> new Alarm().addRequest(() -> {
            if (connection == ServerConnectionManager.getInstance().getConnection(connection.getServer())) {
                pollDeployments(connection);
            }
        }, POLL_DEPLOYMENTS_DELAY, ModalityState.any()));
    }

    @Nullable
    public static ServersTreeNodeSelector getNodeSelector(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return null;
        }

        return getInstance(project).getNodeSelector();
    }

    public static MessagePanel createMessagePanel() {
        return new ServersToolWindowMessagePanel();
    }

    public interface MessagePanel {
        void setEmptyText(@Nonnull String text);

        @Nonnull
        JComponent getComponent();
    }

    private static class ServersTreeNodeManipulator implements ServersTreeNodeSelector, ServerTreeNodeExpander {
        private final Project myProject;

        ServersTreeNodeManipulator(Project project) {
            myProject = project;
        }

        @Override
        public void select(@Nonnull ServerConnection<?> connection) {
            RemoteServersServiceViewContributor contributor = getInstance(myProject).findContributor(connection.getServer());
            if (contributor == null) {
                return;
            }

            ServersTreeStructure.RemoteServerNode serverNode = new ServersTreeStructure.RemoteServerNode(myProject, connection.getServer(), contributor);
            ServiceViewManager.getInstance(myProject).select(serverNode, contributor.getClass(), true, true);
        }

        @Override
        public void select(@Nonnull ServerConnection<?> connection, @Nonnull String deploymentName) {
            RemoteServersServiceViewContributor contributor = getInstance(myProject).findContributor(connection.getServer());
            if (contributor == null) {
                return;
            }

            AbstractTreeNode<?> deploymentNode = findDeployment(contributor, connection, deploymentName);
            if (deploymentNode != null) {
                ServiceViewManager.getInstance(myProject).select(deploymentNode, contributor.getClass(), true, false);
            }
        }

        @Override
        public void select(@Nonnull ServerConnection<?> connection, @Nonnull String deploymentName, @Nonnull String logName) {
            RemoteServersServiceViewContributor contributor = getInstance(myProject).findContributor(connection.getServer());
            if (contributor == null) {
                return;
            }

            AbstractTreeNode<?> deploymentNode = findDeployment(contributor, connection, deploymentName);
            if (deploymentNode != null) {
                contributor.selectLog(deploymentNode, logName);
            }
        }

        @Override
        public void expand(@Nonnull ServerConnection<?> connection, @Nonnull String deploymentName) {
            RemoteServersServiceViewContributor contributor = getInstance(myProject).findContributor(connection.getServer());
            if (contributor == null) {
                return;
            }

            AbstractTreeNode<?> deploymentNode = findDeployment(contributor, connection, deploymentName);
            if (deploymentNode != null) {
                ServiceViewManager.getInstance(myProject).expand(deploymentNode, contributor.getClass());
            }
        }

        public AbstractTreeNode<?> findDeployment(RemoteServersServiceViewContributor contributor,
                                                  ServerConnection<?> connection,
                                                  String deploymentName) {
            ServersTreeStructure.RemoteServerNode serverNode = new ServersTreeStructure.RemoteServerNode(myProject, connection.getServer(), contributor);
            RemoteServersServiceViewContributor.RemoteServerNodeServiceViewContributor serverContributor = contributor.createNodeContributor(serverNode);
            myProject.getMessageBus().syncPublisher(ServiceEventListener.TOPIC).handle(ServiceEventListener.ServiceEvent.createEvent(
                ServiceEventListener.EventType.SERVICE_STRUCTURE_CHANGED, serverContributor, contributor.getClass()));

            for (Deployment deployment : connection.getDeployments()) {
                var runtime = deployment.getRuntime();

                if (deployment.getName().equals(deploymentName) ||
                    (runtime instanceof CloudApplicationRuntime &&
                        ((CloudApplicationRuntime) runtime).getApplicationName().equals(deploymentName))) {
                    return contributor.createDeploymentNode(connection, serverNode, deployment);
                }
            }
            return null;
        }
    }
}
