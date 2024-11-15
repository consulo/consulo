/*
 * Copyright 2013-2024 consulo.io
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
package consulo.http.adapter.httpclient4;

import consulo.http.HttpProxyManager;
import consulo.http.internal.IdeHttpClientHelpers;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;

/**
 * FIXME [VISTALL] maybe need make service for it
 *
 * @author VISTALL
 * @since 2024-11-15
 */
public class HttpClient4Proxy {
    /**
     * Install headers for IDE-wide proxy if usage of proxy was enabled in {@link HttpProxyManager}
     *
     * @param builder HttpClient's request builder used to configure new client
     * @see #setProxyForUrlIfEnabled(RequestConfig.Builder, String)
     */
    public static void setProxyIfEnabled(@Nonnull RequestConfig.Builder builder) {
        if (IdeHttpClientHelpers.isHttpProxyEnabled()) {
            builder.setProxy(new HttpHost(IdeHttpClientHelpers.getProxyHost(), IdeHttpClientHelpers.getProxyPort()));
        }
    }

    /**
     * Install credentials for IDE-wide proxy if usage of proxy and proxy authentication were enabled in {@link HttpProxyManager}.
     *
     * @param provider HttpClient's credentials provider used to configure new client
     * @see #setProxyCredentialsForUrlIfEnabled(CredentialsProvider, String)
     */
    public static void setProxyCredentialsIfEnabled(@Nonnull CredentialsProvider provider) {
        if (IdeHttpClientHelpers.isHttpProxyEnabled() && IdeHttpClientHelpers.isProxyAuthenticationEnabled()) {
            final String ntlmUserPassword = IdeHttpClientHelpers.getProxyLogin().replace('\\', '/') + ":" + IdeHttpClientHelpers.getProxyPassword();
            provider.setCredentials(new AuthScope(IdeHttpClientHelpers.getProxyHost(), IdeHttpClientHelpers.getProxyPort(), AuthScope.ANY_REALM, AuthSchemes.NTLM),
                new NTCredentials(ntlmUserPassword));
            provider.setCredentials(new AuthScope(IdeHttpClientHelpers.getProxyHost(), IdeHttpClientHelpers.getProxyPort()),
                new UsernamePasswordCredentials(IdeHttpClientHelpers.getProxyLogin(), IdeHttpClientHelpers.getProxyPassword()));
        }
    }

    /**
     * Install headers for IDE-wide proxy if usage of proxy was enabled AND host of the given url was not added to exclude list
     * in {@link HttpProxyManagerImpl}.
     *
     * @param builder HttpClient's request builder used to configure new client
     * @param url     URL to access (only host part is checked)
     */
    public static void setProxyForUrlIfEnabled(@Nonnull RequestConfig.Builder builder, @Nullable String url) {
        if (IdeHttpClientHelpers.getHttpProxyManager().isHttpProxyEnabledForUrl(url)) {
            setProxyIfEnabled(builder);
        }
    }

    /**
     * Install credentials for IDE-wide proxy if usage of proxy was enabled AND host of the given url was not added to exclude list
     * in {@link HttpProxyManagerImpl}.
     *
     * @param provider HttpClient's credentials provider used to configure new client
     * @param url      URL to access (only host part is checked)
     */
    public static void setProxyCredentialsForUrlIfEnabled(@Nonnull CredentialsProvider provider, @Nullable String url) {
        if (IdeHttpClientHelpers.getHttpProxyManager().isHttpProxyEnabledForUrl(url)) {
            setProxyCredentialsIfEnabled(provider);
        }
    }
}
