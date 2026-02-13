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

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.MessageDigest;

/**
 * Handy class for reading data from HTTP connections with built-in support for HTTP redirects and gzipped content and automatic cleanup.
 * Usage: <pre>{@code
 * int firstByte = HttpRequests.request(url).connect(new HttpRequests.RequestProcessor<Integer>() {
 *   public Integer process(@NotNull Request request) throws IOException {
 *     return request.getInputStream().read();
 *   }
 * });
 * }</pre>
 */
public final class HttpRequests {
    private HttpRequests() {
    }

    public interface Request {
        @Nonnull
        String getURL();

        @Nonnull
        URLConnection getConnection() throws IOException;

        @Nonnull
        InputStream getInputStream() throws IOException;

        @Nonnull
        BufferedReader getReader() throws IOException;

        @Nonnull
        BufferedReader getReader(@Nullable ProgressIndicator indicator) throws IOException;

        /**
         * @deprecated Called automatically on open connection. Use {@link RequestBuilder#tryConnect()} to get response code
         */
        boolean isSuccessful() throws IOException;

        @Nonnull
        default File saveToFile(@Nonnull File file, @Nullable ProgressIndicator indicator) throws IOException {
            return saveToFile(file, null, indicator);
        }

        @Nonnull
        File saveToFile(@Nonnull File file, @Nullable MessageDigest digest, @Nullable ProgressIndicator indicator) throws IOException;

        @Nonnull
        byte[] readBytes(@Nullable ProgressIndicator indicator) throws IOException;

        @Nonnull
        String readString(@Nullable ProgressIndicator indicator) throws IOException;
    }

    public interface RequestProcessor<T> {
        T process(@Nonnull Request request) throws IOException;
    }

    public interface ConnectionTuner {
        void tune(@Nonnull URLConnection connection) throws IOException;
    }

    public static class HttpStatusException extends IOException {
        private int myStatusCode;
        private String myUrl;

        public HttpStatusException(@Nonnull String message, int statusCode, @Nonnull String url) {
            super(message);
            myStatusCode = statusCode;
            myUrl = url;
        }

        public int getStatusCode() {
            return myStatusCode;
        }

        @Nonnull
        public String getUrl() {
            return myUrl;
        }

        @Override
        public String getMessage() {
            return "Status: " + myStatusCode;
        }

        @Override
        public String toString() {
            return super.toString() + ". Status=" + myStatusCode + ", Url=" + myUrl;
        }
    }

    @Nonnull
    @Deprecated
    public static RequestBuilder request(@Nonnull String url) {
        HttpRequestFactory factory = Application.get().getInstance(HttpRequestFactory.class);
        
        return factory.request(url);
    }
}