package consulo.remoteServer.runtime;

import consulo.localize.LocalizeValue;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;

public interface RemoteOperationCallback {
    void errorOccurred(LocalizeValue errorMessage);

    default void errorOccurred(LocalizeValue errorMessage, DeploymentRuntime failedDeployment) {
        errorOccurred(errorMessage);
    }
}
