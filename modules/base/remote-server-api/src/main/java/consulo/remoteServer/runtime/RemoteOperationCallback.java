package consulo.remoteServer.runtime;

import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

public interface RemoteOperationCallback {
    void errorOccurred(@Nonnull @Nls String errorMessage);

    default void errorOccurred(@Nonnull @Nls String errorMessage, DeploymentRuntime failedDeployment) {
        errorOccurred(errorMessage);
    }
}
