package consulo.remoteServer.configuration.deployment;


import consulo.component.persist.PersistentStateComponent;
import consulo.execution.RuntimeConfigurationException;
import consulo.project.Project;
import consulo.remoteServer.configuration.RemoteServer;

public abstract class DeploymentConfiguration {
    public abstract PersistentStateComponent<?> getSerializer();

    public abstract void checkConfiguration(RemoteServer<?> server, DeploymentSource deploymentSource)
        throws RuntimeConfigurationException;

    public void checkConfiguration(RemoteServer<?> server, DeploymentSource deploymentSource, Project project)
        throws RuntimeConfigurationException {
        checkConfiguration(server, deploymentSource);
    }
}
