// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.util;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.agent.shared.util.CloudAgentLoggingHandler;
import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.ServerTaskExecutor;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class CloudApplicationRuntime extends DeploymentRuntime {

    private static final Logger LOG = Logger.getInstance(CloudApplicationRuntime.class);


    private final String myApplicationName;
    private Deployment myDeployment;

    public CloudApplicationRuntime(String applicationName) {
        myApplicationName = applicationName;
    }

    public String getApplicationName() {
        return myApplicationName;
    }

    public @Nullable DeploymentStatus getStatus() {
        return null;
    }

    public @Nullable String getStatusText() {
        return null;
    }

    public void setDeploymentModel(@Nonnull Deployment deployment) {
        myDeployment = deployment;
    }

    public Deployment getDeploymentModel() {
        return myDeployment;
    }

    public CloudNotifier getCloudNotifier() {
        return new CloudNotifier(getCloudType().getPresentableName().get());
    }

    protected abstract ServerTaskExecutor getTaskExecutor();

    protected abstract AgentTaskExecutor getAgentTaskExecutor();

    protected abstract ServerType<?> getCloudType();

    protected abstract class LoggingTask {

        public void perform(final Project project, final Runnable onDone) {
            getTaskExecutor().submit(() -> {
                try {
                    getAgentTaskExecutor().execute(() -> {
                        Deployment deployment = getDeploymentModel();
                        CloudAgentLoggingHandler loggingHandler
                            = deployment == null
                            ? null
                            : new CloudLoggingHandlerImpl(deployment.getOrCreateLogManager(project)) {

                            @Override
                            public void println(String message) {
                                LOG.info(message);
                            }
                        };
                        this.run(loggingHandler);
                        return null;
                    });
                    onDone.run();
                }
                catch (ServerRuntimeException e) {
                    getCloudNotifier().showMessage(e.getMessage(), NotificationType.ERROR);
                }
            });
        }

        protected abstract void run(CloudAgentLoggingHandler loggingHandler);
    }
}
