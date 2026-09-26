package consulo.builtinWebServer.impl.webSocket;

import consulo.builtinWebServer.impl.jsonRpc.Client;
import consulo.builtinWebServer.impl.jsonRpc.ClientManager;
import consulo.builtinWebServer.impl.jsonRpc.MessageServer;
import consulo.util.netty.NettyKt;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

@ChannelHandler.Sharable
final class MessageChannelHandler extends WebSocketProtocolHandler {
    private final ClientManager myClientManager;
    private final MessageServer myMessageServer;

    MessageChannelHandler(ClientManager clientManager, MessageServer messageServer) {
        myClientManager = clientManager;
        myMessageServer = messageServer;
    }

    @Override
    protected void closeFrameReceived(Channel channel, CloseWebSocketFrame message) {
        WebSocketClient client = (WebSocketClient) channel.attr(ClientManager.CLIENT).get();
        if (client == null) {
            super.closeFrameReceived(channel, message);
        }
        else {
            try {
                myClientManager.disconnectClient(channel, client, false);
            }
            finally {
                client.disconnect(message);
            }
        }
    }

    @Override
    protected void textFrameReceived(Channel channel, TextWebSocketFrame message) {
        WebSocketClient client = (WebSocketClient) channel.attr(ClientManager.CLIENT).get();
        CharSequence chars;
        try {
            chars = NettyKt.readUtf8(message.content());
        }
        catch (Throwable e) {
            try {
                message.release();
            }
            finally {
                myClientManager.getExceptionHandler().exceptionCaught(e);
            }
            return;
        }

        try {
            myMessageServer.messageReceived(client, chars);
        }
        catch (Throwable e) {
            myClientManager.getExceptionHandler().exceptionCaught(e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        Client client = context.channel().attr(ClientManager.CLIENT).get();
        if (client != null) {
            myClientManager.disconnectClient(context.channel(), client, false);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        try {
            myClientManager.getExceptionHandler().exceptionCaught(cause);
        }
        finally {
            super.exceptionCaught(context, cause);
        }
    }
}
