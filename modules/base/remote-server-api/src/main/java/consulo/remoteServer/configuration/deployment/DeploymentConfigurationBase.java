package consulo.remoteServer.configuration.deployment;

import consulo.component.persist.PersistentStateComponent;
import consulo.execution.RuntimeConfigurationException;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class DeploymentConfigurationBase<Self extends DeploymentConfigurationBase> extends DeploymentConfiguration implements PersistentStateComponent<Self> {
    @Override
    public PersistentStateComponent<?> getSerializer() {
        return this;
    }

    @Override
    public void checkConfiguration(RemoteServer<?> server, DeploymentSource deploymentSource) throws RuntimeConfigurationException {

    }

    @Nullable
    @Override
    public Self getState() {
        return (Self) this;
    }

    @Override
    public void loadState(Self state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
