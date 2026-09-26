// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webSocket;

import consulo.builtinWebServer.impl.jsonRpc.Client;
import consulo.builtinWebServer.webSocket.WebSocketConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;

import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;

public class WebSocketClient extends Client implements WebSocketConnection {
    private final WebSocketServerHandshaker myHandshaker;

    WebSocketClient(Channel channel, WebSocketServerHandshaker handshaker) {
        super(channel);

        myHandshaker = handshaker;
    }

    @Override
    public ChannelFuture send(ByteBuf message) {
        return sendFrame(message, false);
    }

    @Override
    public void send(String text) {
        send(Unpooled.copiedBuffer(text, StandardCharsets.UTF_8));
    }

    @Override
    public void send(byte[] data) {
        sendFrame(Unpooled.wrappedBuffer(data), true);
    }

    public ChannelFuture sendFrame(ByteBuf message, boolean binary) {
        if (myChannel.isOpen()) {
            return myChannel.writeAndFlush(binary ? new BinaryWebSocketFrame(message) : new TextWebSocketFrame(message));
        }
        else {
            return myChannel.newFailedFuture(new ClosedChannelException());
        }
    }

    @Override
    public void sendHeartbeat() {
        myChannel.writeAndFlush(new PingWebSocketFrame());
    }

    public void disconnect(CloseWebSocketFrame frame) {
        myHandshaker.close(myChannel, frame);
    }
}
