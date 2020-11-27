package consulo.builtInServer.impl.ide;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.Getter;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.xdebugger.settings.DebuggerSettingsCategory;
import com.intellij.xdebugger.settings.XDebuggerSettings;
import consulo.builtInServer.BuiltInServerManager;
import consulo.builtInServer.custom.CustomPortServerManager;
import consulo.builtInServer.custom.CustomPortServerManagerBase;
import consulo.builtInServer.impl.BuiltInServerManagerImpl;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

@Singleton
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

    @Nonnull
    @Override
    public Collection<? extends Configurable> createConfigurables(@Nonnull DebuggerSettingsCategory category) {
      if (category == DebuggerSettingsCategory.GENERAL) {
        return List.of(new BuiltInServerConfigurable());
      }
      return List.of();
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