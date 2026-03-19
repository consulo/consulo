package consulo.remoteServer.runtime.deployment;

import consulo.remoteServer.runtime.RemoteOperationCallback;
import org.jspecify.annotations.Nullable;

public abstract class DeploymentRuntime {
    public boolean isUndeploySupported() {
        return true;
    }

    public abstract void undeploy(UndeploymentTaskCallback callback);

    public @Nullable DeploymentRuntime getParent() {
        return null;
    }

    public interface UndeploymentTaskCallback extends RemoteOperationCallback {
        void succeeded();
    }
}
