package org.jetbrains.builtInWebServer;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SimpleConfigurable;
import com.intellij.openapi.util.Getter;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.xdebugger.settings.DebuggerSettingsCategory;
import com.intellij.xdebugger.settings.XDebuggerSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;
import org.jetbrains.ide.BuiltInServerManagerImpl;
import org.jetbrains.ide.CustomPortServerManager;
import org.jetbrains.io.CustomPortServerManagerBase;

import java.util.Collection;
import java.util.Collections;

@State(name = "BuiltInServerOptions", storages = @Storage("other.xml"))
public class BuiltInServerOptions implements PersistentStateComponent<BuiltInServerOptions>, Getter<BuiltInServerOptions> {
  private static final int DEFAULT_PORT = 63342;

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

  static final class BuiltInServerDebuggerConfigurableProvider extends XDebuggerSettings<Element> {
    public BuiltInServerDebuggerConfigurableProvider() {
      super("buildin-server");
    }

    @NotNull
    @Override
    public Collection<? extends Configurable> createConfigurables(@NotNull DebuggerSettingsCategory category) {
      if (category == DebuggerSettingsCategory.GENERAL) {
        SimpleConfigurable<BuiltInServerConfigurableUi, BuiltInServerOptions> configurable =
                SimpleConfigurable.create("builtInServer", "Built-in server", BuiltInServerConfigurableUi.class, BuiltInServerOptions.getInstance());
        return Collections.singletonList(configurable);
      }
      return Collections.emptyList();
    }

    @Override
    public Element getState() {
      return null;
    }

    @Override
    public void loadState(Element state) {

    }
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
    MyCustomPortServerManager portServerManager = CustomPortServerManager.EP_NAME.findExtension(MyCustomPortServerManager.class);
    if (!portServerManager.isBound()) {
      return BuiltInServerManager.getInstance().getPort();
    }
    return builtInServerPort;
  }

  public static final class MyCustomPortServerManager extends CustomPortServerManagerBase {
    @Override
    public void cannotBind(Exception e, int port) {
      BuiltInServerManagerImpl.NOTIFICATION_GROUP.getValue().createNotification("Cannot start built-in HTTP server on custom port " +
                                                                                port +
                                                                                ". " +
                                                                                "Please ensure that port is free (or check your firewall settings) and restart " +
                                                                                ApplicationNamesInfo.getInstance().getFullProductName(), NotificationType.ERROR)
              .notify(null);
    }

    @Override
    public int getPort() {
      int port = getInstance().builtInServerPort;
      return port == DEFAULT_PORT ? -1 : port;
    }

    @Override
    public boolean isAvailableExternally() {
      return getInstance().builtInServerAvailableExternally;
    }
  }

  public static void onBuiltInServerPortChanged() {
    CustomPortServerManager.EP_NAME.findExtension(MyCustomPortServerManager.class).portChanged();
  }
}