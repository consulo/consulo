package consulo.remoteServer.runtime;

import consulo.project.Project;
import consulo.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentStatus;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import org.jspecify.annotations.Nullable;

public interface Deployment {
    
    String getName();

    
    
    String getPresentableName();

    
    DeploymentStatus getStatus();

    
    
    String getStatusText();

    @Nullable
    DeploymentRuntime getRuntime();

    @Nullable
    DeploymentRuntime getParentRuntime();

    @Nullable
    DeploymentTask<?> getDeploymentTask();

    
    DeploymentLogManager getOrCreateLogManager(Project project);

    void setStatus(DeploymentStatus status, @Nullable String statusText);

    
    ServerConnection<?> getConnection();
}
