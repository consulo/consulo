/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.application.progress.ProgressIndicator;
import consulo.logging.Logger;
import consulo.ui.Component;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.HostnameVerifier;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface HttpRequestBuilder {
    /**
     * By default connection will throw exception if code is not 200
     */
    HttpRequestBuilder allowErrorCodes(boolean allowErrorCodes);

    HttpRequestBuilder connectTimeout(int value);

    HttpRequestBuilder readTimeout(int value);

    HttpRequestBuilder redirectLimit(int redirectLimit);

    HttpRequestBuilder gzip(boolean value);

    HttpRequestBuilder forceHttps(boolean forceHttps);

    HttpRequestBuilder useProxy(boolean useProxy);

    HttpRequestBuilder hostNameVerifier(@Nullable HostnameVerifier hostnameVerifier);

    /**
     * The component the request is sent for. A question the request has to ask the user - whether to trust a
     * certificate the server sent - is shown in the ui of this component.
     */
    HttpRequestBuilder uiOwner(@Nullable Component component);

    default HttpRequestBuilder userAgent(@Nullable String userAgent) {
        return header("User-Agent", userAgent);
    }

    HttpRequestBuilder productNameAsUserAgent();

    default HttpRequestBuilder accept(@Nullable String mimeType) {
        return header("Accept", mimeType);
    }

    HttpRequestBuilder header(String headerName, @Nullable String headerValue);

    HttpRequestBuilder version(HttpVersion version);

    HttpRequestBuilder body(@Nullable byte[] bytes);

    String getUrl();

    HttpMethod getMethod();

    @Nullable String getHeader(String headerName);

    <T> T connect(HttpRequestProcessor<T> processor) throws IOException;

    /**
     * Sends the request on the application pool. What the builder was told is taken when this is called, so the builder
     * may be changed or reused right after.
     * <p>
     * The future fails with the {@link IOException} {@link #connect(HttpRequestProcessor)} would throw. Cancelling it cancels the
     * progress the exchange runs under: a waiting connection gives up and the body stops being read.
     */
    <T> CompletableFuture<T> connectAsync(HttpRequestProcessor<T> processor);

    int tryConnect() throws IOException;

    default <T> T connect(HttpRequestProcessor<T> processor, T errorValue, @Nullable Logger logger) {
        try {
            return connect(processor);
        }
        catch (Throwable e) {
            if (logger != null) {
                logger.warn(e);
            }
            return errorValue;
        }
    }

    default void saveToFile(Path file, @Nullable ProgressIndicator indicator) throws IOException {
        connect((request) -> request.saveToFile(file, indicator));
    }

    default void saveToFile(File file, @Nullable ProgressIndicator indicator) throws IOException {
        connect((request) -> request.saveToFile(file, indicator));
    }

    default byte[] readBytes(@Nullable ProgressIndicator indicator) throws IOException {
        return connect((request) -> request.readBytes(indicator));
    }

    default String readString(@Nullable ProgressIndicator indicator) throws IOException {
        return connect((request) -> request.readString(indicator));
    }
}