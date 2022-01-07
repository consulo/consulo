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
package consulo.builtInServer.impl.net.http;

import consulo.logging.Logger;
import consulo.builtInServer.http.Responses;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * @author VISTALL
 * @since 06-May-17
 * <p>
 * from kotlin platform\platform-impl\src\org\jetbrains\io\DelegatingHttpRequestHandlerBase.kt
 */
public abstract class DelegatingHttpRequestHandlerBase extends SimpleChannelInboundHandlerAdapter<FullHttpRequest> {
  @Override
  public void messageReceived(ChannelHandlerContext context, FullHttpRequest message) throws Exception {
    Logger logger = Logger.getInstance(BuiltInServer.class);
    if (logger.isDebugEnabled()) {
      logger.debug("\n\nIN HTTP: $message\n\n");
    }

    if (!process(context, message, new QueryStringDecoder(message.uri()))) {
      Responses.send(HttpResponseStatus.NOT_FOUND, context.channel(), message);
    }
  }

  protected abstract boolean process(ChannelHandlerContext context, FullHttpRequest request, QueryStringDecoder urlDecoder) throws Exception;

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
    NettyUtil.logAndClose(cause, Logger.getInstance(BuiltInServer.class), context.channel());
  }
}
