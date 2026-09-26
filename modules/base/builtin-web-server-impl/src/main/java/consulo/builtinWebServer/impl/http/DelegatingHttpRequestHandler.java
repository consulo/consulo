package consulo.builtinWebServer.impl.http;

import consulo.application.Application;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.http.util.HttpRequestUtil;
import consulo.builtinWebServer.impl.webSocket.WebSocketHandler;
import consulo.builtinWebServer.impl.webSocket.WebSocketHandshakeHandler;
import consulo.logging.Logger;
import consulo.util.lang.function.ThrowableFunction;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.io.IOException;

@ChannelHandler.Sharable
final class DelegatingHttpRequestHandler extends DelegatingHttpRequestHandlerBase {
    private static final AttributeKey<HttpRequestHandler> PREV_HANDLER = AttributeKey.valueOf("DelegatingHttpRequestHandler.handler");

    @Override
    public void messageReceived(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        HttpHeaders headers = request.headers();
        if (HttpMethod.GET.equals(request.method()) && "WebSocket".equalsIgnoreCase(headers.getAsString(HttpHeaderNames.UPGRADE))) {
            QueryStringDecoder urlDecoder = new QueryStringDecoder(request.uri());
            consulo.builtinWebServer.http.HttpRequest httpRequest = new HttpRequestImpl(request, urlDecoder, context);
            WebSocketHandshakeHandler handshakeHandler = Application.get().getExtensionPoint(HttpRequestHandler.class).computeSafeIfAny(
                handler -> handler instanceof WebSocketHandshakeHandler webSocketHandler && webSocketHandler.isSupported(httpRequest)
                    ? webSocketHandler
                    : null
            );
            if (handshakeHandler != null) {
                if (isApplicable(handshakeHandler, httpRequest)) {
                    handshakeHandler.handshake(context, request, urlDecoder);
                }
                else {
                    Responses.send(HttpResponseStatus.NOT_FOUND, context.channel(), request);
                }
                return;
            }

            if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION))) {
                context.pipeline().replace(this, "websocketHandler", new WebSocketHandler());
                handleHandshake(context, request);
                return;
            }
        }

        super.messageReceived(context, request);
    }

    @Override
    protected HttpResponse process(ChannelHandlerContext context, FullHttpRequest request, QueryStringDecoder urlDecoder) throws Exception {
        consulo.builtinWebServer.http.HttpRequest httpRequest = new HttpRequestImpl(request, urlDecoder, context);
        ThrowableFunction<HttpRequestHandler, HttpResponse, IOException> checkAndProcess = httpRequestHandler -> {
            if (isApplicable(httpRequestHandler, httpRequest)) {
                return httpRequestHandler.process(httpRequest);
            }
            return null;
        };

        Attribute<HttpRequestHandler> prevHandlerAttribute = context.channel().attr(PREV_HANDLER);
        HttpRequestHandler connectedHandler = prevHandlerAttribute.get();
        if (connectedHandler != null) {
            HttpResponse temp = checkAndProcess.apply(connectedHandler);
            if (temp != null) {
                return temp;
            }
            // prev cached connectedHandler is not suitable for this request, so, let's find it again
            prevHandlerAttribute.set(null);
        }

        return Application.get().getExtensionPoint(HttpRequestHandler.class).computeSafeIfAny(handler -> {
            try {
                HttpResponse temp = checkAndProcess.apply(handler);
                if (temp != null) {
                    prevHandlerAttribute.set(handler);
                    return temp;
                }
            }
            catch (Throwable e) {
                Logger.getInstance(BuiltInServer.class).error(e);
            }
            return null;
        });
    }

    private static boolean isApplicable(HttpRequestHandler handler, consulo.builtinWebServer.http.HttpRequest request) {
        return handler.isSupported(request)
            && !HttpRequestUtil.isWriteFromBrowserWithoutOrigin(request)
            && handler.isAccessible(request);
    }

    private void handleHandshake(ChannelHandlerContext ctx, HttpRequest req) {
        WebSocketServerHandshakerFactory wsFactory =
            new WebSocketServerHandshakerFactory(getWebSocketURL(req), null, true, Integer.MAX_VALUE);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }
        else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    private String getWebSocketURL(HttpRequest req) {
        return "ws://" + req.headers().get("Host") + req.uri();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            ctx.channel().attr(PREV_HANDLER).set(null);
        }
        finally {
            super.exceptionCaught(ctx, cause);
        }
    }
}
