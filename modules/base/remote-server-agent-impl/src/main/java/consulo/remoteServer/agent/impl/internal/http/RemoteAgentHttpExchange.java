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
package consulo.remoteServer.agent.impl.internal.http;

import consulo.http.HttpVersion;
import consulo.http.internal.HttpExchange;
import consulo.remoteServer.agent.impl.internal.protocol.HttpResponse;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class RemoteAgentHttpExchange implements HttpExchange {
    private final String myUrl;
    private final int myStatusCode;
    private final Map<String, List<String>> myHeaders = new LinkedHashMap<>();
    private final byte[] myBody;

    RemoteAgentHttpExchange(String requestUrl, HttpResponse response) {
        myUrl = response.getUrl().isEmpty() ? requestUrl : response.getUrl();
        myStatusCode = response.getStatusCode();
        myBody = response.getBody().toByteArray();
        for (Map.Entry<String, String> entry : response.getHeadersMap().entrySet()) {
            myHeaders.put(entry.getKey(), List.of(entry.getValue()));
        }
    }

    @Override
    public String url() {
        return myUrl;
    }

    @Override
    public int statusCode() {
        return myStatusCode;
    }

    @Override
    public @Nullable String statusMessage() {
        return null;
    }

    @Override
    public HttpVersion version() {
        return HttpVersion.HTTP_1_1;
    }

    @Override
    public Map<String, List<String>> headers() {
        return myHeaders;
    }

    @Override
    public InputStream body() {
        return new ByteArrayInputStream(myBody);
    }

    @Override
    public int contentLength() {
        return myBody.length;
    }

    @Override
    public void close() {
    }
}
