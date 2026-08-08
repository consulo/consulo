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
package consulo.builtinWebServer.impl.http;

import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.http.HttpResponseStream;
import consulo.builtinWebServer.http.HttpStreamingBody;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
final class StreamingResponses {
    static void send(Channel channel, HttpResponse response) {
        HttpStreamingBody body = response.getStreamingBody();
        assert body != null;

        io.netty.handler.codec.http.HttpResponse nettyResponse =
            new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.getCode()));

        String contentType = response.getContentType();
        if (contentType != null) {
            nettyResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        }
        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            nettyResponse.headers().set(header.getKey(), header.getValue());
        }

        HttpUtil.setTransferEncodingChunked(nettyResponse, true);
        HttpUtil.setKeepAlive(nettyResponse, true);
        Responses.addNoCache(nettyResponse);
        Responses.addServer(nettyResponse);
        Responses.setDate(nettyResponse);

        channel.writeAndFlush(nettyResponse);

        HttpResponseStream stream = new NettyHttpResponseStream(channel);
        try {
            body.start(stream);
        }
        catch (Throwable e) {
            stream.close();
            throw e;
        }
    }

    private StreamingResponses() {
    }
}
