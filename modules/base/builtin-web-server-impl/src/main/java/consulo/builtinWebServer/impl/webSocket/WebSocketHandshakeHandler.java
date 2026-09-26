// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webSocket;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.impl.http.BuiltInServer;
import consulo.builtinWebServer.impl.http.NettyUtil;
import consulo.builtinWebServer.impl.jsonRpc.Client;
import consulo.builtinWebServer.impl.jsonRpc.ClientListener;
import consulo.builtinWebServer.impl.jsonRpc.ClientManager;
import consulo.builtinWebServer.impl.jsonRpc.ExceptionHandler;
import consulo.builtinWebServer.impl.jsonRpc.MessageServer;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.http.HttpMethod;
import consulo.logging.Logger;
import consulo.util.lang.lazy.LazyValue;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class WebSocketHandshakeHandler extends HttpRequestHandler implements ClientListener, ExceptionHandler {
    private static final Logger LOG = Logger.getInstance(WebSocketHandshakeHandler.class);

    private final BuiltInServerManager myServerManager;
    private final ApplicationConcurrency myApplicationConcurrency;

    private final LazyValue<ClientManager> myClientManager = LazyValue.atomicNotNull(this::createClientManager);

    protected WebSocketHandshakeHandler(BuiltInServerManager serverManager, ApplicationConcurrency applicationConcurrency) {
        myServerManager = serverManager;
        myApplicationConcurrency = applicationConcurrency;
    }

    private ClientManager createClientManager() {
        ClientManager result = new ClientManager(this, this, null, myApplicationConcurrency.getScheduledExecutorService());
        Disposable serverDisposable = Objects.requireNonNull(myServerManager.getServerDisposable());
        Disposer.register(serverDisposable, result);
        serverCreated(result);
        return result;
    }

    @Override
    public boolean isSupported(HttpRequest request) {
        return request.method() == HttpMethod.GET
            && "WebSocket".equalsIgnoreCase(request.getHeaderValue(HttpHeaderNames.UPGRADE.toString()))
            && request.uri().length() > 2;
    }

    protected void serverCreated(ClientManager server) {
    }

    @Override
    public void exceptionCaught(Throwable e) {
        NettyUtil.log(e, LOG);
    }

    @Override
    public final @Nullable HttpResponse process(HttpRequest request) {
        return null;
    }

    public final void handshake(ChannelHandlerContext context, FullHttpRequest request, QueryStringDecoder uriDecoder) {
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
            "ws://" + request.headers().getAsString(HttpHeaderNames.HOST) + uriDecoder.path(),
            null,
            false,
            NettyUtil.MAX_CONTENT_LENGTH
        );
        WebSocketServerHandshaker handshaker = factory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(context.channel());
            return;
        }

        if (!context.channel().isOpen()) {
            return;
        }

        Client client = new WebSocketClient(context.channel(), handshaker);
        context.channel().attr(ClientManager.CLIENT).set(client);
        handshaker.handshake(context.channel(), request).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ClientManager clientManager = myClientManager.get();
                clientManager.addClient(client);
                MessageChannelHandler messageChannelHandler = new MessageChannelHandler(clientManager, getMessageServer());
                BuiltInServer.replaceDefaultHandler(context, messageChannelHandler);
                ChannelHandlerContext messageChannelHandlerContext = context.pipeline().context(messageChannelHandler);
                context.pipeline().addBefore(
                    messageChannelHandlerContext.name(),
                    "webSocketFrameAggregator",
                    new WebSocketFrameAggregator(NettyUtil.MAX_CONTENT_LENGTH)
                );
                messageChannelHandlerContext.channel().attr(ClientManager.CLIENT).set(client);
                connected(client, uriDecoder.parameters());
            }
        });
    }

    protected abstract MessageServer getMessageServer();

    @Override
    public void connected(Client client, @Nullable Map<String, List<String>> parameters) {
    }
}
