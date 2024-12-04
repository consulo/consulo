package consulo.remoteServer.runtime.deployment;

import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.RemoteOperationCallback;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nullable;

public abstract class ServerRuntimeInstance<D extends DeploymentConfiguration> {

    public abstract void deploy(@Nonnull DeploymentTask<D> task, @Nonnull DeploymentLogManager logManager,
                                @Nonnull DeploymentOperationCallback callback);

    public abstract void computeDeployments(@Nonnull ComputeDeploymentsCallback callback);

    @Nonnull
    public String getDeploymentName(@Nonnull DeploymentSource source, @Nonnull D configuration) {
        return source.getPresentableName().get();
    }

    @Nonnull
    @Nls
    public String getRuntimeDeploymentName(@Nonnull DeploymentRuntime deploymentRuntime,
                                           @Nonnull DeploymentSource source, @Nonnull D configuration) {
        return getDeploymentName(source, configuration);
    }

    public abstract void disconnect();

    public interface DeploymentOperationCallback extends RemoteOperationCallback {
        default void started(@Nonnull DeploymentRuntime deploymentRuntime) {
            //
        }

        Deployment succeeded(@Nonnull DeploymentRuntime deploymentRuntime);
    }

    public interface ComputeDeploymentsCallback extends RemoteOperationCallback {
        void addDeployment(@Nonnull String deploymentName);

        void addDeployment(@Nonnull String deploymentName, @Nullable DeploymentRuntime deploymentRuntime);

        Deployment addDeployment(@Nonnull String deploymentName,
                                 @Nullable DeploymentRuntime deploymentRuntime,
                                 @Nullable DeploymentStatus deploymentStatus,
                                 @Nullable String deploymentStatusText);

        void succeeded();
    }
}
