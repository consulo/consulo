/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.github.markusbernhardt.proxy.ProxySearch;
import com.github.markusbernhardt.proxy.selector.misc.BufferedProxySelector;
import com.github.markusbernhardt.proxy.selector.pac.PacProxySelector;
import com.github.markusbernhardt.proxy.selector.pac.UrlPacScriptSource;
import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * @author Irina.Chernushina
 * @since 2013-01-30
 */
public class IdeaWideProxySelector extends ProxySelector {
    private final static Logger LOG = Logger.getInstance(IdeaWideProxySelector.class);

    private final HttpProxyManagerImpl myHttpConfigurable;
    private final AtomicReference<ProxySelector> myPacProxySelector = new AtomicReference<>();

    public IdeaWideProxySelector(HttpProxyManagerImpl configurable) {
        myHttpConfigurable = configurable;
    }

    @Override
    public List<Proxy> select(@Nonnull URI uri) {
        LOG.debug("IDEA-wide proxy selector asked for " + uri.toString());

        String scheme = uri.getScheme();
        if (!("http".equals(scheme) || "https".equals(scheme))) {
            LOG.debug("No proxy: not http/https scheme: " + scheme);
            return CommonProxy.NO_PROXY_LIST;
        }

        HttpProxyManagerState state = myHttpConfigurable.getState();

        if (myHttpConfigurable.isHttpProxyEnabled()) {
            if (isProxyException(uri)) {
                LOG.debug("No proxy: URI '", uri, "' matches proxy exceptions [", state.PROXY_EXCEPTIONS, "]");
                return CommonProxy.NO_PROXY_LIST;
            }

            if (state.PROXY_PORT < 0 || state.PROXY_PORT > 65535) {
                LOG.debug("No proxy: invalid port: " + state.PROXY_PORT);
                return CommonProxy.NO_PROXY_LIST;
            }

            Proxy.Type type = state.PROXY_TYPE_IS_SOCKS ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            Proxy proxy = new Proxy(type, new InetSocketAddress(state.PROXY_HOST, state.PROXY_PORT));
            LOG.debug("Defined proxy: ", proxy);
            state.LAST_ERROR = null;
            return Collections.singletonList(proxy);
        }

        if (myHttpConfigurable.isPacProxyEnabled()) {
            ProxySelector pacProxySelector = myPacProxySelector.get();
            if (myHttpConfigurable.isPacProxyEnabled() && !StringUtil.isEmpty(state.PAC_URL)) {
                myPacProxySelector.set(new PacProxySelector(new UrlPacScriptSource(state.PAC_URL)));
            }
            else if (pacProxySelector == null) {
                ProxySearch proxySearch = ProxySearch.getDefaultProxySearch();
                proxySearch.setPacCacheSettings(
                    32,
                    10 * 60 * 1000,
                    BufferedProxySelector.CacheScope.CACHE_SCOPE_HOST
                ); // Cache 32 urls for up to 10 min.
                pacProxySelector = proxySearch.getProxySelector();
                myPacProxySelector.lazySet(pacProxySelector);
            }

            if (pacProxySelector != null) {
                List<Proxy> select = pacProxySelector.select(uri);
                LOG.debug("Autodetected proxies: ", select);
                return select;
            }
            else {
                LOG.debug("No proxies detected");
            }
        }

        return CommonProxy.NO_PROXY_LIST;
    }

    private boolean isProxyException(URI uri) {
        String uriHost = uri.getHost();
        return isProxyException(uriHost);
    }

    @Contract("null -> false")
    public boolean isProxyException(@Nullable String uriHost) {
        if (StringUtil.isEmptyOrSpaces(uriHost) || StringUtil.isEmptyOrSpaces(myHttpConfigurable.getState().PROXY_EXCEPTIONS)) {
            return false;
        }

        List<String> hosts = StringUtil.split(myHttpConfigurable.getState().PROXY_EXCEPTIONS, ",");
        for (String hostPattern : hosts) {
            String regexpPattern = StringUtil.escapeToRegexp(hostPattern.trim()).replace("\\*", ".*");
            if (Pattern.compile(regexpPattern).matcher(uriHost).matches()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if (myHttpConfigurable.isPacProxyEnabled()) {
            myHttpConfigurable.removeGeneric(new CommonProxy.HostInfo(uri.getScheme(), uri.getHost(), uri.getPort()));
            LOG.debug("generic proxy credentials (if were saved) removed");
            return;
        }

        final InetSocketAddress isa = sa instanceof InetSocketAddress inetSocketAddress ? inetSocketAddress : null;
        if (myHttpConfigurable.isHttpProxyEnabled() && isa != null
            && Comparing.equal(myHttpConfigurable.getProxyHost(), isa.getHostName())) {
            LOG.debug("connection failed message passed to http configurable");
            myHttpConfigurable.getState().LAST_ERROR = ioe.getMessage();
        }
    }
}
