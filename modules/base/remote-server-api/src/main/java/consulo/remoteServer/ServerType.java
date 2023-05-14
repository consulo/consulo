package consulo.remoteServer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.configurable.UnnamedConfigurable;
import consulo.project.Project;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurator;
import consulo.remoteServer.runtime.ServerConnector;
import consulo.remoteServer.runtime.ServerTaskExecutor;
import consulo.remoteServer.runtime.deployment.debug.DebugConnector;
import consulo.remoteServer.runtime.local.LocalRunner;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ServerType<C extends ServerConfiguration> {
  public static final ExtensionPointName<ServerType> EP_NAME = ExtensionPointName.create(ServerType.class);
  private final String myId;

  protected ServerType(String id) {
    myId = id;
  }

  public final String getId() {
    return myId;
  }

  @Nonnull
  public abstract String getPresentableName();

  @Nonnull
  public abstract Image getIcon();

  @Nonnull
  public abstract C createDefaultConfiguration();

  @Nonnull
  public abstract UnnamedConfigurable createConfigurable(@Nonnull C configuration);

  @Nonnull
  public abstract DeploymentConfigurator<?> createDeploymentConfigurator(Project project);

  @Nonnull
  public abstract ServerConnector<?> createConnector(@Nonnull C configuration, @Nonnull ServerTaskExecutor asyncTasksExecutor);

  public boolean isConfigurationTypeIsAvailable(@Nonnull Project project) {
    return !RemoteServersManager.getInstance().getServers(this).isEmpty();
  }

  /**
   * @return a non-null instance of {@link DebugConnector} if the server supports deployment in debug mode
   */
  @Nullable
  public DebugConnector<?,?> createDebugConnector() {
    return null;
  }

  @Nullable
  public LocalRunner getLocalRunner() {
    return null;
  }
}
