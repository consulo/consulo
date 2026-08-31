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
package consulo.http.impl.internal;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A minimal in-process HTTP server backed by Netty for exercising {@link HttpRequestImpl}
 * against a real socket without touching the network.
 *
 * @author VISTALL
 */
public final class StubHttpServer implements AutoCloseable {
    public interface Responder {
        Response respond(RecordedRequest request) throws Exception;
    }

    public static final class RecordedRequest {
        public final String method;
        public final String path;
        public final Map<String, String> headers;
        public final byte[] body;

        RecordedRequest(String method, String path, Map<String, String> headers, byte[] body) {
            this.method = method;
            this.path = path;
            this.headers = headers;
            this.body = body;
        }
    }

    public static final class Response {
        public final int status;
        public final byte[] body;
        public final Map<String, String> headers = new LinkedHashMap<>();

        public Response(int status, byte[] body) {
            this.status = status;
            this.body = body;
        }

        public static Response text(String value) {
            Response response = new Response(200, value.getBytes(StandardCharsets.UTF_8));
            response.headers.put("Content-Type", "text/plain; charset=UTF-8");
            return response;
        }
    }

    private final EventLoopGroup myBossGroup;
    private final EventLoopGroup myWorkerGroup;
    private final Channel myChannel;
    private volatile RecordedRequest myLastRequest;

    public StubHttpServer(Responder responder) throws InterruptedException {
        myBossGroup = new NioEventLoopGroup(1);
        myWorkerGroup = new NioEventLoopGroup(1);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(myBossGroup, myWorkerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new HttpServerCodec());
                    ch.pipeline().addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                    ch.pipeline().addLast(new Handler(responder));
                }
            });

        myChannel = bootstrap.bind("127.0.0.1", 0).sync().channel();
    }

    public int port() {
        return ((InetSocketAddress) myChannel.localAddress()).getPort();
    }

    public String url(String path) {
        return "http://127.0.0.1:" + port() + path;
    }

    public RecordedRequest lastRequest() {
        return myLastRequest;
    }

    @Override
    public void close() {
        myChannel.close().awaitUninterruptibly();
        myWorkerGroup.shutdownGracefully(0, 100, TimeUnit.MILLISECONDS);
        myBossGroup.shutdownGracefully(0, 100, TimeUnit.MILLISECONDS);
    }

    private final class Handler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private final Responder myResponder;

        Handler(Responder responder) {
            myResponder = responder;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            byte[] body = new byte[request.content().readableBytes()];
            request.content().readBytes(body);

            Map<String, String> headers = new HashMap<>();
            for (Map.Entry<String, String> entry : request.headers()) {
                headers.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }

            RecordedRequest recorded = new RecordedRequest(request.method().name(), request.uri(), headers, body);
            myLastRequest = recorded;

            Response response = myResponder.respond(recorded);

            ByteBuf content = Unpooled.wrappedBuffer(response.body);
            FullHttpResponse httpResponse =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.status), content);
            for (Map.Entry<String, String> entry : response.headers.entrySet()) {
                httpResponse.headers().set(entry.getKey(), entry.getValue());
            }
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

            ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
