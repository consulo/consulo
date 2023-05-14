package consulo.builtinWebServer.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.builtinWebServer.custom.CustomPortServerManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;

@Singleton
@State(name = "BuiltInServerOptions", storages = @Storage("other.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class BuiltInServerOptions implements PersistentStateComponent<BuiltInServerOptions> {
  public static final int DEFAULT_PORT = 63342;

  @Attribute
  public int builtInServerPort = DEFAULT_PORT;
  @Attribute
  public boolean builtInServerAvailableExternally = false;

  @Attribute
  public boolean allowUnsignedRequests = false;

  public static BuiltInServerOptions getInstance() {
    return Application.get().getInstance(BuiltInServerOptions.class);
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