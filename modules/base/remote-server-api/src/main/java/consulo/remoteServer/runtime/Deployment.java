package consulo.remoteServer.runtime;

import consulo.project.Project;
import consulo.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentStatus;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nullable;

public interface Deployment {
    @Nonnull
    String getName();

    @Nonnull
    @Nls
    String getPresentableName();

    @Nonnull
    DeploymentStatus getStatus();

    @Nonnull
    @Nls
    String getStatusText();

    @Nullable
    DeploymentRuntime getRuntime();

    @Nullable
    DeploymentRuntime getParentRuntime();

    @Nullable
    DeploymentTask<?> getDeploymentTask();

    @Nonnull
    DeploymentLogManager getOrCreateLogManager(@Nonnull Project project);

    void setStatus(@Nonnull DeploymentStatus status, @Nullable String statusText);

    @Nonnull
    ServerConnection<?> getConnection();
}
