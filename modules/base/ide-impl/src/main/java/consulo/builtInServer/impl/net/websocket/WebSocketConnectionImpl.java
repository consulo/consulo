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

import consulo.builtInServer.websocket.WebSocketConnection;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * @author VISTALL
 * @since 2020-06-14
 */
public class WebSocketConnectionImpl implements WebSocketConnection {
  private final ChannelHandlerContext myContext;

  public WebSocketConnectionImpl(ChannelHandlerContext context) {
    myContext = context;
  }

  @Override
  public void send(String text) {
    myContext.channel().writeAndFlush(new TextWebSocketFrame(text));
  }

  @Override
  public void send(byte[] data) {
    myContext.channel().writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)));
  }
}
