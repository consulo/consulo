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
package consulo.http.impl.internal.platform;

import consulo.http.internal.HttpExchange;
import consulo.http.internal.HttpRequestOptions;
import consulo.http.HttpStatusException;
import consulo.http.HttpVersion;
import consulo.http.internal.PlatformHttpRequestSender;
import consulo.http.impl.internal.BaseHttpRequestImpl;
import consulo.http.localize.HttpLocalize;
import consulo.platform.Platform;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

class PlatformHttpRequest extends BaseHttpRequestImpl {
    private final Platform myPlatform;
    private final PlatformHttpRequestSender mySender;

    private @Nullable HttpExchange myExchange;
    private @Nullable Map<String, List<String>> myHeaders;

    PlatformHttpRequest(HttpRequestOptions options, Platform platform, PlatformHttpRequestSender sender) {
        super(options);
        myPlatform = platform;
        mySender = sender;
    }

    private HttpExchange getExchange() throws IOException {
        HttpExchange exchange = myExchange;
        if (exchange != null) {
            return exchange;
        }

        exchange = mySender.send(myPlatform, myOptions);
        myExchange = exchange;

        int statusCode = exchange.statusCode();
        if (statusCode == HttpURLConnection.HTTP_MOVED_PERM || statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            throw new IOException(HttpLocalize.errorConnectionFailedRedirects().get());
        }

        if (statusCode != HttpURLConnection.HTTP_NOT_MODIFIED && !myOptions.allowErrorCodes() && (statusCode < 200 || statusCode >= 300)) {
            String message = HttpLocalize.errorConnectionFailedWithHttpCodeN(statusCode).get();
            throw new HttpStatusException(message, statusCode, exchange.url());
        }
        return exchange;
    }

    @Override
    public String getURL() {
        HttpExchange exchange = myExchange;
        return exchange != null ? exchange.url() : myOptions.url();
    }

    @Override
    public int statusCode() throws IOException {
        return getExchange().statusCode();
    }

    @Override
    public @Nullable String statusMessage() throws IOException {
        return getExchange().statusMessage();
    }

    @Override
    public HttpVersion version() {
        HttpExchange exchange = myExchange;
        return exchange != null ? exchange.version() : HttpVersion.HTTP_1_1;
    }

    @Override
    public Map<String, List<String>> responseHeaders() throws IOException {
        Map<String, List<String>> headers = myHeaders;
        if (headers == null) {
            headers = Maps.newHashMap(HashingStrategy.caseInsensitive());
            headers.putAll(getExchange().headers());
            myHeaders = headers;
        }
        return headers;
    }

    @Override
    public @Nullable String getContentEncoding() throws IOException {
        return headerValue("Content-Encoding");
    }

    @Override
    public @Nullable String getContentType() throws IOException {
        return headerValue("Content-Type");
    }

    @Override
    protected InputStream openInputStream() throws IOException {
        return getExchange().body();
    }

    @Override
    protected int getContentLength() throws IOException {
        return getExchange().contentLength();
    }

    @Override
    protected void disconnect() {
        HttpExchange exchange = myExchange;
        if (exchange != null) {
            exchange.close();
        }
    }
}
