package consulo.ide.impl.builtInServer.impl.ide;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.ServiceManager;
import consulo.ide.impl.builtInServer.BuiltInServerManager;
import consulo.ide.impl.builtInServer.custom.CustomPortServerManager;
import consulo.ide.impl.idea.openapi.util.Getter;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;

@Singleton
@State(name = "BuiltInServerOptions", storages = @Storage("other.xml"))
@Service(ComponentScope.APPLICATION)
@ServiceImpl
public class BuiltInServerOptions implements PersistentStateComponent<BuiltInServerOptions>, Getter<BuiltInServerOptions> {
  public static final int DEFAULT_PORT = 63342;

  @Attribute
  public int builtInServerPort = DEFAULT_PORT;
  @Attribute
  public boolean builtInServerAvailableExternally = false;

  @Attribute
  public boolean allowUnsignedRequests = false;

  public static BuiltInServerOptions getInstance() {
    return ServiceManager.getService(BuiltInServerOptions.class);
  }

  @Override
  public BuiltInServerOptions get() {
    return this;
  }

  @Nullable
  @Override
  public BuiltInServerOptions getState() {
    return this;
  }

  @Override
  public void loadState(BuiltInServerOptions state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public int getEffectiveBuiltInServerPort() {
    DefaultCustomPortServerManager portServerManager = CustomPortServerManager.EP_NAME.findExtension(DefaultCustomPortServerManager.class);
    if (!portServerManager.isBound()) {
      return BuiltInServerManager.getInstance().getPort();
    }
    return builtInServerPort;
  }

  public static void onBuiltInServerPortChanged() {
    CustomPortServerManager.EP_NAME.findExtension(DefaultCustomPortServerManager.class).portChanged();
  }
}