// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime.deployment;

import consulo.project.Project;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.impl.internal.runtime.ServerConnectionImpl;
import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentStatus;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nullable;

public class DeploymentImpl<D extends DeploymentConfiguration> implements Deployment {
    private final ServerConnectionImpl<D> myConnection;
    private final String myName;
    private final DeploymentTask<D> myDeploymentTask;
    private volatile DeploymentState myState;
    private @Nls String myPresentableName;

    public DeploymentImpl(@Nonnull ServerConnectionImpl<D> connection,
                          @Nonnull String name,
                          @Nonnull DeploymentStatus status,
                          @Nullable @Nls String statusText,
                          @Nullable DeploymentRuntime runtime,
                          @Nullable DeploymentTask<D> deploymentTask) {
        myConnection = connection;
        myName = name;
        myDeploymentTask = deploymentTask;
        myState = new DeploymentState(status, statusText, runtime);
    }

    @Override
    public @Nonnull String getName() {
        return myName;
    }

    @Override
    public @Nonnull DeploymentStatus getStatus() {
        return myState.getStatus();
    }

    @Override
    public @Nonnull @Nls String getStatusText() {
        String statusText = myState.getStatusText();
        return statusText != null ? statusText : myState.getStatus().getPresentableText().get();
    }

    @Override
    public DeploymentRuntime getRuntime() {
        return myState.getRuntime();
    }

    @Override
    public @Nullable DeploymentTask<D> getDeploymentTask() {
        return myDeploymentTask;
    }

    @Override
    public @Nonnull DeploymentLogManager getOrCreateLogManager(@Nonnull Project project) {
        return myConnection.getOrCreateLogManager(project, this);
    }

    public void disposeAllLogs() {
        myConnection.disposeAllLogs(this);
    }

    @Override
    public void setStatus(final @Nonnull DeploymentStatus status, final @Nullable @Nls String statusText) {
        myConnection.changeDeploymentState(this, getRuntime(), myState.getStatus(), status, statusText);
    }

    @Override
    public @Nonnull ServerConnection<?> getConnection() {
        return myConnection;
    }

    @Override
    public @Nullable DeploymentRuntime getParentRuntime() {
        DeploymentRuntime runtime = getRuntime();
        return runtime == null ? null : runtime.getParent();
    }

    public boolean changeState(@Nonnull DeploymentStatus oldStatus,
                               @Nonnull DeploymentStatus newStatus,
                               @Nullable @Nls String statusText,
                               @Nullable DeploymentRuntime runtime) {
        if (myState.getStatus() == oldStatus) {
            myState = new DeploymentState(newStatus, statusText, runtime);
            return true;
        }
        return false;
    }

    @Override
    public @Nonnull String getPresentableName() {
        return myPresentableName == null ? getName() : myPresentableName;
    }

    public void setPresentableName(@Nls String presentableName) {
        myPresentableName = presentableName;
    }

    protected static final class DeploymentState {
        private final DeploymentStatus myStatus;
        private final @Nls String myStatusText;
        private final DeploymentRuntime myRuntime;

        private DeploymentState(@Nonnull DeploymentStatus status, @Nullable @Nls String statusText, @Nullable DeploymentRuntime runtime) {
            myStatus = status;
            myStatusText = statusText;
            myRuntime = runtime;
        }

        public @Nonnull DeploymentStatus getStatus() {
            return myStatus;
        }

        public @Nullable @Nls String getStatusText() {
            return myStatusText;
        }

        public @Nullable DeploymentRuntime getRuntime() {
            return myRuntime;
        }
    }
}
