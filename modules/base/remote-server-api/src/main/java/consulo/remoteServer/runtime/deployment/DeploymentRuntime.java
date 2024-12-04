package consulo.remoteServer.runtime.deployment;

import consulo.remoteServer.runtime.RemoteOperationCallback;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class DeploymentRuntime {
    public boolean isUndeploySupported() {
        return true;
    }

    public abstract void undeploy(@Nonnull UndeploymentTaskCallback callback);

    @Nullable
    public DeploymentRuntime getParent() {
        return null;
    }

    public interface UndeploymentTaskCallback extends RemoteOperationCallback {
        void succeeded();
    }
}
