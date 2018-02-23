package com.intellij.remoteServer.configuration.deployment;

import com.intellij.openapi.components.PersistentStateComponent;

/**
 * @author nik
 */
public class DummyDeploymentConfiguration extends DeploymentConfiguration implements PersistentStateComponent<DummyDeploymentConfiguration> {
  @Override
  public PersistentStateComponent<?> getSerializer() {
    return this;
  }

  @javax.annotation.Nullable
  @Override
  public DummyDeploymentConfiguration getState() {
    return null;
  }

  @Override
  public void loadState(DummyDeploymentConfiguration state) {
  }
}
