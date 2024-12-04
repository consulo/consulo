// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui;

import consulo.component.util.text.UniqueNameGenerator;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.service.ServiceEventListener;
import consulo.execution.service.ServiceViewContributor;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.execution.service.ServiceViewProvidingContributor;
import consulo.navigation.ItemPresentation;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.impl.internal.configuration.RemoteServerListConfigurable;
import consulo.remoteServer.impl.internal.ui.tree.DeploymentNode;
import consulo.remoteServer.impl.internal.ui.tree.ServersTreeStructure;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class RemoteServersServiceViewContributor
    implements ServiceViewContributor<RemoteServersServiceViewContributor.RemoteServerNodeServiceViewContributor>,
    Comparator<RemoteServersServiceViewContributor.RemoteServerNodeServiceViewContributor>,
    ServersTreeStructure.DeploymentNodeProducer {
    public abstract boolean accept(@Nonnull RemoteServer<?> server);

    public abstract void selectLog(@Nonnull AbstractTreeNode<?> deploymentNode, @Nonnull String logName);

    public abstract @Nonnull ActionGroups getActionGroups();

    protected @Nonnull RemoteServerNodeServiceViewContributor createNodeContributor(@Nonnull AbstractTreeNode<?> node) {
        return new RemoteServerNodeServiceViewContributor(this, node);
    }

    @Override
    public @Nonnull List<RemoteServerNodeServiceViewContributor> getServices(@Nonnull Project project) {
        List<RemoteServerNodeServiceViewContributor> services = RemoteServersManager.getInstance().getServers().stream()
            .filter(this::accept)
            .map(server -> createNodeContributor(new ServersTreeStructure.RemoteServerNode(project, server, this)))
            .collect(Collectors.toList());
        RemoteServersDeploymentManager.getInstance(project).registerContributor(this);
        return services;
    }

    @Override
    public @Nonnull ServiceViewDescriptor getServiceDescriptor(@Nonnull Project project, @Nonnull RemoteServerNodeServiceViewContributor service) {
        return service.getViewDescriptor(project);
    }

    @Override
    public int compare(RemoteServerNodeServiceViewContributor o1, RemoteServerNodeServiceViewContributor o2) {
        Function<RemoteServerNodeServiceViewContributor, String> getName = contributor -> Optional.ofNullable(contributor)
            .map(RemoteServerNodeServiceViewContributor::asService)
            .map(o -> ObjectUtil.tryCast(o, ServersTreeStructure.RemoteServerNode.class))
            .map(node -> node.getServer().getName())
            .orElse(null);

        String name1 = getName.apply(o1);
        String name2 = getName.apply(o2);

        if (name1 == null || name2 == null) {
            if (name1 == null && name2 == null) {
                return 0;
            }
            return name1 == null ? -1 : 1;
        }

        return name1.compareTo(name2);
    }

    protected @Nullable ServiceEventListener.ServiceEvent createDeploymentsChangedEvent(@Nonnull ServerConnection<?> connection) {
        return ServiceEventListener.ServiceEvent.createResetEvent(this.getClass());
    }

    protected static @Nonnull ActionGroup getToolbarActions(@Nonnull ActionGroups groups) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(ActionManager.getInstance().getAction(groups.getMainToolbarID()));
        return group;
    }

    protected static @Nonnull ActionGroup getPopupActions(@Nonnull ActionGroups groups) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(ActionManager.getInstance().getAction(groups.getMainToolbarID()));
        group.add(ActionManager.getInstance().getAction(groups.getPopupID()));
        return group;
    }

    private static String generateUniqueServerName(ServerType<?> serverType) {
        List<RemoteServer<?>> servers = ContainerUtil.filter(
            RemoteServersManager.getInstance().getServers(),
            server -> server.getType().equals(serverType));
        return UniqueNameGenerator.generateUniqueName(serverType.getPresentableName().get(), s -> {
            for (RemoteServer<?> server : servers) {
                if (server.getName().equals(s)) {
                    return false;
                }
            }
            return true;
        });
    }

    @RequiredUIAccess
    public static <C extends ServerConfiguration> void addNewRemoteServer(@Nonnull Project project,
                                                                          @Nonnull ServerType<C> serverType,
                                                                          @Nullable Class<?> contributorClass) {
        ShowConfigurableService service = project.getApplication().getInstance(ShowConfigurableService.class);

        String name = generateUniqueServerName(serverType);
        RemoteServersManager remoteServersManager = RemoteServersManager.getInstance();
        RemoteServer<C> server = remoteServersManager.createServer(serverType, name);

        service.showAndSelect(project, RemoteServerListConfigurable.class, listConfigurable -> {
            MasterDetailsComponent.MyNode node = listConfigurable.addServerNode(server, true);

            listConfigurable.selectNodeInTree(node);
        });
    }

    public static class RemoteServerNodeDescriptor implements ServiceViewDescriptor {
        private final AbstractTreeNode<?> myNode;
        private final ActionGroups myActionGroups;

        protected RemoteServerNodeDescriptor(@Nonnull AbstractTreeNode<?> node, @Nonnull ActionGroups actionGroups) {
            myNode = node;
            myActionGroups = actionGroups;
        }

        @Override
        public @Nullable String getId() {
            if (myNode instanceof ServersTreeStructure.RemoteServerNode) {
                ((ServersTreeStructure.RemoteServerNode) myNode).getServer().getName();
            }
            if (myNode instanceof DeploymentNode) {
                ((DeploymentNode) myNode).getDeploymentName();
            }
            return getPresentation().getPresentableText();
        }

        @Override
        public JComponent getContentComponent() {
            if (myNode instanceof ServersTreeStructure.LogProvidingNode) {
                return ((ServersTreeStructure.LogProvidingNode) myNode).getComponent();
            }
            else if (myNode instanceof ServersTreeStructure.RemoteServerNode) {
                return RemoteServersDeploymentManager.getInstance(myNode.getProject()).getServerContent(((ServersTreeStructure.RemoteServerNode) myNode).getServer());
            }
            return null;
        }

        @Override
        public ActionGroup getToolbarActions() {
            return RemoteServersServiceViewContributor.getToolbarActions(myActionGroups);
        }

        @Override
        public ActionGroup getPopupActions() {
            return RemoteServersServiceViewContributor.getPopupActions(myActionGroups);
        }

        @Override
        public @Nonnull ItemPresentation getPresentation() {
            return myNode.getPresentation();
        }

        @Override
        public boolean handleDoubleClick(@Nonnull MouseEvent event) {
            AnAction connectAction = ActionManager.getInstance().getAction("RemoteServers.ConnectServer");
            DataContext dataContext = DataManager.getInstance().getDataContext(event.getComponent());
            AnActionEvent actionEvent = AnActionEvent.createFromAnAction(connectAction, event, ActionPlaces.UNKNOWN, dataContext);
            connectAction.actionPerformed(actionEvent);
            return true;
        }

        @Override
        public @Nullable Runnable getRemover() {
            AbstractTreeNode<?> node = getNode();
            if (node instanceof ServersTreeStructure.RemoteServerNode) {
                return () -> RemoteServersManager.getInstance().removeServer(((ServersTreeStructure.RemoteServerNode) node).getServer());
            }
            return null;
        }

        protected @Nonnull AbstractTreeNode<?> getNode() {
            return myNode;
        }
    }

    public static class RemoteServerNodeServiceViewContributor
        implements ServiceViewProvidingContributor<RemoteServerNodeServiceViewContributor, AbstractTreeNode<?>> {
        private final RemoteServersServiceViewContributor myRootContributor;
        private final AbstractTreeNode<?> myNode;

        protected RemoteServerNodeServiceViewContributor(@Nonnull RemoteServersServiceViewContributor rootContributor,
                                                         @Nonnull AbstractTreeNode<?> node) {
            myRootContributor = rootContributor;
            myNode = node;
        }

        @Override
        public @Nonnull AbstractTreeNode<?> asService() {
            return myNode;
        }

        @Override
        public @Nonnull ServiceViewDescriptor getViewDescriptor(@Nonnull Project project) {
            return new RemoteServerNodeDescriptor(myNode, myRootContributor.getActionGroups());
        }

        @Override
        public @Nonnull List<RemoteServerNodeServiceViewContributor> getServices(@Nonnull Project project) {
            return ContainerUtil.map(myNode.getChildren(), myRootContributor::createNodeContributor);
        }

        @Override
        public @Nonnull ServiceViewDescriptor getServiceDescriptor(@Nonnull Project project, @Nonnull RemoteServerNodeServiceViewContributor service) {
            return service.getViewDescriptor(project);
        }

        protected @Nonnull RemoteServersServiceViewContributor getRootContributor() {
            return myRootContributor;
        }
    }

    public static class ActionGroups {
        private final @Nonnull String myMainToolbarID;
        private final @Nonnull String mySecondaryToolbarID;
        private final @Nonnull String myPopupID;

        public ActionGroups(@Nonnull String mainToolbarID, @Nonnull String secondaryToolbarID, @Nonnull String popupID) {
            myMainToolbarID = mainToolbarID;
            mySecondaryToolbarID = secondaryToolbarID;
            myPopupID = popupID;
        }

        public @Nonnull String getMainToolbarID() {
            return myMainToolbarID;
        }

        public @Nonnull String getPopupID() {
            return myPopupID;
        }

        public @Nonnull String getSecondaryToolbarID() {
            return mySecondaryToolbarID;
        }

        public static final ActionGroups SHARED_ACTION_GROUPS = new ActionGroups(
            "RemoteServersViewToolbar", "RemoteServersViewToolbar.Top", "RemoteServersViewPopup");
    }
}
