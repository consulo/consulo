/*
 * Copyright 2013-2022 consulo.io
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

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.util.lang.Pair;
import consulo.util.lang.SystemProperties;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 05-Jul-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface HttpProxyManager {
    public static final int CONNECTION_TIMEOUT = SystemProperties.getIntProperty("consulo.connection.timeout", 10000);
    public static final int READ_TIMEOUT = SystemProperties.getIntProperty("consulo.read.timeout", 60000);
    public static final int REDIRECT_LIMIT = SystemProperties.getIntProperty("consulo.redirect.limit", 10);

    
    static HttpProxyManager getInstance() {
        return Application.get().getInstance(HttpProxyManager.class);
    }

    /**
     * Opens HTTP connection to a given location using configured http proxy settings.
     *
     * @param location url to connect to
     * @return instance of {@link HttpURLConnection}
     * @throws IOException in case of any I/O troubles or if created connection isn't instance of HttpURLConnection.
     */
    
    @Deprecated
    @DeprecationInfo("Prefer HttpRequests")
    default HttpURLConnection openHttpConnection(String location) throws IOException {
        URLConnection urlConnection = openConnection(location);
        if (urlConnection instanceof HttpURLConnection) {
            return (HttpURLConnection) urlConnection;
        }
        else {
            throw new IOException("Expected " + HttpURLConnection.class + ", but got " + urlConnection.getClass());
        }
    }

    
    @Deprecated
    @DeprecationInfo("Prefer HttpRequests")
    URLConnection openConnection(String location) throws IOException;

    boolean isHttpProxyEnabled();

    boolean isPacProxyEnabled();

    boolean isProxyAuthenticationEnabled();

    boolean isHttpProxyEnabledForUrl(@Nullable String url);

    /**
     * todo [all] It is NOT necessary to call anything if you obey common IDE proxy settings;
     * todo if you want to define your own behaviour, refer to {@link CommonProxy}
     * <p>
     * also, this method is useful in a way that it test connection to the host [through proxy]
     *
     * @param url URL for HTTP connection
     */
    void prepareURL(String url) throws IOException;

    String getProxyHost();

    int getProxyPort();

    String getProxyLogin();

    String getPlainProxyPassword();

    
    List<Pair<String, String>> getJvmProperties(boolean withAutodetection, @Nullable URI uri);

    
    List<String> getProxyExceptions();

    boolean isRealProxy(Proxy proxy);

    
    ProxySelector getOnlyBySettingsSelector();

    @Nullable
    PasswordAuthentication getGenericPassword(String host, int port);
}
