package consulo.remoteServer.runtime.deployment;

import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.RemoteOperationCallback;
import org.jspecify.annotations.Nullable;

public abstract class ServerRuntimeInstance<D extends DeploymentConfiguration> {

    public abstract void deploy(DeploymentTask<D> task, DeploymentLogManager logManager,
                                DeploymentOperationCallback callback);

    public abstract void computeDeployments(ComputeDeploymentsCallback callback);

    
    public String getDeploymentName(DeploymentSource source, D configuration) {
        return source.getPresentableName().get();
    }

    
    
    public String getRuntimeDeploymentName(DeploymentRuntime deploymentRuntime,
                                           DeploymentSource source, D configuration) {
        return getDeploymentName(source, configuration);
    }

    public abstract void disconnect();

    public interface DeploymentOperationCallback extends RemoteOperationCallback {
        default void started(DeploymentRuntime deploymentRuntime) {
            //
        }

        Deployment succeeded(DeploymentRuntime deploymentRuntime);
    }

    public interface ComputeDeploymentsCallback extends RemoteOperationCallback {
        void addDeployment(String deploymentName);

        void addDeployment(String deploymentName, @Nullable DeploymentRuntime deploymentRuntime);

        Deployment addDeployment(String deploymentName,
                                 @Nullable DeploymentRuntime deploymentRuntime,
                                 @Nullable DeploymentStatus deploymentStatus,
                                 @Nullable String deploymentStatusText);

        void succeeded();
    }
}
