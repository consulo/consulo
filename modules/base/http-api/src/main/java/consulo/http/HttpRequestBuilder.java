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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.net.ssl.HostnameVerifier;
import java.io.File;
import java.io.IOException;

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

    default HttpRequestBuilder userAgent(@Nullable String userAgent) {
        return header("User-Agent", userAgent);
    }

    HttpRequestBuilder productNameAsUserAgent();

    default HttpRequestBuilder accept(@Nullable String mimeType) {
        return header("Accept", mimeType);
    }

    HttpRequestBuilder header(@Nonnull String headerName, @Nullable String headerValue);

    HttpRequestBuilder version(@Nonnull HttpVersion version);

    HttpRequestBuilder body(@Nullable byte[] bytes);

    <T> T connect(@Nonnull HttpRequestProcessor<T> processor) throws IOException;

    int tryConnect() throws IOException;

    default <T> T connect(@Nonnull HttpRequestProcessor<T> processor, T errorValue, @Nullable Logger logger) {
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

    default void saveToFile(@Nonnull File file, @Nullable ProgressIndicator indicator) throws IOException {
        connect((request) -> request.saveToFile(file, indicator));
    }

    @Nonnull
    default byte[] readBytes(@Nullable ProgressIndicator indicator) throws IOException {
        return connect((request) -> request.readBytes(indicator));
    }

    @Nonnull
    default String readString(@Nullable ProgressIndicator indicator) throws IOException {
        return connect((request) -> request.readString(indicator));
    }
}