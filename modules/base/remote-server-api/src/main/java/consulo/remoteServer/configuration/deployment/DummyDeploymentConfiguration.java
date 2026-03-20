package consulo.remoteServer.configuration.deployment;

import consulo.component.persist.PersistentStateComponent;
import consulo.execution.RuntimeConfigurationException;
import consulo.remoteServer.configuration.RemoteServer;
import org.jspecify.annotations.Nullable;

/**
 * @author nik
 */
public class DummyDeploymentConfiguration extends DeploymentConfiguration implements PersistentStateComponent<DummyDeploymentConfiguration> {
    @Override
    public PersistentStateComponent<?> getSerializer() {
        return this;
    }

    @Override
    public void checkConfiguration(RemoteServer<?> server, DeploymentSource deploymentSource) throws RuntimeConfigurationException {

    }

    @Override
    public @Nullable DummyDeploymentConfiguration getState() {
        return null;
    }

    @Override
    public void loadState(DummyDeploymentConfiguration state) {
    }
}
