// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webSocket;

import consulo.builtinWebServer.impl.http.NettyUtil;
import consulo.builtinWebServer.impl.webServer.BuiltInWebServer;
import consulo.logging.Logger;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;

public abstract class WebSocketProtocolHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = Logger.getInstance(BuiltInWebServer.class);

    @Override
    public final void channelRead(ChannelHandlerContext context, Object message) {
        switch (message) {
            case PongWebSocketFrame pong -> ReferenceCountUtil.release(pong);
            case PingWebSocketFrame ping -> context.channel().writeAndFlush(new PongWebSocketFrame(ping.content()));
            case CloseWebSocketFrame close -> closeFrameReceived(context.channel(), close);
            case TextWebSocketFrame text -> {
                try {
                    textFrameReceived(context.channel(), text);
                }
                finally {
                    if (text.refCnt() > 0) {
                        text.release();
                    }
                }
            }
            case WebSocketFrame frame -> throw new UnsupportedOperationException(frame.getClass().getName() + " frame types not supported");
            default -> ReferenceCountUtil.release(message);
        }
    }

    protected abstract void textFrameReceived(Channel channel, TextWebSocketFrame message);

    protected void closeFrameReceived(Channel channel, CloseWebSocketFrame message) {
        channel.close();
        message.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        NettyUtil.logAndClose(cause, LOG, context.channel());
    }
}
