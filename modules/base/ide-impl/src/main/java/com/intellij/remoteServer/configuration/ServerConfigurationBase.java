package com.intellij.remoteServer.configuration;

import consulo.component.persist.PersistentStateComponent;
import consulo.util.xml.serializer.XmlSerializerUtil;

/**
 * @author nik
 */
public abstract class ServerConfigurationBase<Self extends ServerConfigurationBase<Self>> extends ServerConfiguration implements PersistentStateComponent<Self> {
  @Override
  public PersistentStateComponent<?> getSerializer() {
    return this;
  }

  @javax.annotation.Nullable
  @Override
  public Self getState() {
    return (Self)this;
  }

  @Override
  public void loadState(Self state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
