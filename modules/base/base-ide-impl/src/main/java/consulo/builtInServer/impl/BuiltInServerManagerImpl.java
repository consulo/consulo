package consulo.builtInServer.impl;

import com.intellij.idea.StartupUtil;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationNamesInfo;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Url;
import com.intellij.util.UrlImpl;
import com.intellij.util.net.NetUtils;
import consulo.builtInServer.BuiltInServerManager;
import consulo.builtInServer.custom.CustomPortServerManager;
import consulo.builtInServer.impl.net.http.ImportantFolderLockerViaBuiltInServer;
import consulo.start.ImportantFolderLocker;
import io.netty.channel.oio.OioEventLoopGroup;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;
import consulo.builtInServer.impl.ide.BuiltInServerOptions;
import consulo.builtInServer.impl.net.http.BuiltInServer;
import consulo.builtInServer.impl.net.http.SubServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class BuiltInServerManagerImpl extends BuiltInServerManager {
  private static final Logger LOG = Logger.getInstance(BuiltInServerManager.class);

  public static final NotNullLazyValue<NotificationGroup> NOTIFICATION_GROUP = new NotNullLazyValue<NotificationGroup>() {
    @Nonnull
    @Override
    protected NotificationGroup compute() {
      return new NotificationGroup("Built-in Server", NotificationDisplayType.STICKY_BALLOON, true);
    }
  };

  @NonNls
  public static final String PROPERTY_RPC_PORT = "consulo.rpc.port";
  private static final int PORTS_COUNT = 20;

  private final AtomicBoolean started = new AtomicBoolean(false);
  private final Application myApplication;

  @Nullable
  private BuiltInServer server;

  @Inject
  public BuiltInServerManagerImpl(Application application) {
    myApplication = application;
    startServerInPooledThread();
  }

  @Override
  public int getPort() {
    return server == null ? getDefaultPort() : server.getPort();
  }

  @Override
  public BuiltInServerManager waitForStart() {
    Future<?> serverStartFuture = startServerInPooledThread();
    if (serverStartFuture != null) {
      LOG.assertTrue(myApplication.isUnitTestMode() || !myApplication.isDispatchThread());
      try {
        serverStartFuture.get();
      }
      catch (InterruptedException | ExecutionException ignored) {
      }
    }
    return this;
  }

  private int getDefaultPort() {
    if (System.getProperty(PROPERTY_RPC_PORT) == null) {
      // Default port will be occupied by main idea instance - define the custom default to avoid searching of free port
      return myApplication.isUnitTestMode() ? 62252 : 62242;
    }
    else {
      return Integer.parseInt(System.getProperty(PROPERTY_RPC_PORT));
    }
  }

  private Future<?> startServerInPooledThread() {
    if (!started.compareAndSet(false, true)) {
      return null;
    }

    return myApplication.executeOnPooledThread(() -> {
      try {
        ImportantFolderLocker locker = StartupUtil.getLocker();

        BuiltInServer mainServer = locker instanceof ImportantFolderLockerViaBuiltInServer ? ((ImportantFolderLockerViaBuiltInServer)locker).getServer() : null;
        if (mainServer == null || mainServer.getEventLoopGroup() instanceof OioEventLoopGroup) {
          server = BuiltInServer.start(1, getDefaultPort(), PORTS_COUNT, false, null);
        }
        else {
          server = BuiltInServer.start(mainServer.getEventLoopGroup(), false, getDefaultPort(), PORTS_COUNT, true, null);
        }
        bindCustomPorts(server);
      }
      catch (Throwable e) {
        LOG.info(e);
        NOTIFICATION_GROUP.getValue().createNotification("Cannot start internal HTTP server. Git integration, Some plugins may operate with errors. " +
                                                         "Please check your firewall settings and restart " + ApplicationNamesInfo.getInstance().getFullProductName(),
                                                         NotificationType.ERROR).notify(null);
        return;
      }

      LOG.info("built-in server started, port " + server.getPort());

      Disposer.register(myApplication, server);
    });
  }

  @Override
  @Nullable
  public Disposable getServerDisposable() {
    return server;
  }

  @Override
  public boolean isOnBuiltInWebServer(@Nullable Url url) {
    return url != null && !StringUtil.isEmpty(url.getAuthority()) && isOnBuiltInWebServerByAuthority(url.getAuthority());
  }

  @Override
  public Url addAuthToken(@Nonnull Url url) {
    if (url.getParameters() != null) {
      // built-in server url contains query only if token specified
      return url;
    }
    return new UrlImpl(url.getScheme(), url.getAuthority(), url.getPath(), "?" + BuiltInWebServerKt.TOKEN_PARAM_NAME + "=" + BuiltInWebServerKt.acquireToken());
  }

  @Override
  public void configureRequestToWebServer(@Nonnull URLConnection connection) {
    connection.setRequestProperty(BuiltInWebServerKt.TOKEN_HEADER_NAME, BuiltInWebServerKt.acquireToken());
  }

  private void bindCustomPorts(@Nonnull BuiltInServer server) {
    if (myApplication.isUnitTestMode()) {
      return;
    }

    for (CustomPortServerManager customPortServerManager : CustomPortServerManager.EP_NAME.getExtensionList()) {
      try {
        new SubServer(customPortServerManager, server).bind(customPortServerManager.getPort());
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  public static boolean isOnBuiltInWebServerByAuthority(@Nonnull String authority) {
    int portIndex = authority.indexOf(':');
    if (portIndex < 0 || portIndex == authority.length() - 1) {
      return false;
    }

    int port = StringUtil.parseInt(authority.substring(portIndex + 1), -1);
    if (port == -1) {
      return false;
    }

    BuiltInServerOptions options = BuiltInServerOptions.getInstance();
    int idePort = BuiltInServerManager.getInstance().getPort();
    if (options.builtInServerPort != port && idePort != port) {
      return false;
    }

    String host = authority.substring(0, portIndex);
    if (NetUtils.isLocalhost(host)) {
      return true;
    }

    try {
      InetAddress inetAddress = InetAddress.getByName(host);
      return inetAddress.isLoopbackAddress() ||
             inetAddress.isAnyLocalAddress() ||
             (options.builtInServerAvailableExternally && idePort != port && NetworkInterface.getByInetAddress(inetAddress) != null);
    }
    catch (IOException e) {
      return false;
    }
  }
}