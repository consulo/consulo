package consulo.remoteServer.configuration;

import consulo.component.persist.PersistentStateComponent;
import consulo.util.xml.serializer.XmlSerializerUtil;

import org.jspecify.annotations.Nullable;

/**
 * @author nik
 */
public abstract class ServerConfigurationBase<Self extends ServerConfigurationBase<Self>> extends ServerConfiguration implements PersistentStateComponent<Self> {
  @Override
  public PersistentStateComponent<?> getSerializer() {
    return this;
  }

  @Override
  public @Nullable Self getState() {
    return (Self)this;
  }

  @Override
  public void loadState(Self state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
