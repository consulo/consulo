package consulo.remoteServer.runtime;

import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.remoteServer.runtime.deployment.DeploymentTask;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author nik
 */
public interface ServerConnection<D extends DeploymentConfiguration> {
  @Nonnull
  RemoteServer<?> getServer();

  @Nonnull
  ConnectionStatus getStatus();

  @Nonnull
  String getStatusText();

  void connect(@Nonnull Runnable onFinished);

  void disconnect();

  void deploy(@Nonnull DeploymentTask<D> task, @Nonnull Consumer<String> onDeploymentStarted);

  void computeDeployments(@Nonnull Runnable onFinished);

  void undeploy(@Nonnull Deployment deployment, @Nonnull DeploymentRuntime runtime);

  @Nonnull
  Collection<Deployment> getDeployments();

  @javax.annotation.Nullable
  DeploymentLogManager getLogManager(@Nonnull String deployment);

  DeploymentLogManager getLogManager(@Nonnull Deployment deployment);

  void connectIfNeeded(ServerConnector.ConnectionCallback<D> callback);
}
