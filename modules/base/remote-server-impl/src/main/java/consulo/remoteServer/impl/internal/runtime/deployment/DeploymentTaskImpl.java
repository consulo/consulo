package consulo.remoteServer.impl.internal.runtime.deployment;

import consulo.execution.runner.ExecutionEnvironment;
import consulo.project.Project;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import consulo.remoteServer.runtime.deployment.debug.DebugConnector;
import org.jspecify.annotations.Nullable;

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

  @Override
  
  public DeploymentSource getSource() {
    return mySource;
  }

  @Override
  
  public D getConfiguration() {
    return myConfiguration;
  }

  @Override
  
  public Project getProject() {
    return myProject;
  }

  @Override
  public boolean isDebugMode() {
    return myDebugConnector != null;
  }

  public @Nullable DebugConnector<?, ?> getDebugConnector() {
    return myDebugConnector;
  }

  
  public ExecutionEnvironment getExecutionEnvironment() {
    return myExecutionEnvironment;
  }
}
