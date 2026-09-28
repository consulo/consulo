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
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An answer read through {@link URLConnection} - the connection is opened when the answer is first asked for.
 *
 * @author VISTALL
 * @since 2026-02-18
 */
class LocalUrlConnectionRequest extends BaseHttpRequestImpl {
    private @Nullable URLConnection myConnection;

    LocalUrlConnectionRequest(HttpRequestOptions options) {
        super(options);
    }

    @Override
    public String getURL() {
        if (myConnection != null) {
            return myConnection.getURL().toExternalForm();
        }
        return myOptions.url();
    }

    @Override
    public int statusCode() throws IOException {
        URLConnection connection = getConnection();
        if (connection instanceof HttpURLConnection httpURLConnection) {
            return httpURLConnection.getResponseCode();
        }
        return 0;
    }

    @Override
    public @Nullable String statusMessage() throws IOException {
        URLConnection connection = getConnection();
        if (connection instanceof HttpURLConnection httpURLConnection) {
            return httpURLConnection.getResponseMessage();
        }
        return null;
    }

    @Override
    public HttpVersion version() {
        // TODO unsupported for now
        return HttpVersion.HTTP_1_1;
    }

    @Override
    public Map<String, List<String>> responseHeaders() throws IOException {
        URLConnection connection = getConnection();

        Map<String, List<String>> headers = Maps.newHashMap(HashingStrategy.caseInsensitive());
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }

            headers.put(key, new ArrayList<>(entry.getValue()));
        }
        return headers;
    }

    private URLConnection getConnection() throws IOException {
        if (myConnection == null) {
            myConnection = LocalUrlConnectionExecutor.openConnection(myOptions);
        }
        return myConnection;
    }

    @Override
    public @Nullable String getContentEncoding() throws IOException {
        return getConnection().getContentEncoding();
    }

    @Override
    public @Nullable String getContentType() throws IOException {
        return getConnection().getContentType();
    }

    @Override
    protected InputStream openInputStream() throws IOException {
        return getConnection().getInputStream();
    }

    @Override
    protected int getContentLength() throws IOException {
        return getConnection().getContentLength();
    }

    @Override
    protected void disconnect() {
        if (myConnection instanceof HttpURLConnection httpURLConnection) {
            httpURLConnection.disconnect();
        }
    }
}
