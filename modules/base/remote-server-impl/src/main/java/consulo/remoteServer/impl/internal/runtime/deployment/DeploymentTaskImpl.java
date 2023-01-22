package consulo.remoteServer.impl.internal.runtime.deployment;

import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import consulo.remoteServer.runtime.deployment.debug.DebugConnector;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class DeploymentTaskImpl<D extends DeploymentConfiguration> implements DeploymentTask<D> {
  private final DeploymentSource mySource;
  private final D myConfiguration;
  private final Project myProject;
  private final DebugConnector<?,?> myDebugConnector;
  private final ExecutionEnvironment myExecutionEnvironment;

  public DeploymentTaskImpl(DeploymentSource source, D configuration, Project project, DebugConnector<?, ?> connector,
                            ExecutionEnvironment environment) {
    mySource = source;
    myConfiguration = configuration;
    myProject = project;
    myDebugConnector = connector;
    myExecutionEnvironment = environment;
  }

  @Nonnull
  public DeploymentSource getSource() {
    return mySource;
  }

  @Nonnull
  public D getConfiguration() {
    return myConfiguration;
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  public boolean isDebugMode() {
    return myDebugConnector != null;
  }

  @javax.annotation.Nullable
  public DebugConnector<?, ?> getDebugConnector() {
    return myDebugConnector;
  }

  @Nonnull
  public ExecutionEnvironment getExecutionEnvironment() {
    return myExecutionEnvironment;
  }
}
