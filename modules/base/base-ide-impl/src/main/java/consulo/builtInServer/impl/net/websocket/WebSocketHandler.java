/*
 * Copyright 2013-2020 consulo.io
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
package consulo.builtInServer.impl.net.websocket;

import consulo.builtInServer.websocket.WebSocketAccepter;
import consulo.builtInServer.websocket.WebSocketConnection;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.*;

public class WebSocketHandler extends ChannelInboundHandlerAdapter {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof WebSocketFrame) {
      if (msg instanceof BinaryWebSocketFrame) {
        if(WebSocketAccepter.EP_NAME.hasAnyExtensions()) {
          byte[] array = ByteBufUtil.getBytes(((BinaryWebSocketFrame)msg).content());

          WebSocketConnection connection = new WebSocketConnectionImpl(ctx);
          for (WebSocketAccepter accepter : WebSocketAccepter.EP_NAME.getExtensionList()) {
            accepter.accept(connection, array);
          }
        }
      }
      else if (msg instanceof TextWebSocketFrame) {
        if (WebSocketAccepter.EP_NAME.hasAnyExtensions()) {
          String text = ((TextWebSocketFrame)msg).text();

          WebSocketConnection connection = new WebSocketConnectionImpl(ctx);
          for (WebSocketAccepter accepter : WebSocketAccepter.EP_NAME.getExtensionList()) {
            accepter.accept(connection, text);
          }
        }
      }
      //else if (msg instanceof PingWebSocketFrame) {
      //}
      //else if (msg instanceof PongWebSocketFrame) {
      //}
      //else if (msg instanceof CloseWebSocketFrame) {
      //}
      //else {
      //}
    }
  }
}
