package consulo.ide.impl.idea.remoteServer.runtime;

import consulo.ide.impl.idea.remoteServer.configuration.RemoteServer;
import consulo.ide.impl.idea.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.ide.impl.idea.remoteServer.runtime.deployment.DeploymentLogManager;
import consulo.ide.impl.idea.remoteServer.runtime.deployment.DeploymentRuntime;
import consulo.ide.impl.idea.remoteServer.runtime.deployment.DeploymentTask;
import consulo.ide.impl.idea.util.ParameterizedRunnable;
import javax.annotation.Nonnull;

import java.util.Collection;

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

  void deploy(@Nonnull DeploymentTask<D> task, @Nonnull ParameterizedRunnable<String> onDeploymentStarted);

  void computeDeployments(@Nonnull Runnable onFinished);

  void undeploy(@Nonnull Deployment deployment, @Nonnull DeploymentRuntime runtime);

  @Nonnull
  Collection<Deployment> getDeployments();

  @javax.annotation.Nullable
  DeploymentLogManager getLogManager(@Nonnull String deployment);

  DeploymentLogManager getLogManager(@Nonnull Deployment deployment);

  void connectIfNeeded(ServerConnector.ConnectionCallback<D> callback);
}
