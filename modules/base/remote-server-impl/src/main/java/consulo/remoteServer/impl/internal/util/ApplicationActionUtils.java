// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.util;

import consulo.application.AppUIExecutor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.service.ServiceViewActionUtils;
import consulo.project.Project;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.impl.internal.ui.tree.DeploymentNode;
import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.ui.ServersTreeNodeSelector;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Contract;
import jakarta.annotation.Nullable;

public final class ApplicationActionUtils {
    private ApplicationActionUtils() {
    }

    public static @Nullable DeploymentNode getDeploymentTarget(@Nonnull AnActionEvent e) {
        return ServiceViewActionUtils.getTarget(e, DeploymentNode.class);
    }

    public static @Nullable Deployment getDeployment(@Nullable DeploymentNode node) {
        return node == null ? null : ObjectUtil.tryCast(node.getValue(), Deployment.class);
    }

    @Contract("null, _ -> null")
    public static @Nullable <T> T getApplicationRuntime(@Nullable DeploymentNode node, @Nonnull Class<T> clazz) {
        Deployment deployment = getDeployment(node);
        return deployment == null ? null : ObjectUtil.tryCast(deployment.getRuntime(), clazz);
    }

    public static @Nullable <T> T getApplicationRuntime(@Nonnull AnActionEvent e, @Nonnull Class<T> clazz) {
        Deployment deployment = getDeployment(getDeploymentTarget(e));
        return deployment == null ? null : ObjectUtil.tryCast(deployment.getRuntime(), clazz);
    }

    public static @Nonnull Runnable createLogSelector(@Nonnull Project project,
                                                      @Nonnull ServersTreeNodeSelector selector,
                                                      @Nonnull DeploymentNode node,
                                                      @Nonnull String logName) {
        SelectLogRunnable selectLogRunnable = new SelectLogRunnable(project, selector, node, logName);
        DisposableSelectLogRunnableWrapper wrapper = new DisposableSelectLogRunnableWrapper(selectLogRunnable);
        Disposer.register(project, wrapper);
        return wrapper;
    }

    private static final class DisposableSelectLogRunnableWrapper implements Runnable, Disposable {
        private volatile Runnable mySelectLogRunnable;

        private DisposableSelectLogRunnableWrapper(Runnable selectLogRunnable) {
            mySelectLogRunnable = selectLogRunnable;
        }

        @Override
        public void dispose() {
            mySelectLogRunnable = null;
        }

        @Override
        public void run() {
            Runnable selectLogRunnable = mySelectLogRunnable;
            if (selectLogRunnable != null) {
                selectLogRunnable.run();
                Disposer.dispose(this);
            }
        }
    }

    private static class SelectLogRunnable implements Runnable {
        private final Project myProject;
        private final ServersTreeNodeSelector mySelector;
        private final DeploymentNode myNode;
        private final String myLogName;

        SelectLogRunnable(@Nonnull Project project,
                          @Nonnull ServersTreeNodeSelector selector,
                          @Nonnull DeploymentNode node,
                          @Nonnull String logName) {
            myProject = project;
            mySelector = selector;
            myNode = node;
            myLogName = logName;
        }

        @Override
        public void run() {
            RemoteServer<?> server = (RemoteServer<?>) myNode.getServerNode().getValue();
            ServerConnection<?> connection = ServerConnectionManager.getInstance().getConnection(server);
            if (connection == null) return;

            Deployment deployment = findDeployment(connection);
            if (deployment == null) return;

            AppUIExecutor.onUiThread().expireWith(myProject).submit(() -> mySelector.select(connection, deployment.getName(), myLogName));
        }

        private Deployment findDeployment(ServerConnection<?> connection) {
            DeploymentRuntime applicationRuntime = getApplicationRuntime(myNode, DeploymentRuntime.class);
            for (Deployment deployment : connection.getDeployments()) {
                if (applicationRuntime == deployment.getRuntime()) {
                    return deployment;
                }
            }
            return null;
        }
    }
}
