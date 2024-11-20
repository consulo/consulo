package consulo.remoteServer.runtime;

import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public interface RemoteOperationCallback {
    void errorOccurred(@NotNull @Nls String errorMessage);

    default void errorOccurred(@NotNull @Nls String errorMessage, DeploymentRuntime failedDeployment) {
        errorOccurred(errorMessage);
    }
}
