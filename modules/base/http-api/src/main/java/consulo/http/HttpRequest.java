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
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

public interface HttpRequest {
    
    String getURL();

    int statusCode() throws IOException;

    @Nullable
    String statusMessage() throws IOException;

    
    Map<String, List<String>> responseHeaders() throws IOException;

    
    HttpVersion version();

    @Nullable
    default String headerValue(String header) throws IOException {
        Map<String, List<String>> map = responseHeaders();
        List<String> headers = map.get(header);
        return headers == null || headers.isEmpty() ? null : headers.getFirst();
    }

    @Nullable
    String getContentEncoding() throws IOException;

    @Nullable
    String getContentType() throws IOException;

    
    InputStream getInputStream() throws IOException;

    
    BufferedReader getReader() throws IOException;

    
    BufferedReader getReader(@Nullable ProgressIndicator indicator) throws IOException;

    /**
     * @deprecated Called automatically on open connection. Use {@link HttpRequestBuilder#tryConnect()} to get response code
     */
    default boolean isSuccessful() throws IOException {
        int code = statusCode();
        // zero mean it's not http connection
        return code == 0 ||code == HttpURLConnection.HTTP_OK;
    }

    
    default File saveToFile(File file, @Nullable ProgressIndicator indicator) throws IOException {
        return saveToFile(file, null, indicator);
    }

    
    File saveToFile(File file, @Nullable MessageDigest digest, @Nullable ProgressIndicator indicator) throws IOException;

    
    byte[] readBytes(@Nullable ProgressIndicator indicator) throws IOException;

    
    String readString(@Nullable ProgressIndicator indicator) throws IOException;
}
