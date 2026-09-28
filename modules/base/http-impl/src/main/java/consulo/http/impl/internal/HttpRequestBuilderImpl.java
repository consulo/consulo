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
import consulo.application.ApplicationManager;
import consulo.http.*;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.HostnameVerifier;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-02-18
 */
class HttpRequestBuilderImpl implements HttpRequestBuilder {
    private static final Logger LOG = Logger.getInstance(HttpRequestBuilderImpl.class);

    private final Application myApplication;
    private final HttpRequestExecutor myExecutor;

    private final String myUrl;
    private final HttpMethod myHttpMethod;
    private int myConnectTimeout = HttpProxyManager.CONNECTION_TIMEOUT;
    private int myTimeout = HttpProxyManager.READ_TIMEOUT;
    private int myRedirectLimit = HttpProxyManager.REDIRECT_LIMIT;
    private boolean myGzip = true;
    private boolean myForceHttps;
    private boolean myUseProxy = true;
    private @Nullable HostnameVerifier myHostnameVerifier;
    private final Map<String, String> myHeaders = new LinkedHashMap<>();
    private @Nullable HttpVersion myHttpVersion;
    private byte @Nullable [] myBody;
    private boolean myAllowErrorCodes;

    HttpRequestBuilderImpl(Application application, HttpRequestExecutor executor, String url, HttpMethod httpMethod) {
        myApplication = application;
        myExecutor = executor;
        myUrl = url;
        myHttpMethod = httpMethod;
    }

    @Override
    public HttpRequestBuilder allowErrorCodes(boolean allowErrorCodes) {
        myAllowErrorCodes = allowErrorCodes;
        return this;
    }

    @Override
    public HttpRequestBuilder body(@Nullable byte[] bytes) {
        myBody = bytes;
        return this;
    }

    @Override
    public HttpRequestBuilder version(HttpVersion version) {
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
    public HttpRequestBuilder header(String headerName, @Nullable String headerValue) {
        if (headerValue == null) {
            myHeaders.remove(headerName);
        } else {
            myHeaders.put(headerName, headerValue);
        }
        return this;
    }

    @Override
    public int tryConnect() throws IOException {
        return connect((request) -> {
            int statusCode = request.statusCode();
            // zero is not a http connection
            return statusCode == 0 ? -1 : statusCode;
        });
    }

    @Override
    public <T> T connect(HttpRequestProcessor<T> processor) throws IOException {
        LOG.assertTrue(ApplicationManager.getApplication() == null || !ApplicationManager.getApplication().isReadAccessAllowed(), "Network shouldn't be accessed in EDT or inside read action");

        HttpRequestOptions options = new HttpRequestOptions(
            myUrl,
            myHttpMethod,
            new LinkedHashMap<>(myHeaders),
            myBody,
            myHttpVersion,
            myConnectTimeout,
            myTimeout,
            myRedirectLimit,
            myGzip,
            myForceHttps,
            myUseProxy,
            myHostnameVerifier,
            myAllowErrorCodes
        );
        return myExecutor.execute(options, processor);
    }
}
