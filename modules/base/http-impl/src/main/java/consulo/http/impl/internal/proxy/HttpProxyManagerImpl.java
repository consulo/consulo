/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.http.impl.internal.proxy;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.SystemInfo;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.container.boot.ContainerPathManager;
import consulo.credentialStorage.AuthenticationData;
import consulo.credentialStorage.ui.AuthenticationDialog;
import consulo.disposer.Disposable;
import consulo.http.HttpProxyManager;
import consulo.logging.Logger;
import consulo.project.util.WaitForProgressToShow;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.lang.ref.Ref;
import consulo.util.lang.ref.SimpleReference;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.Transient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@State(name = "HttpConfigurable", storages = @Storage("proxy.settings.xml"))
@Singleton
@ServiceImpl
public class HttpProxyManagerImpl implements PersistentStateComponent<HttpProxyManagerState>, Disposable, HttpProxyManager {
  private static final Logger LOG = Logger.getInstance(HttpProxyManagerImpl.class);
  private static final File PROXY_CREDENTIALS_FILE = new File(ContainerPathManager.get().getOptionsPath(), "proxy.settings.pwd");

  private final HttpProxyManagerState myState = new HttpProxyManagerState();
  public volatile transient boolean AUTHENTICATION_CANCELLED;

  private final Map<CommonProxy.HostInfo, ProxyInfo> myGenericPasswords = new HashMap<>();
  private final Set<CommonProxy.HostInfo> myGenericCancelled = new HashSet<>();

  private transient IdeaWideProxySelector mySelector;
  private transient final Object myLock = new Object();

  private transient final PropertiesEncryptionSupport myEncryptionSupport = new PropertiesEncryptionSupport(new SecretKeySpec(
          new byte[]{(byte)0x50, (byte)0x72, (byte)0x6f, (byte)0x78, (byte)0x79, (byte)0x20, (byte)0x43, (byte)0x6f, (byte)0x6e, (byte)0x66, (byte)0x69, (byte)0x67, (byte)0x20, (byte)0x53, (byte)0x65,
                  (byte)0x63}, "AES"));
  private transient final Supplier<Properties> myProxyCredentials = LazyValue.notNull(() -> {
    try {
      return myEncryptionSupport.load(PROXY_CREDENTIALS_FILE);
    }
    catch (FileNotFoundException | NoSuchFileException ignored) {
    }
    catch (Throwable th) {
      LOG.info(th);
    }
    return new Properties();
  });

  @SuppressWarnings("UnusedDeclaration")
  public transient SimpleReference<PasswordAuthentication> myTestAuthRunnable = new SimpleReference<>(null);
  public transient SimpleReference<PasswordAuthentication> myTestGenericAuthRunnable = new SimpleReference<>(null);

  private final AuthenticationDialog myAuthenticationDialog;

  @Inject
  public HttpProxyManagerImpl(AuthenticationDialog authenticationDialog) {
    myAuthenticationDialog = authenticationDialog;
  }

  public static HttpProxyManagerImpl getInstance() {
    return (HttpProxyManagerImpl)ApplicationManager.getApplication().getInstance(HttpProxyManager.class);
  }

  @Override
  public HttpProxyManagerState getState() {
    CommonProxy.isInstalledAssertion();

    if (!myState.KEEP_PROXY_PASSWORD) {
      removeSecure("proxy.password");
    }
    correctPasswords();
    return myState;
  }

  @Override
  public void afterLoadState() {
// all settings are defaults
    // trying user's proxy configuration entered while obtaining the license
    final SharedProxyConfig.ProxyParameters cfg = SharedProxyConfig.load();
    if (cfg != null) {
      SharedProxyConfig.clear();
      if (cfg.host != null) {
        myState.USE_HTTP_PROXY = true;
        myState.PROXY_HOST = cfg.host;
        myState.PROXY_PORT = cfg.port;
        if (cfg.login != null) {
          setPlainProxyPassword(new String(cfg.password));
          storeSecure("proxy.login", cfg.login);
          myState.PROXY_AUTHENTICATION = true;
          myState.KEEP_PROXY_PASSWORD = true;
        }
      }
    }

    mySelector = new IdeaWideProxySelector(this);
    String name = getClass().getName();
    CommonProxy.getInstance().setCustom(name, mySelector);
    CommonProxy.getInstance().setCustomAuth(name, new IdeaWideAuthenticator(this));
  }

  @Nonnull
  public ProxySelector getOnlyBySettingsSelector() {
    return mySelector;
  }

  @Override
  public void dispose() {
    final String name = getClass().getName();
    CommonProxy.getInstance().removeCustom(name);
    CommonProxy.getInstance().removeCustomAuth(name);
  }

  private void correctPasswords() {
    synchronized (myLock) {
      Iterator<ProxyInfo> iterator = myGenericPasswords.values().iterator();
      while (iterator.hasNext()) {
        ProxyInfo next = iterator.next();

        if (!next.isStore()) {
          iterator.remove();
        }
      }
    }
  }

  @Override
  public void loadState(HttpProxyManagerState state) {
    XmlSerializerUtil.copyBean(state, myState);

    if (!myState.KEEP_PROXY_PASSWORD) {
      removeSecure("proxy.password");
    }
    correctPasswords();
  }

  public boolean isGenericPasswordCanceled(@Nonnull String host, int port) {
    synchronized (myLock) {
      return myGenericCancelled.contains(new CommonProxy.HostInfo(null, host, port));
    }
  }

  public void setGenericPasswordCanceled(final String host, final int port) {
    synchronized (myLock) {
      myGenericCancelled.add(new CommonProxy.HostInfo(null, host, port));
    }
  }

  public PasswordAuthentication getGenericPassword(@Nonnull String host, int port) {
    final ProxyInfo proxyInfo;
    synchronized (myLock) {
      proxyInfo = myGenericPasswords.get(new CommonProxy.HostInfo(null, host, port));
    }
    if (proxyInfo == null) {
      return null;
    }
    return new PasswordAuthentication(proxyInfo.getUsername(), decode(String.valueOf(proxyInfo.getPasswordCrypt())).toCharArray());
  }

  public void putGenericPassword(final String host, final int port, @Nonnull PasswordAuthentication authentication, boolean remember) {
    PasswordAuthentication coded = new PasswordAuthentication(authentication.getUserName(), encode(String.valueOf(authentication.getPassword())).toCharArray());
    synchronized (myLock) {
      myGenericPasswords.put(new CommonProxy.HostInfo(null, host, port), new ProxyInfo(remember, coded.getUserName(), String.valueOf(coded.getPassword())));
    }
  }

  @Override
  @Transient
  @Nullable
  public String getProxyLogin() {
    return getSecure("proxy.login");
  }

  @Transient
  public void setProxyLogin(String login) {
    storeSecure("proxy.login", login);
  }

  @Override
  @Transient
  @Nullable
  public String getPlainProxyPassword() {
    return getSecure("proxy.password");
  }

  @Transient
  public void setPlainProxyPassword(String password) {
    storeSecure("proxy.password", password);
  }


  private static String decode(String value) {
    return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
  }

  private static String encode(String password) {
    return Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
  }

  public PasswordAuthentication getGenericPromptedAuthentication(final String prefix, final String host, final String prompt, final int port, final boolean remember) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return myTestGenericAuthRunnable.get();
    }

    final Ref<PasswordAuthentication> value = Ref.create();
    runAboveAll(() -> {
      if (isGenericPasswordCanceled(host, port)) {
        return;
      }

      PasswordAuthentication password = getGenericPassword(host, port);
      if (password != null) {
        value.set(password);
        return;
      }

      AuthenticationData dialog = myAuthenticationDialog.showNoSafe(prefix + host, "Please enter credentials for: " + prompt, "", "", remember);
      if (dialog != null) {
        PasswordAuthentication passwordAuthentication = new PasswordAuthentication(dialog.getLogin(), dialog.getPassword());
        putGenericPassword(host, port, passwordAuthentication, remember && dialog.isRememberPassword());
        value.set(passwordAuthentication);
      }
      else {
        setGenericPasswordCanceled(host, port);
      }
    });
    return value.get();
  }

  public PasswordAuthentication getPromptedAuthentication(final String host, final String prompt) {
    if (AUTHENTICATION_CANCELLED) {
      return null;
    }

    final String password = getPlainProxyPassword();
    if (myState.PROXY_AUTHENTICATION) {
      final String login = getSecure("proxy.login");
      if (!StringUtil.isEmptyOrSpaces(login) && !StringUtil.isEmptyOrSpaces(password)) {
        return new PasswordAuthentication(login, password.toCharArray());
      }
    }

    // do not try to show any dialogs if application is exiting
    if (ApplicationManager.getApplication() == null || ApplicationManager.getApplication().isDisposeInProgress() || ApplicationManager.getApplication().isDisposed()) return null;

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return myTestGenericAuthRunnable.get();
    }
    final PasswordAuthentication[] value = new PasswordAuthentication[1];
    runAboveAll(() -> {
      if (AUTHENTICATION_CANCELLED) {
        return;
      }

      // password might have changed, and the check below is for that
      final String password1 = getPlainProxyPassword();
      if (myState.PROXY_AUTHENTICATION) {
        final String login = getSecure("proxy.login");
        if (!StringUtil.isEmptyOrSpaces(login) && !StringUtil.isEmptyOrSpaces(password1)) {
          value[0] = new PasswordAuthentication(login, password1.toCharArray());
          return;
        }
      }

      AuthenticationData data =
              myAuthenticationDialog.showNoSafe("Proxy authentication: " + host, "Please enter credentials for: " + prompt, getSecure("proxy.login"), "", myState.KEEP_PROXY_PASSWORD);
      if (data != null) {
        myState.PROXY_AUTHENTICATION = true;
        final boolean keepPass = data.isRememberPassword();
        myState.KEEP_PROXY_PASSWORD = keepPass;
        storeSecure("proxy.login", StringUtil.nullize(data.getLogin()));
        if (keepPass) {
          setPlainProxyPassword(String.valueOf(data.getPassword()));
        }
        else {
          removeSecure("proxy.password");
        }
        value[0] = new PasswordAuthentication(data.getLogin(), data.getPassword());
      }
      else {
        AUTHENTICATION_CANCELLED = true;
      }
    });
    return value[0];
  }

  private static void runAboveAll(@Nonnull final Runnable runnable) {
    ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
    if (progressIndicator != null && progressIndicator.isModal()) {
      WaitForProgressToShow.runOrInvokeAndWaitAboveProgress(runnable);
    }
    else {
      Application app = ApplicationManager.getApplication();
      app.invokeAndWait(runnable, app.getAnyModalityState());
    }
  }

  @Override
  public void prepareURL(@Nonnull String url) throws IOException {
    URLConnection connection = openConnection(url);
    try {
      connection.connect();
      connection.getInputStream();
    }
    catch (IOException e) {
      throw e;
    }
    catch (Throwable ignored) {
    }
    finally {
      if (connection instanceof HttpURLConnection) {
        ((HttpURLConnection)connection).disconnect();
      }
    }
  }

  @Override
  @Nonnull
  public URLConnection openConnection(@Nonnull String location) throws IOException {
    final URL url = new URL(location);
    URLConnection urlConnection = null;
    final List<Proxy> proxies = CommonProxy.getInstance().select(url);
    if (ContainerUtil.isEmpty(proxies)) {
      urlConnection = url.openConnection();
    }
    else {
      IOException exception = null;
      for (Proxy proxy : proxies) {
        try {
          urlConnection = url.openConnection(proxy);
        }
        catch (IOException e) {
          // continue iteration
          exception = e;
        }
      }
      if (urlConnection == null && exception != null) {
        throw exception;
      }
    }

    assert urlConnection != null;
    urlConnection.setReadTimeout(READ_TIMEOUT);
    urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
    return urlConnection;
  }

  @Override
  public boolean isHttpProxyEnabled() {
    return myState.USE_HTTP_PROXY;
  }

  @Override
  public boolean isPacProxyEnabled() {
    return myState.USE_PROXY_PAC;
  }

  @Override
  public boolean isProxyAuthenticationEnabled() {
    return myState.PROXY_AUTHENTICATION;
  }

  @Override
  public String getProxyHost() {
    return myState.PROXY_HOST;
  }

  @Override
  public int getProxyPort() {
    return myState.PROXY_PORT;
  }

  @Override
  public boolean isHttpProxyEnabledForUrl(@Nullable String url) {
    if (!myState.USE_HTTP_PROXY) return false;
    URI uri = url != null ? toUri(url) : null;
    return uri == null || !mySelector.isProxyException(uri.getHost());
  }

  public static URI toUri(@Nonnull String uri) {
    int index = uri.indexOf("://");
    if (index < 0) {
      // true URI, like mailto:
      try {
        return new URI(uri);
      }
      catch (URISyntaxException e) {
        LOG.debug(e);
        return null;
      }
    }

    if (SystemInfo.isWindows && uri.startsWith("file://")) {
      int firstSlashIndex = index + "://".length();
      if (uri.charAt(firstSlashIndex) != '/') {
        uri = "file://" + '/' + uri.substring(firstSlashIndex);
      }
    }

    try {
      return new URI(uri);
    }
    catch (URISyntaxException e) {
      LOG.debug("uri is not fully encoded", e);
      // so, uri is not fully encoded (space)
      try {
        int fragmentIndex = uri.lastIndexOf('#');
        String path = uri.substring(index + 1, fragmentIndex > 0 ? fragmentIndex : uri.length());
        String fragment = fragmentIndex > 0 ? uri.substring(fragmentIndex + 1) : null;
        return new URI(uri.substring(0, index), path, fragment);
      }
      catch (URISyntaxException e1) {
        LOG.debug(e1);
        return null;
      }
    }
  }

  /**
   * @deprecated use {@link #getJvmProperties(boolean, URI)} (to be removed in IDEA 2018)
   */
  @SuppressWarnings({"deprecation", "unused"})
  public static List<Pair<String, String>> getJvmPropertiesList(boolean withAutodetection, @Nullable URI uri) {
    List<Pair<String, String>> properties = getInstance().getJvmProperties(withAutodetection, uri);
    return properties.stream().map(p -> Pair.create(p.first, p.second)).collect(Collectors.toList());
  }

  @Override
  @Nonnull
  public List<Pair<String, String>> getJvmProperties(boolean withAutodetection, @Nullable URI uri) {
    if (!myState.USE_HTTP_PROXY && !myState.USE_PROXY_PAC) {
      return Collections.emptyList();
    }

    List<Pair<String, String>> result = new ArrayList<>();
    if (myState.USE_HTTP_PROXY) {
      boolean putCredentials = myState.KEEP_PROXY_PASSWORD && StringUtil.isNotEmpty(getProxyLogin());
      if (myState.PROXY_TYPE_IS_SOCKS) {
        result.add(Pair.pair(JavaProxyProperty.SOCKS_HOST, myState.PROXY_HOST));
        result.add(Pair.pair(JavaProxyProperty.SOCKS_PORT, String.valueOf(myState.PROXY_PORT)));
        if (putCredentials) {
          result.add(Pair.pair(JavaProxyProperty.SOCKS_USERNAME, getProxyLogin()));
          result.add(Pair.pair(JavaProxyProperty.SOCKS_PASSWORD, getPlainProxyPassword()));
        }
      }
      else {
        result.add(Pair.pair(JavaProxyProperty.HTTP_HOST, myState.PROXY_HOST));
        result.add(Pair.pair(JavaProxyProperty.HTTP_PORT, String.valueOf(myState.PROXY_PORT)));
        result.add(Pair.pair(JavaProxyProperty.HTTPS_HOST, myState.PROXY_HOST));
        result.add(Pair.pair(JavaProxyProperty.HTTPS_PORT, String.valueOf(myState.PROXY_PORT)));
        if (putCredentials) {
          result.add(Pair.pair(JavaProxyProperty.HTTP_USERNAME, getProxyLogin()));
          result.add(Pair.pair(JavaProxyProperty.HTTP_PASSWORD, getPlainProxyPassword()));
        }
      }
    }
    else if (myState.USE_PROXY_PAC && withAutodetection && uri != null) {
      List<Proxy> proxies = CommonProxy.getInstance().select(uri);
      // we will just take the first returned proxy, but we have an option to test connection through each of them,
      // for instance, by calling prepareUrl()
      if (proxies != null && !proxies.isEmpty()) {
        for (Proxy proxy : proxies) {
          if (isRealProxy(proxy)) {
            SocketAddress address = proxy.address();
            if (address instanceof InetSocketAddress) {
              InetSocketAddress inetSocketAddress = (InetSocketAddress)address;
              if (Proxy.Type.SOCKS.equals(proxy.type())) {
                result.add(Pair.pair(JavaProxyProperty.SOCKS_HOST, inetSocketAddress.getHostName()));
                result.add(Pair.pair(JavaProxyProperty.SOCKS_PORT, String.valueOf(inetSocketAddress.getPort())));
              }
              else {
                result.add(Pair.pair(JavaProxyProperty.HTTP_HOST, inetSocketAddress.getHostName()));
                result.add(Pair.pair(JavaProxyProperty.HTTP_PORT, String.valueOf(inetSocketAddress.getPort())));
                result.add(Pair.pair(JavaProxyProperty.HTTPS_HOST, inetSocketAddress.getHostName()));
                result.add(Pair.pair(JavaProxyProperty.HTTPS_PORT, String.valueOf(inetSocketAddress.getPort())));
              }
            }
          }
        }
      }
    }
    return result;
  }

  @Nonnull
  @Override
  public List<String> getProxyExceptions() {
    String proxyExceptions = myState.PROXY_EXCEPTIONS;
    if (StringUtil.isEmpty(proxyExceptions)) {
      return List.of();
    }
    return StringUtil.split(proxyExceptions, ",");
  }

  public static boolean isRealProxy(@Nonnull Proxy proxy) {
    return !Proxy.NO_PROXY.equals(proxy) && !Proxy.Type.DIRECT.equals(proxy.type());
  }

  /**
   * @deprecated use {@link ParametersList#addProperty(String, String)} (to be removed in IDEA 2018)
   */
  @SuppressWarnings({"deprecation", "unused"})
  @Nonnull
  public static List<String> convertArguments(@Nonnull final List<Pair<String, String>> list) {
    if (list.isEmpty()) {
      return Collections.emptyList();
    }
    final List<String> result = new ArrayList<>(list.size());
    for (Pair<String, String> value : list) {
      result.add("-D" + value.getFirst() + "=" + value.getSecond());
    }
    return result;
  }

  public void clearGenericPasswords() {
    synchronized (myLock) {
      myGenericPasswords.clear();
      myGenericCancelled.clear();
    }
  }

  public void removeGeneric(@Nonnull CommonProxy.HostInfo info) {
    synchronized (myLock) {
      myGenericPasswords.remove(info);
    }
  }

  public static class ProxyInfo {
    public boolean myStore;
    public String myUsername;
    public String myPasswordCrypt;

    @SuppressWarnings("UnusedDeclaration")
    public ProxyInfo() {
    }

    public ProxyInfo(boolean store, String username, String passwordCrypt) {
      myStore = store;
      myUsername = username;
      myPasswordCrypt = passwordCrypt;
    }

    public boolean isStore() {
      return myStore;
    }

    public void setStore(boolean store) {
      myStore = store;
    }

    public String getUsername() {
      return myUsername;
    }

    public void setUsername(String username) {
      myUsername = username;
    }

    public String getPasswordCrypt() {
      return myPasswordCrypt;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPasswordCrypt(String passwordCrypt) {
      myPasswordCrypt = passwordCrypt;
    }
  }

  private String getSecure(String key) {
    try {
      //return PasswordSafe.getInstance().getPassword(null, HttpProxyManagerImpl.class, key);
      synchronized (myProxyCredentials) {
        final Properties props = myProxyCredentials.get();
        return props.getProperty(key, null);
      }
    }
    catch (Exception e) {
      LOG.info(e);
    }
    return null;
  }

  private void storeSecure(String key, @Nullable String value) {
    if (value == null) {
      removeSecure(key);
      return;
    }

    try {
      //PasswordSafe.getInstance().storePassword(null, HttpProxyManagerImpl.class, key, value);
      synchronized (myProxyCredentials) {
        final Properties props = myProxyCredentials.get();
        props.setProperty(key, value);
        myEncryptionSupport.store(props, "Proxy Credentials", PROXY_CREDENTIALS_FILE);
      }
    }
    catch (Exception e) {
      LOG.info(e);
    }
  }

  private void removeSecure(String key) {
    try {
      //PasswordSafe.getInstance().removePassword(null, HttpProxyManagerImpl.class, key);
      synchronized (myProxyCredentials) {
        final Properties props = myProxyCredentials.get();
        props.remove(key);
        myEncryptionSupport.store(props, "Proxy Credentials", PROXY_CREDENTIALS_FILE);
      }
    }
    catch (Exception e) {
      LOG.info(e);
    }
  }
}
