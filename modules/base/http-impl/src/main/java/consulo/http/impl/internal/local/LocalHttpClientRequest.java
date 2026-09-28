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
package consulo.http.impl.internal.local;

import consulo.http.HttpVersion;
import consulo.http.impl.internal.BaseHttpRequestImpl;
import consulo.http.impl.internal.HttpRequestOptions;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.io.StreamUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * An answer read through {@link HttpClient} - the request is sent when the answer is first asked for.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
class LocalHttpClientRequest extends BaseHttpRequestImpl {
    private final LocalHttpClientExecutor myExecutor;

    private LocalHttpClientExecutor.@Nullable Exchange myExchange;

    LocalHttpClientRequest(LocalHttpClientExecutor executor, HttpRequestOptions options) {
        super(options);
        myExecutor = executor;
    }

    private HttpResponse<InputStream> getResponse() throws IOException {
        if (myExchange == null) {
            myExchange = myExecutor.send(myOptions);
        }
        return myExchange.response();
    }

    @Override
    public String getURL() {
        LocalHttpClientExecutor.Exchange exchange = myExchange;
        return exchange != null ? exchange.url() : myOptions.url();
    }

    @Override
    public int statusCode() throws IOException {
        return getResponse().statusCode();
    }

    /**
     * {@link HttpClient} gives no reason phrase - HTTP/2 has none - so it is the standard one of the status.
     */
    @Override
    public @Nullable String statusMessage() throws IOException {
        return reasonPhrase(statusCode());
    }

    /**
     * The version of the answer - the asked one until the request is sent, since this method can not send it.
     */
    @Override
    public HttpVersion version() {
        LocalHttpClientExecutor.Exchange exchange = myExchange;
        if (exchange == null) {
            HttpVersion version = myOptions.version();
            return version == null ? HttpVersion.HTTP_1_1 : version;
        }

        HttpClient.Version version = exchange.response().version();
        return switch (version.name()) {
            case "HTTP_2" -> HttpVersion.HTTP_2;
            case "HTTP_3" -> HttpVersion.HTTP_3;
            default -> HttpVersion.HTTP_1_1;
        };
    }

    @Override
    public Map<String, List<String>> responseHeaders() throws IOException {
        Map<String, List<String>> headers = Maps.newHashMap(HashingStrategy.caseInsensitive());
        for (Map.Entry<String, List<String>> entry : getResponse().headers().map().entrySet()) {
            headers.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return headers;
    }

    @Override
    public @Nullable String getContentEncoding() throws IOException {
        return getResponse().headers().firstValue("Content-Encoding").orElse(null);
    }

    @Override
    public @Nullable String getContentType() throws IOException {
        return getResponse().headers().firstValue("Content-Type").orElse(null);
    }

    @Override
    protected InputStream openInputStream() throws IOException {
        return getResponse().body();
    }

    @Override
    protected int getContentLength() throws IOException {
        OptionalLong length = getResponse().headers().firstValueAsLong("Content-Length");
        return length.isPresent() && length.getAsLong() <= Integer.MAX_VALUE ? (int) length.getAsLong() : -1;
    }

    @Override
    protected void disconnect() {
        LocalHttpClientExecutor.Exchange exchange = myExchange;
        if (exchange != null) {
            StreamUtil.closeStream(exchange.response().body());
        }
    }

    static @Nullable String reasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 100 -> "Continue";
            case 101 -> "Switching Protocols";
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 203 -> "Non-Authoritative Information";
            case 204 -> "No Content";
            case 205 -> "Reset Content";
            case 206 -> "Partial Content";
            case 300 -> "Multiple Choices";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 303 -> "See Other";
            case 304 -> "Not Modified";
            case 307 -> "Temporary Redirect";
            case 308 -> "Permanent Redirect";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 402 -> "Payment Required";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 406 -> "Not Acceptable";
            case 407 -> "Proxy Authentication Required";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 410 -> "Gone";
            case 411 -> "Length Required";
            case 412 -> "Precondition Failed";
            case 413 -> "Content Too Large";
            case 414 -> "URI Too Long";
            case 415 -> "Unsupported Media Type";
            case 416 -> "Range Not Satisfiable";
            case 417 -> "Expectation Failed";
            case 421 -> "Misdirected Request";
            case 422 -> "Unprocessable Content";
            case 425 -> "Too Early";
            case 426 -> "Upgrade Required";
            case 428 -> "Precondition Required";
            case 429 -> "Too Many Requests";
            case 431 -> "Request Header Fields Too Large";
            case 451 -> "Unavailable For Legal Reasons";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            case 505 -> "HTTP Version Not Supported";
            case 511 -> "Network Authentication Required";
            default -> null;
        };
    }
}
