/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.NetUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CommonProxy extends ProxySelector {
    private static final Logger LOG = Logger.getInstance(CommonProxy.class);

    private final static CommonProxy ourInstance = new CommonProxy();
    private final CommonAuthenticator myAuthenticator = new CommonAuthenticator();

    private final static ThreadLocal<Boolean> ourReenterDefence = new ThreadLocal<>();

    public final static List<Proxy> NO_PROXY_LIST = Collections.singletonList(Proxy.NO_PROXY);
    private final static long ourErrorInterval = TimeUnit.MINUTES.toMillis(3);
    private static volatile int ourNotificationCount;
    private volatile static long ourErrorTime;
    private volatile static ProxySelector ourWrong;
    private static final AtomicReference<Map<String, String>> ourProps = new AtomicReference<>();

    static {
        ProxySelector.setDefault(ourInstance);
    }

    private final Object myLock = new Object();
    private final Set<Pair<HostInfo, Thread>> myNoProxy = new HashSet<>();

    private final Map<String, ProxySelector> myCustom = new HashMap<>();
    private final Map<String, NonStaticAuthenticator> myCustomAuth = new HashMap<>();

    public static CommonProxy getInstance() {
        return ourInstance;
    }

    private CommonProxy() {
        ensureAuthenticator();
    }

    public static void isInstalledAssertion() {
        ProxySelector aDefault = ProxySelector.getDefault();
        if (ourInstance != aDefault) {
            // to report only once
            if (ourWrong != aDefault || itsTime()) {
                LOG.error(
                    "ProxySelector.setDefault() was changed to [" + aDefault.toString() +
                        "] - other than consulo.http.impl.internal.proxy.CommonProxy.ourInstance.\n" +
                        "This will make some " + Application.get().getName() + " network calls fail.\n" +
                        "Instead, methods of consulo.http.impl.internal.proxy.CommonProxy should be used for proxying."
                );
                ourWrong = aDefault;
            }
            ProxySelector.setDefault(ourInstance);
            ourInstance.ensureAuthenticator();
        }
        assertSystemPropertiesSet();
    }

    private static boolean itsTime() {
        boolean b = System.currentTimeMillis() - ourErrorTime > ourErrorInterval && ourNotificationCount < 5;
        if (b) {
            ourErrorTime = System.currentTimeMillis();
            ++ourNotificationCount;
        }
        return b;
    }

    private static void assertSystemPropertiesSet() {
        Map<String, String> props = getOldStyleProperties();

        Map<String, String> was = ourProps.get();
        if (Comparing.equal(was, props) && !itsTime()) {
            return;
        }
        ourProps.set(props);

        String message = getMessageFromProps(props);
        if (message != null) {
            // we only intend to somehow report possible misconfiguration
            // will not show to the user since on Mac OS this setting is typical
            LOG.info(message);
        }
    }

    @Nullable
    public static String getMessageFromProps(Map<String, String> props) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            if (!StringUtil.isEmptyOrSpaces(entry.getValue())) {
                return CommonLocalize.labelOldWayJvmPropertyUsed(entry.getKey(), entry.getValue()).get();
            }
        }
        return null;
    }

    public static Map<String, String> getOldStyleProperties() {
        Map<String, String> props = new HashMap<>();
        props.put(JavaProxyProperty.HTTP_HOST, System.getProperty(JavaProxyProperty.HTTP_HOST));
        props.put(JavaProxyProperty.HTTPS_HOST, System.getProperty(JavaProxyProperty.HTTPS_HOST));
        props.put(JavaProxyProperty.SOCKS_HOST, System.getProperty(JavaProxyProperty.SOCKS_HOST));
        return props;
    }

    public void ensureAuthenticator() {
        Authenticator.setDefault(myAuthenticator);
    }

    public void noProxy(@Nonnull String protocol, @Nonnull String host, int port) {
        synchronized (myLock) {
            LOG.debug("no proxy added: " + protocol + "://" + host + ":" + port);
            myNoProxy.add(Pair.create(new HostInfo(protocol, host, port), Thread.currentThread()));
        }
    }

    public void removeNoProxy(@Nonnull String protocol, @Nonnull String host, int port) {
        synchronized (myLock) {
            LOG.debug("no proxy removed: " + protocol + "://" + host + ":" + port);
            myNoProxy.remove(Pair.create(new HostInfo(protocol, host, port), Thread.currentThread()));
        }
    }

    public void noAuthentication(@Nonnull String protocol, @Nonnull String host, int port) {
        synchronized (myLock) {
            LOG.debug("no proxy added: " + protocol + "://" + host + ":" + port);
            myNoProxy.add(Pair.create(new HostInfo(protocol, host, port), Thread.currentThread()));
        }
    }

    @SuppressWarnings("unused")
    public void removeNoAuthentication(@Nonnull String protocol, @Nonnull String host, int port) {
        synchronized (myLock) {
            LOG.debug("no proxy removed: " + protocol + "://" + host + ":" + port);
            myNoProxy.remove(Pair.create(new HostInfo(protocol, host, port), Thread.currentThread()));
        }
    }

    public void setCustom(@Nonnull String key, @Nonnull ProxySelector proxySelector) {
        synchronized (myLock) {
            LOG.debug("custom set: " + key + ", " + proxySelector.toString());
            myCustom.put(key, proxySelector);
        }
    }

    public void setCustomAuth(@Nonnull String key, NonStaticAuthenticator authenticator) {
        synchronized (myLock) {
            LOG.debug("custom auth set: " + key + ", " + authenticator.toString());
            myCustomAuth.put(key, authenticator);
        }
    }

    public void removeCustomAuth(@Nonnull String key) {
        synchronized (myLock) {
            LOG.debug("custom auth removed: " + key);
            myCustomAuth.remove(key);
        }
    }

    public void removeCustom(@Nonnull String key) {
        synchronized (myLock) {
            LOG.debug("custom set: " + key);
            myCustom.remove(key);
        }
    }

    public List<Proxy> select(@Nonnull URL url) {
        return select(createUri(url));
    }

    @Override
    public List<Proxy> select(@Nullable URI uri) {
        isInstalledAssertion();
        if (uri == null) {
            return NO_PROXY_LIST;
        }
        LOG.debug("CommonProxy.select called for " + uri.toString());

        if (Boolean.TRUE.equals(ourReenterDefence.get())) {
            return NO_PROXY_LIST;
        }
        try {
            ourReenterDefence.set(Boolean.TRUE);
            String host = StringUtil.notNullize(uri.getHost());
            if (NetUtil.isLocalhost(host)) {
                return NO_PROXY_LIST;
            }

            HostInfo info = new HostInfo(uri.getScheme(), host, correctPortByProtocol(uri));
            Map<String, ProxySelector> copy;
            synchronized (myLock) {
                if (myNoProxy.contains(Pair.create(info, Thread.currentThread()))) {
                    LOG.debug("CommonProxy.select returns no proxy (in no proxy list) for " + uri.toString());
                    return NO_PROXY_LIST;
                }
                copy = new HashMap<>(myCustom);
            }
            for (Map.Entry<String, ProxySelector> entry : copy.entrySet()) {
                List<Proxy> proxies = entry.getValue().select(uri);
                if (!ContainerUtil.isEmpty(proxies)) {
                    LOG.debug("CommonProxy.select returns custom proxy for " + uri.toString() + ", " + proxies.toString());
                    return proxies;
                }
            }
            return NO_PROXY_LIST;
        }
        finally {
            ourReenterDefence.remove();
        }
    }

    private static int correctPortByProtocol(@Nonnull URI uri) {
        if (uri.getPort() == -1) {
            if ("http".equals(uri.getScheme())) {
                return ProtocolDefaultPorts.HTTP;
            }
            else if ("https".equals(uri.getScheme())) {
                return ProtocolDefaultPorts.SSL;
            }
        }
        return uri.getPort();
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        LOG.info("connect failed to " + uri.toString() + ", sa: " + sa.toString(), ioe);

        Map<String, ProxySelector> copy;
        synchronized (myLock) {
            copy = new HashMap<>(myCustom);
        }
        for (Map.Entry<String, ProxySelector> entry : copy.entrySet()) {
            entry.getValue().connectFailed(uri, sa, ioe);
        }
    }

    private class CommonAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            String siteStr = getRequestingSite() == null ? null : getRequestingSite().toString();
            LOG.debug("CommonAuthenticator.getPasswordAuthentication called for " + siteStr);
            String host = getHostNameReliably(getRequestingHost(), getRequestingSite(), getRequestingURL());
            int port = getRequestingPort();

            Map<String, NonStaticAuthenticator> copy;
            synchronized (myLock) {
                // for hosts defined as no proxy we will NOT pass authentication to not provoke credentials
                HostInfo hostInfo = new HostInfo(getRequestingProtocol(), host, port);
                Pair<HostInfo, Thread> pair = Pair.create(hostInfo, Thread.currentThread());
                if (myNoProxy.contains(pair)) {
                    LOG.debug("CommonAuthenticator.getPasswordAuthentication found host in no proxies set (" + siteStr + ")");
                    return null;
                }
                copy = new HashMap<>(myCustomAuth);
            }

            if (!copy.isEmpty()) {
                for (Map.Entry<String, NonStaticAuthenticator> entry : copy.entrySet()) {
                    NonStaticAuthenticator authenticator = entry.getValue();
                    prepareAuthenticator(authenticator);
                    PasswordAuthentication authentication = authenticator.getPasswordAuthentication();
                    if (authentication != null) {
                        LOG.debug(
                            "CommonAuthenticator.getPasswordAuthentication found custom authenticator for " +
                                siteStr + ", " + entry.getKey() + ", " + authenticator
                        );
                        logAuthentication(authentication);
                        return authentication;
                    }
                }
            }
            return null;
        }

        private void prepareAuthenticator(NonStaticAuthenticator authenticator) {
            authenticator.setRequestingHost(getRequestingHost());
            authenticator.setRequestingSite(getRequestingSite());
            authenticator.setRequestingPort(getRequestingPort());
            authenticator.setRequestingProtocol(getRequestingProtocol());//http
            authenticator.setRequestingPrompt(getRequestingPrompt());
            authenticator.setRequestingScheme(getRequestingScheme());//ntlm
            authenticator.setRequestingURL(getRequestingURL());
            authenticator.setRequestorType(getRequestorType());
        }

        private void logAuthentication(PasswordAuthentication authentication) {
            if (authentication == null) {
                LOG.debug("CommonAuthenticator.getPasswordAuthentication returned null");
            }
            else {
                LOG.debug(
                    "CommonAuthenticator.getPasswordAuthentication returned authentication pair with login: " +
                        authentication.getUserName()
                );
            }
        }
    }

    public static String getHostNameReliably(String requestingHost, InetAddress site, URL requestingUrl) {
        String host = requestingHost;
        if (host == null) {
            if (site != null) {
                host = site.getHostName();
            }
            else if (requestingUrl != null) {
                host = requestingUrl.getHost();
            }
        }
        host = host == null ? "" : host;
        return host;
    }

    private static URI createUri(URL url) {
        return HttpProxyManagerImpl.toUri(url.toString());
    }

    public static class HostInfo {
        public final String myProtocol;
        public final String myHost;
        public final int myPort;

        public HostInfo(@Nullable String protocol, @Nonnull String host, int port) {
            myPort = port;
            myHost = host;
            myProtocol = protocol;
        }

        public String getProtocol() {
            return myProtocol;
        }

        public String getHost() {
            return myHost;
        }

        public int getPort() {
            return myPort;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            HostInfo info = (HostInfo)o;
            return myPort == info.myPort && myHost.equals(info.myHost) && Comparing.equal(myProtocol, info.myProtocol);
        }

        @Override
        public int hashCode() {
            int result = myProtocol != null ? myProtocol.hashCode() : 0;
            result = 31 * result + myHost.hashCode();
            result = 31 * result + myPort;
            return result;
        }
    }
}
