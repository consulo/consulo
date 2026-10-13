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

import com.google.protobuf.ByteString;
import consulo.annotation.component.ExtensionImpl;
import consulo.remoteServer.agent.impl.internal.RemoteAgentException;
import consulo.remoteServer.agent.impl.internal.platform.RemotePlatform;
import consulo.http.internal.HttpExchange;
import consulo.http.internal.HttpRequestOptions;
import consulo.http.internal.PlatformHttpRequestSender;
import consulo.platform.Platform;
import consulo.remoteServer.agent.impl.internal.protocol.HttpRequest;
import consulo.remoteServer.agent.impl.internal.protocol.HttpResponse;

import java.io.IOException;

@ExtensionImpl
public class RemoteAgentHttpRequestSender implements PlatformHttpRequestSender {
    @Override
    public boolean isApplicable(Platform platform) {
        return platform instanceof RemotePlatform;
    }

    @Override
    public HttpExchange send(Platform platform, HttpRequestOptions options) throws IOException {
        RemotePlatform remotePlatform = (RemotePlatform) platform;

        String url = options.url();
        if (options.forceHttps() && url.startsWith("http:")) {
            url = "https:" + url.substring(5);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .setMethod(options.method().name())
            .setUrl(url)
            .putAllHeaders(options.headers())
            .setConnectTimeoutMillis(options.connectTimeout())
            .setReadTimeoutMillis(options.readTimeout())
            .setMaxRedirects(Math.max(options.redirectLimit() - 1, 0));
        byte[] body = options.body();
        if (body != null) {
            builder.setBody(ByteString.copyFrom(body));
        }
        HttpRequest request = builder.build();

        try {
            HttpResponse response = remotePlatform.getConnection().executeHttpRequest(request);
            return new RemoteAgentHttpExchange(url, response);
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
