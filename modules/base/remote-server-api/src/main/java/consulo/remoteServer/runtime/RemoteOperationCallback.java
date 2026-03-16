package consulo.remoteServer.runtime;

import consulo.remoteServer.runtime.deployment.DeploymentRuntime;

public interface RemoteOperationCallback {
    void errorOccurred(String errorMessage);

    default void errorOccurred(String errorMessage, DeploymentRuntime failedDeployment) {
        errorOccurred(errorMessage);
    }
}
