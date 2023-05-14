package consulo.builtinWebServer.impl;

import com.google.common.net.InetAddresses;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.impl.internal.start.ImportantFolderLocker;
import consulo.application.impl.internal.start.StartupUtil;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.builtinWebServer.custom.CustomPortServerManager;
import consulo.builtinWebServer.impl.http.BuiltInServer;
import consulo.builtinWebServer.impl.http.ImportantFolderLockerViaBuiltInServer;
import consulo.builtinWebServer.impl.http.SubServer;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.util.io.NetUtil;
import consulo.util.io.Url;
import consulo.util.io.Urls;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.ThrowableFunction;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.resolver.HostsFileEntriesResolver;
import io.netty.resolver.ResolvedAddressTypes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@ServiceImpl
public class BuiltInServerManagerImpl extends BuiltInServerManager {
  private static final Logger LOG = Logger.getInstance(BuiltInServerManager.class);

  public static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("Built-in Server", NotificationDisplayType.STICKY_BALLOON, true);

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
        NOTIFICATION_GROUP.createNotification("Cannot start internal HTTP server. Git integration, Some plugins may operate with errors. " +
                                              "Please check your firewall settings and restart " +
                                              Application.get().getName(), NotificationType.ERROR).notify(null);
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
    return Urls.newUrl(url.getScheme(), url.getAuthority(), url.getPath(), "?" + BuiltInWebServerKt.TOKEN_PARAM_NAME + "=" + BuiltInWebServerKt.acquireToken());
  }

  @Override
  public void configureRequestToWebServer(@Nonnull URLConnection connection) {
    connection.setRequestProperty(BuiltInWebServerKt.TOKEN_HEADER_NAME, BuiltInWebServerKt.acquireToken());
  }

  @Override
  public boolean isLocalHost(String host, boolean onlyAnyOrLoopback, boolean hostsOnly) {
    if (NetUtil.isLocalhost(host)) {
      return true;
    }

    // if IP address, it is safe to use getByName (not affected by DNS rebinding)
    if (onlyAnyOrLoopback && !InetAddresses.isInetAddress(host)) {
      return false;
    }

    ThrowableFunction<InetAddress, Boolean, SocketException> isLocal =
            inetAddress -> inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || NetworkInterface.getByInetAddress(inetAddress) != null;

    try {
      InetAddress address = InetAddress.getByName(host);
      if (!isLocal.apply(address)) {
        return false;
      }
      // be aware - on windows hosts file doesn't contain localhost
      // hosts can contain remote addresses, so, we check it
      if (hostsOnly && !InetAddresses.isInetAddress(host)) {
        InetAddress hostInetAddress = HostsFileEntriesResolver.DEFAULT.address(host, ResolvedAddressTypes.IPV4_PREFERRED);
        return hostInetAddress != null && isLocal.apply(hostInetAddress);
      }
      else {
        return true;
      }
    }
    catch (IOException ignored) {
      return false;
    }
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
    if (NetUtil.isLocalhost(host)) {
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