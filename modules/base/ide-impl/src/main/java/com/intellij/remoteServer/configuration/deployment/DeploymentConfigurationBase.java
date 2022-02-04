package com.intellij.remoteServer.configuration.deployment;

import consulo.component.persist.PersistentStateComponent;
import com.intellij.util.xmlb.XmlSerializerUtil;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public class DeploymentConfigurationBase<Self extends DeploymentConfigurationBase> extends DeploymentConfiguration implements PersistentStateComponent<Self> {
  @Override
  public PersistentStateComponent<?> getSerializer() {
    return this;
  }

  @Nullable
  @Override
  public Self getState() {
    return (Self)this;
  }

  @Override
  public void loadState(Self state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
