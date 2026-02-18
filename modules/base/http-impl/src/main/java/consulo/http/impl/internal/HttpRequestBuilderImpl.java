/*
 * Copyright 2013-2026 consulo.io
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
package consulo.http.impl.internal;

import consulo.application.Application;
import consulo.http.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.net.ssl.HostnameVerifier;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-02-18
 */
class HttpRequestBuilderImpl implements HttpRequestBuilder {
    @Nonnull
    private final Application myApplication;

    final String myUrl;
    final HttpMethod myHttpMethod;
    int myConnectTimeout = HttpProxyManager.CONNECTION_TIMEOUT;
    int myTimeout = HttpProxyManager.READ_TIMEOUT;
    int myRedirectLimit = HttpProxyManager.REDIRECT_LIMIT;
    boolean myGzip = true;
    boolean myForceHttps;
    boolean myUseProxy = true;
    HostnameVerifier myHostnameVerifier;
    Map<String, String> myHeaders = new LinkedHashMap<>();
    HttpVersion myHttpVersion;
    byte[] myBody;

    HttpRequestBuilderImpl(@Nonnull Application application, @Nonnull String url, HttpMethod httpMethod) {
        myApplication = application;
        myUrl = url;
        myHttpMethod = httpMethod;
    }

    @Override
    public HttpRequestBuilder body(@Nullable byte[] bytes) {
        myBody = bytes;
        return this;
    }

    @Override
    public HttpRequestBuilder version(@Nonnull HttpVersion version) {
        myHttpVersion = version;
        return this;
    }

    @Override
    public HttpRequestBuilder connectTimeout(int value) {
        myConnectTimeout = value;
        return this;
    }

    @Override
    public HttpRequestBuilder readTimeout(int value) {
        myTimeout = value;
        return this;
    }

    @Override
    public HttpRequestBuilder redirectLimit(int redirectLimit) {
        myRedirectLimit = redirectLimit;
        return this;
    }

    @Override
    public HttpRequestBuilder gzip(boolean value) {
        myGzip = value;
        return this;
    }

    @Override
    public HttpRequestBuilder forceHttps(boolean forceHttps) {
        myForceHttps = forceHttps;
        return this;
    }

    @Override
    public HttpRequestBuilder useProxy(boolean useProxy) {
        myUseProxy = useProxy;
        return this;
    }

    @Override
    public HttpRequestBuilder hostNameVerifier(@Nullable HostnameVerifier hostnameVerifier) {
        myHostnameVerifier = hostnameVerifier;
        return this;
    }

    @Override
    public HttpRequestBuilder productNameAsUserAgent() {
        return userAgent(myApplication.getName().get() + "/" + myApplication.getBuildNumber().asString());
    }

    @Override
    public HttpRequestBuilder header(@Nonnull String headerName, @Nullable String headerValue) {
        if (headerValue != null) {
            myHeaders.remove(headerName);
        } else {
            myHeaders.put(headerName, headerValue);
        }
        return this;
    }

    @Override
    public int tryConnect() throws IOException {
        return connect((request) -> {
            URLConnection connection = ((HttpRequestImpl) request).getConnection();
            
            return connection instanceof HttpURLConnection ? ((HttpURLConnection) connection).getResponseCode() : -1;
        });
    }

    @Override
    public <T> T connect(@Nonnull HttpRequestProcessor<T> processor) throws IOException {
        return HttpRequestBuilderFactoryImpl.process(this, processor);
    }
}
