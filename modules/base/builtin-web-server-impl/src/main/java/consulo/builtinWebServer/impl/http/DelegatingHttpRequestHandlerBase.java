/*
 * Copyright 2013-2017 consulo.io
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
import consulo.logging.Logger;
import consulo.util.netty.SimpleChannelInboundHandlerAdapter;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 06-May-17
 * <p>
 * from kotlin platform\platform-impl\src\org\jetbrains\io\DelegatingHttpRequestHandlerBase.kt
 */
public abstract class DelegatingHttpRequestHandlerBase extends SimpleChannelInboundHandlerAdapter<FullHttpRequest> {
    @Override
    public void messageReceived(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        send(context, request, process(context, request, new QueryStringDecoder(request.uri())));
    }

    private void send(ChannelHandlerContext context, FullHttpRequest request, @Nullable HttpResponse httpResponse) throws IOException {
        if (httpResponse == null) {
            Responses.send(HttpResponseStatus.NOT_FOUND, context.channel(), request);
        }
        else if (httpResponse.getDeferred() != null) {
            sendDeferred(context, request, httpResponse.getDeferred());
        }
        else if (httpResponse.getStreamingBody() != null) {
            StreamingResponses.send(context.channel(), httpResponse);
        }
        else if (httpResponse.getFileRegion() != null) {
            FileRegionResponses.send(context.channel(), request, httpResponse);
        }
        else {
            byte[] content = httpResponse.getContent();

            FullHttpResponse response = Responses.response(
                HttpResponseStatus.valueOf(httpResponse.getCode()),
                httpResponse.getContentType(),
                content == null ? null : Unpooled.copiedBuffer(content)
            );
            for (Map.Entry<String, String> header : httpResponse.getHeaders().entrySet()) {
                response.headers().set(header.getKey(), header.getValue());
            }
            Responses.send(response, context.channel(), request);
        }
    }

    private void sendDeferred(ChannelHandlerContext context, FullHttpRequest request, CompletableFuture<HttpResponse> deferred) {
        Channel channel = context.channel();
        FullHttpRequest retained = request.retainedDuplicate();
        channel.closeFuture().addListener(future -> deferred.cancel(false));
        deferred.whenComplete((response, error) -> channel.eventLoop().execute(() -> {
            try {
                if (!channel.isActive()) {
                    return;
                }
                if (error == null) {
                    send(context, retained, response);
                }
                else if (error instanceof CancellationException || error.getCause() instanceof CancellationException) {
                    channel.close();
                }
                else {
                    Logger.getInstance(BuiltInServer.class).warn(error);
                    Responses.send(HttpResponseStatus.INTERNAL_SERVER_ERROR, channel, retained);
                }
            }
            catch (Throwable e) {
                NettyUtil.logAndClose(e, Logger.getInstance(BuiltInServer.class), channel);
            }
            finally {
                retained.release();
            }
        }));
    }

    protected abstract @Nullable HttpResponse process(
        ChannelHandlerContext context,
        FullHttpRequest request,
        QueryStringDecoder urlDecoder
    ) throws Exception;

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        NettyUtil.logAndClose(cause, Logger.getInstance(BuiltInServer.class), context.channel());
    }
}
