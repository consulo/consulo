package consulo.ide.impl.idea.remoteServer;

import consulo.component.extension.ExtensionPointName;
import consulo.configurable.UnnamedConfigurable;
import consulo.project.Project;
import consulo.ide.impl.idea.remoteServer.configuration.RemoteServersManager;
import consulo.ide.impl.idea.remoteServer.configuration.ServerConfiguration;
import consulo.ide.impl.idea.remoteServer.configuration.deployment.DeploymentConfigurator;
import consulo.ide.impl.idea.remoteServer.configuration.localServer.LocalRunner;
import consulo.ide.impl.idea.remoteServer.runtime.ServerConnector;
import consulo.ide.impl.idea.remoteServer.runtime.ServerTaskExecutor;
import consulo.ide.impl.idea.remoteServer.runtime.deployment.debug.DebugConnector;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public abstract class ServerType<C extends ServerConfiguration> {
  public static final ExtensionPointName<ServerType> EP_NAME = ExtensionPointName.create("consulo.remoteServer.type");
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
