// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime.deployment;

import consulo.localize.LocalizeValue;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.impl.internal.runtime.ServerConnectionImpl;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentStatus;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import consulo.remoteServer.runtime.deployment.ServerRuntimeInstance;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class LocalDeploymentImpl<D extends DeploymentConfiguration> extends DeploymentImpl<D> {

    private final ServerRuntimeInstance<D> myServerInstance;
    private DeploymentImpl myRemoteDeployment;

    public LocalDeploymentImpl(ServerRuntimeInstance<D> instance,
                               ServerConnectionImpl<D> connection,
                               DeploymentStatus status,
                               LocalizeValue statusText,
                               @Nullable DeploymentRuntime runtime,
                               DeploymentTask<D> deploymentTask) {
        super(connection,
            instance.getDeploymentName(deploymentTask.getSource(), deploymentTask.getConfiguration()),
            status,
            statusText,
            runtime,
            deploymentTask);
        myServerInstance = instance;
    }

    public void setRemoteDeployment(DeploymentImpl remoteDeployment) {
        myRemoteDeployment = remoteDeployment;
        String presentableName = null;
        if (remoteDeployment != null) {
            DeploymentRuntime deploymentRuntime = remoteDeployment.getRuntime();
            DeploymentTask<D> task = getDeploymentTask();
            if (deploymentRuntime != null) {
                presentableName = myServerInstance.getRuntimeDeploymentName(deploymentRuntime, task.getSource(), task.getConfiguration());
            }
        }
        setPresentableName(presentableName);
    }

    @Override
    public DeploymentTask<D> getDeploymentTask() {
        return Objects.requireNonNull(super.getDeploymentTask());
    }

    private boolean isLocalState() {
        return myRemoteDeployment == null || super.getStatus().isTransition();
    }

    @Override
    public DeploymentStatus getStatus() {
        return isLocalState() ? super.getStatus() : myRemoteDeployment.getStatus();
    }

    @Override
    public LocalizeValue getStatusText() {
        return isLocalState() ? super.getStatusText() : myRemoteDeployment.getStatusText();
    }

    public @Nullable DeploymentRuntime getRemoteRuntime() {
        return isLocalState() ? null : myRemoteDeployment.getRuntime();
    }

    @Override
    public boolean changeState(DeploymentStatus oldStatus,
                               DeploymentStatus newStatus,
                               LocalizeValue statusText,
                               @Nullable DeploymentRuntime runtime) {
        boolean result = super.changeState(oldStatus, newStatus, statusText, runtime);
        if (result && myRemoteDeployment != null) {
            myRemoteDeployment.changeState(myRemoteDeployment.getStatus(), newStatus, statusText, myRemoteDeployment.getRuntime());
        }
        return result;
    }

    public boolean hasRemoteDeloyment() {
        return myRemoteDeployment != null;
    }
}
