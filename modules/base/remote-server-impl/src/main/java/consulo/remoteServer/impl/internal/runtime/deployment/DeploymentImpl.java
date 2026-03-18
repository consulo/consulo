// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime.deployment;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.impl.internal.runtime.ServerConnectionImpl;
import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentStatus;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import org.jspecify.annotations.Nullable;

public class DeploymentImpl<D extends DeploymentConfiguration> implements Deployment {
    private final ServerConnectionImpl<D> myConnection;
    private final String myName;
    private final DeploymentTask<D> myDeploymentTask;
    private volatile DeploymentState myState;
    private String myPresentableName;

    public DeploymentImpl(ServerConnectionImpl<D> connection,
                          String name,
                          DeploymentStatus status,
                          LocalizeValue statusText,
                          @Nullable DeploymentRuntime runtime,
                          @Nullable DeploymentTask<D> deploymentTask) {
        myConnection = connection;
        myName = name;
        myDeploymentTask = deploymentTask;
        myState = new DeploymentState(status, statusText, runtime);
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public DeploymentStatus getStatus() {
        return myState.getStatus();
    }

    @Override
    public LocalizeValue getStatusText() {
        LocalizeValue statusText = myState.getStatusText();
        return statusText.orIfEmpty(myState.getStatus().getPresentableText());
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
    public DeploymentLogManager getOrCreateLogManager(Project project) {
        return myConnection.getOrCreateLogManager(project, this);
    }

    public void disposeAllLogs() {
        myConnection.disposeAllLogs(this);
    }

    @Override
    public void setStatus(DeploymentStatus status, LocalizeValue statusText) {
        myConnection.changeDeploymentState(this, getRuntime(), myState.getStatus(), status, statusText);
    }

    @Override
    public ServerConnection<?> getConnection() {
        return myConnection;
    }

    @Override
    public @Nullable DeploymentRuntime getParentRuntime() {
        DeploymentRuntime runtime = getRuntime();
        return runtime == null ? null : runtime.getParent();
    }

    public boolean changeState(DeploymentStatus oldStatus,
                               DeploymentStatus newStatus,
                               LocalizeValue statusText,
                               @Nullable DeploymentRuntime runtime) {
        if (myState.getStatus() == oldStatus) {
            myState = new DeploymentState(newStatus, statusText, runtime);
            return true;
        }
        return false;
    }

    @Override
    public String getPresentableName() {
        return myPresentableName == null ? getName() : myPresentableName;
    }

    public void setPresentableName(String presentableName) {
        myPresentableName = presentableName;
    }

    protected static final class DeploymentState {
        private final DeploymentStatus myStatus;
        private final LocalizeValue myStatusText;
        private final DeploymentRuntime myRuntime;

        private DeploymentState(DeploymentStatus status, LocalizeValue statusText, @Nullable DeploymentRuntime runtime) {
            myStatus = status;
            myStatusText = statusText;
            myRuntime = runtime;
        }

        public DeploymentStatus getStatus() {
            return myStatus;
        }

        public LocalizeValue getStatusText() {
            return myStatusText;
        }

        public @Nullable DeploymentRuntime getRuntime() {
            return myRuntime;
        }
    }
}
