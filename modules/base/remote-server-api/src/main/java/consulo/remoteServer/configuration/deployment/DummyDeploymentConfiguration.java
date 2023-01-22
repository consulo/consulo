package consulo.remoteServer.configuration.deployment;

import consulo.component.persist.PersistentStateComponent;

import javax.annotation.Nullable;

/**
 * @author nik
 */
public class DummyDeploymentConfiguration extends DeploymentConfiguration implements PersistentStateComponent<DummyDeploymentConfiguration> {
  @Override
  public PersistentStateComponent<?> getSerializer() {
    return this;
  }

  @Nullable
  @Override
  public DummyDeploymentConfiguration getState() {
    return null;
  }

  @Override
  public void loadState(DummyDeploymentConfiguration state) {
  }
}
