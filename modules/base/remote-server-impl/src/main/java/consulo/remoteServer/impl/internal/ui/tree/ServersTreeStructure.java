// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree;

import consulo.application.Application;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunConfigurationEditor;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.icon.ExecutionIconGroup;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.remoteServer.CloudBundle;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurationManager;
import consulo.remoteServer.impl.internal.configuration.RemoteServerListConfigurable;
import consulo.remoteServer.impl.internal.configuration.deployment.DeployToServerRunConfiguration;
import consulo.remoteServer.impl.internal.runtime.deployment.DeploymentTaskImpl;
import consulo.remoteServer.impl.internal.runtime.log.DeploymentLogManagerImpl;
import consulo.remoteServer.impl.internal.runtime.log.LoggingHandlerBase;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.remoteServer.runtime.ConnectionStatus;
import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentStatus;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import consulo.remoteServer.runtime.deployment.SingletonDeploymentSourceType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * @author michael.golubev
 */
public final class ServersTreeStructure {
    // 1st level: servers (RunnerAndConfigurationSettings (has CommonStrategy (extends RunConfiguration)) or RemoteServer)
    // 2nd level: deployments (DeploymentModel or Deployment)

    private ServersTreeStructure() {
    }

    public static Image getServerNodeIcon(@Nonnull Image itemIcon, @Nullable Image statusIcon) {
        if (statusIcon == null) {
            return itemIcon;
        }

        return ImageEffects.layered(itemIcon, statusIcon);
    }

    public interface LogProvidingNode {
        @Nullable
        JComponent getComponent();

        @Nonnull
        String getLogId();
    }

    public static class RemoteServerNode extends AbstractTreeNode<RemoteServer<?>> implements ServerNode {
        private final DeploymentNodeProducer myNodeProducer;

        public RemoteServerNode(Project project, @Nonnull RemoteServer<?> server, @Nonnull DeploymentNodeProducer nodeProducer) {
            super(project, server);
            myNodeProducer = nodeProducer;
        }

        public @Nonnull RemoteServer<?> getServer() {
            return getValue();
        }

        @Override
        public @Nonnull Collection<? extends AbstractTreeNode<?>> getChildren() {
            final ServerConnection<?> connection = getConnection();
            if (connection == null) {
                return Collections.emptyList();
            }

            final List<AbstractTreeNode<?>> children = new ArrayList<>();
            for (Deployment deployment : connection.getDeployments()) {
                if (deployment.getParentRuntime() == null) {
                    children.add(myNodeProducer.createDeploymentNode(connection, this, deployment));
                }
            }
            return children;
        }

        @Override
        protected void update(@Nonnull PresentationData presentation) {
            RemoteServer<?> server = getServer();
            ServerConnection<?> connection = getConnection();
            presentation.setPresentableText(server.getName());

            Image icon;

            icon = getServerNodeIcon(server.getType().getIcon(), connection != null ? getStatusIcon(connection.getStatus()) : null);

            presentation.setIcon(icon);

            presentation.setTooltip(connection != null ? connection.getStatusText() : null);
        }

        private @Nullable ServerConnection<?> getConnection() {
            return ServerConnectionManager.getInstance().getConnection(getServer());
        }

        public boolean isConnected() {
            ServerConnection<?> connection = getConnection();
            return connection != null && connection.getStatus() == ConnectionStatus.CONNECTED;
        }

        public void deploy(@Nonnull AnActionEvent e) {
            doDeploy(e, DefaultRunExecutor.getRunExecutorInstance(),
                CloudBundle.message("ServersTreeStructure.RemoteServerNode.popup.title.deploy.configuration"), true);
        }

        public void deployWithDebug(@Nonnull AnActionEvent e) {
            doDeploy(e, DefaultDebugExecutor.getDebugExecutorInstance(),
                CloudBundle.message("ServersTreeStructure.RemoteServerNode.popup.title.deploy.debug.configuration"), false);
        }

        public void doDeploy(@Nonnull AnActionEvent e, final Executor executor, String popupTitle, boolean canCreate) {
            final RemoteServer<?> server = getServer();
            final ServerType<? extends ServerConfiguration> serverType = server.getType();
            final DeploymentConfigurationManager configurationManager = DeploymentConfigurationManager.getInstance(myProject);

            final List<Object> runConfigsAndTypes = new LinkedList<>();
            final List<RunnerAndConfigurationSettings> runConfigs =
                ContainerUtil.filter(configurationManager.getDeploymentConfigurations(serverType), settings -> {
                    DeployToServerRunConfiguration<?, ?> configuration = (DeployToServerRunConfiguration<?, ?>) settings.getConfiguration();
                    return StringUtil.equals(server.getName(), configuration.getServerName());
                });
            runConfigsAndTypes.addAll(runConfigs);

            if (canCreate) {
                runConfigsAndTypes.addAll(server.getType().getSingletonDeploymentSourceTypes());
                if (server.getType().mayHaveProjectSpecificDeploymentSources()) {
                    runConfigsAndTypes.add(null);
                }
            }

            ListPopup popup =
                JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<Object>(popupTitle, runConfigsAndTypes) {
                    @Override
                    public Image getIconFor(Object runConfigOrSourceType) {
                        return runConfigOrSourceType != null ? serverType.getIcon() : null;
                    }

                    @Override
                    public @Nonnull String getTextFor(Object runConfigOrSourceType) {
                        if (runConfigOrSourceType instanceof RunnerAndConfigurationSettings) {
                            return ((RunnerAndConfigurationSettings) runConfigOrSourceType).getName();
                        }
                        if (runConfigOrSourceType instanceof SingletonDeploymentSourceType) {
                            String displayName = ((SingletonDeploymentSourceType) runConfigOrSourceType).getPresentableName().get();
                            return CloudBundle.message("create.new.deployment.configuration.for.singleton.type", displayName);
                        }
                        return CloudBundle.message("create.new.deployment.configuration.generic");
                    }

                    @Override
                    public PopupStep<?> onChosen(Object selectedRunConfigOrSourceType, boolean finalChoice) {
                        return doFinalStep(() -> {
                            if (selectedRunConfigOrSourceType instanceof RunnerAndConfigurationSettings) {
                                ProgramRunnerUtil.executeConfiguration((RunnerAndConfigurationSettings) selectedRunConfigOrSourceType, executor);
                            }
                            else if (selectedRunConfigOrSourceType instanceof SingletonDeploymentSourceType sourceType) {
                                configurationManager.createAndRunConfiguration(serverType, RemoteServerNode.this.getValue(), sourceType);
                            }
                            else {
                                assert selectedRunConfigOrSourceType == null;
                                configurationManager.createAndRunConfiguration(serverType, RemoteServerNode.this.getValue(), null);
                            }
                        });
                    }
                });
            if (e.getInputEvent() instanceof MouseEvent) {
                popup.show(new RelativePoint((MouseEvent) e.getInputEvent()));
            }
            else {
                popup.showInBestPositionFor(e.getDataContext());
            }
        }

        @RequiredUIAccess
        public void editConfiguration() {
            Application.get().getInstance(ShowConfigurableService.class).showAndSelect(myProject, RemoteServerListConfigurable.class, remoteServerListConfigurable -> {
                remoteServerListConfigurable.selectNodeInTree(getValue());
            });
        }

        @Nullable
        private static Image getStatusIcon(final ConnectionStatus status) {
            return switch (status) {
                case CONNECTED -> PlatformIconGroup.remoteserversResumescaled();
                case DISCONNECTED -> PlatformIconGroup.remoteserversSuspendscaled();
                default -> null;
            };
        }
    }

    public static class DeploymentNodeImpl extends AbstractTreeNode<Deployment> implements LogProvidingNode, DeploymentNode {
        private final ServerConnection<?> myConnection;
        private final RemoteServerNode myServerNode;
        private final DeploymentNodeProducer myNodeProducer;

        public DeploymentNodeImpl(Project project,
                                  @Nonnull ServerConnection<?> connection,
                                  @Nonnull RemoteServerNode serverNode,
                                  @Nonnull Deployment value,
                                  @Nonnull DeploymentNodeProducer nodeProducer) {
            super(project, value);
            myConnection = connection;
            myServerNode = serverNode;
            myNodeProducer = nodeProducer;
        }

        public @Nonnull Deployment getDeployment() {
            return getValue();
        }

        @Override
        public @Nonnull RemoteServerNode getServerNode() {
            return myServerNode;
        }

        @Override
        public boolean isDeployActionVisible() {
            DeploymentTask<?> deploymentTask = getValue().getDeploymentTask();
            return deploymentTask instanceof DeploymentTaskImpl<?> && deploymentTask
                .getExecutionEnvironment().getRunnerAndConfigurationSettings() != null;
        }

        @Override
        public boolean isDeployActionEnabled() {
            return true;
        }

        @Override
        public void deploy() {
            doDeploy(DefaultRunExecutor.getRunExecutorInstance());
        }

        public void doDeploy(Executor executor) {
            DeploymentTask<?> deploymentTask = getDeployment().getDeploymentTask();
            if (deploymentTask != null) {
                ExecutionEnvironment environment = deploymentTask.getExecutionEnvironment();
                RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
                if (settings != null) {
                    ProgramRunnerUtil.executeConfiguration(settings, executor);
                }
            }
        }

        @Override
        public boolean isDebugActionVisible() {
            return myServerNode.getServer().getType().createDebugConnector() != null;
        }

        @Override
        public void deployWithDebug() {
            doDeploy(DefaultDebugExecutor.getDebugExecutorInstance());
        }

        @Override
        public boolean isUndeployActionEnabled() {
            DeploymentRuntime runtime = getDeployment().getRuntime();
            return runtime != null && runtime.isUndeploySupported();
        }

        @Override
        public void undeploy() {
            DeploymentRuntime runtime = getDeployment().getRuntime();
            if (runtime != null) {
                getConnection().undeploy(getDeployment(), runtime);
            }
        }

        public boolean isEditConfigurationActionVisible() {
            return getDeployment().getDeploymentTask() != null;
        }

        public void editConfiguration() {
            DeploymentTask<?> task = getDeployment().getDeploymentTask();
            if (task != null) {
                RunnerAndConfigurationSettings settings = task.getExecutionEnvironment().getRunnerAndConfigurationSettings();
                if (settings != null) {
                    RunConfigurationEditor.getInstance(myProject).editConfiguration(myProject, settings, RemoteServerLocalize.dialogTitleEditDeploymentConfiguration().get());
                }
            }
        }

        @Override
        public boolean isDeployed() {
            return getDeployment().getStatus() == DeploymentStatus.DEPLOYED;
        }

        @Override
        public String getDeploymentName() {
            return getDeployment().getName();
        }

        public ServerConnection<?> getConnection() {
            return myConnection;
        }

        @Override
        public @Nullable JComponent getComponent() {
            DeploymentLogManagerImpl logManager = getLogManager();
            return logManager != null && logManager.isMainHandlerVisible()
                ? logManager.getMainLoggingHandler().getConsole().getComponent()
                : null;
        }

        protected @Nullable DeploymentLogManagerImpl getLogManager() {
            return (DeploymentLogManagerImpl) myConnection.getLogManager(myProject, getDeployment());
        }

        public String getId() {
            return myServerNode.getName() + ";deployment" + getDeployment().getName();
        }

        @Override
        public @Nonnull String getLogId() {
            return getId() + ";main-log";
        }

        @Override
        public @Nonnull Collection<? extends AbstractTreeNode<?>> getChildren() {
            List<AbstractTreeNode<?>> result = new ArrayList<>();
            collectDeploymentChildren(result);
            collectLogChildren(result);
            return result;
        }

        protected void collectDeploymentChildren(List<? super AbstractTreeNode<?>> children) {
            ServerConnection<?> connection = getConnection();
            if (connection == null) {
                return;
            }
            for (Deployment deployment : connection.getDeployments()) {
                DeploymentRuntime parent = deployment.getParentRuntime();
                if (parent != null && parent == getDeployment().getRuntime()) {
                    children.add(myNodeProducer.createDeploymentNode(connection, myServerNode, deployment));
                }
            }
        }

        protected void collectLogChildren(List<? super AbstractTreeNode<?>> children) {
            ServerConnection<?> connection = getConnection();
            if (connection == null) {
                return;
            }
            DeploymentLogManagerImpl logManager = (DeploymentLogManagerImpl) connection.getLogManager(myProject, getDeployment());
            if (logManager != null) {
                for (LoggingHandlerBase loggingComponent : logManager.getAdditionalLoggingHandlers()) {
                    children.add(new DeploymentLogNode(myProject, loggingComponent, this));
                }
            }
        }

        @Override
        protected void update(@Nonnull PresentationData presentation) {
            Deployment deployment = getDeployment();
            presentation.setIcon(deployment.getStatus().getIcon());
            presentation.setPresentableText(deployment.getPresentableName());
            presentation.setTooltip(deployment.getStatusText());
        }
    }

    public static class DeploymentLogNode extends AbstractTreeNode<LoggingHandlerBase> implements ServersTreeNode, LogProvidingNode {
        private final @Nonnull DeploymentNodeImpl myDeploymentNode;

        public DeploymentLogNode(Project project, @Nonnull LoggingHandlerBase value, @Nonnull DeploymentNodeImpl deploymentNode) {
            super(project, value);
            myDeploymentNode = deploymentNode;
        }

        @Override
        public @Nonnull Collection<? extends AbstractTreeNode<?>> getChildren() {
            return Collections.emptyList();
        }

        @Override
        protected void update(@Nonnull PresentationData presentation) {
            presentation.setIcon(ExecutionIconGroup.console());
            presentation.setPresentableText(getLogName());
        }

        private String getLogName() {
            return getValue().getPresentableName();
        }

        @Override
        public @Nullable JComponent getComponent() {
            return getValue().getComponent();
        }

        @Override
        public @Nonnull String getLogId() {
            return myDeploymentNode.getId() + ";log:" + getLogName();
        }
    }

    @FunctionalInterface
    public interface DeploymentNodeProducer {
        AbstractTreeNode<?> createDeploymentNode(ServerConnection<?> connection, RemoteServerNode serverNode, Deployment deployment);
    }
}
