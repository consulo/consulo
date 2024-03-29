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
package consulo.http;

import consulo.util.lang.StringUtil;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Mikhail Golubev
 */
public class IdeHttpClientHelpers {
  private IdeHttpClientHelpers() {
  }

  @Nonnull
  private static HttpProxyManager getHttpProxyManager() {
    return HttpProxyManager.getInstance();
  }

  private static boolean isHttpProxyEnabled() {
    return getHttpProxyManager().isHttpProxyEnabled();
  }

  private static boolean isProxyAuthenticationEnabled() {
    return getHttpProxyManager().isProxyAuthenticationEnabled();
  }

  @Nonnull
  private static String getProxyHost() {
    return StringUtil.notNullize(getHttpProxyManager().getProxyHost());
  }

  private static int getProxyPort() {
    return getHttpProxyManager().getProxyPort();
  }

  @Nonnull
  private static String getProxyLogin() {
    return StringUtil.notNullize(getHttpProxyManager().getProxyLogin());
  }

  @Nonnull
  private static String getProxyPassword() {
    return StringUtil.notNullize(getHttpProxyManager().getPlainProxyPassword());
  }

  public static final class ApacheHttpClient4 {

    /**
     * Install headers for IDE-wide proxy if usage of proxy was enabled in {@link HttpProxyManagerImpl}.
     *
     * @param builder HttpClient's request builder used to configure new client
     * @see #setProxyForUrlIfEnabled(RequestConfig.Builder, String)
     */
    public static void setProxyIfEnabled(@Nonnull RequestConfig.Builder builder) {
      if (isHttpProxyEnabled()) {
        builder.setProxy(new HttpHost(getProxyHost(), getProxyPort()));
      }
    }

    /**
     * Install credentials for IDE-wide proxy if usage of proxy and proxy authentication were enabled in {@link HttpProxyManagerImpl}.
     *
     * @param provider HttpClient's credentials provider used to configure new client
     * @see #setProxyCredentialsForUrlIfEnabled(CredentialsProvider, String)
     */
    public static void setProxyCredentialsIfEnabled(@Nonnull CredentialsProvider provider) {
      if (isHttpProxyEnabled() && isProxyAuthenticationEnabled()) {
        final String ntlmUserPassword = getProxyLogin().replace('\\', '/') + ":" + getProxyPassword();
        provider.setCredentials(new AuthScope(getProxyHost(), getProxyPort(), AuthScope.ANY_REALM, AuthSchemes.NTLM),
                                new NTCredentials(ntlmUserPassword));
        provider.setCredentials(new AuthScope(getProxyHost(), getProxyPort()),
                                new UsernamePasswordCredentials(getProxyLogin(), getProxyPassword()));
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
      if (getHttpProxyManager().isHttpProxyEnabledForUrl(url)) {
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
      if (getHttpProxyManager().isHttpProxyEnabledForUrl(url)) {
        setProxyCredentialsIfEnabled(provider);
      }
    }
  }
}
