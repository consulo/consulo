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
package consulo.ide.impl.builtInServer.impl.net.http;

import consulo.builtinWebServer.http.HttpResponse;
import consulo.logging.Logger;
import consulo.util.netty.SimpleChannelInboundHandlerAdapter;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 06-May-17
 * <p>
 * from kotlin platform\platform-impl\src\org\jetbrains\io\DelegatingHttpRequestHandlerBase.kt
 */
public abstract class DelegatingHttpRequestHandlerBase extends SimpleChannelInboundHandlerAdapter<FullHttpRequest> {
  @Override
  public void messageReceived(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
    HttpResponse httpResponse = process(context, request, new QueryStringDecoder(request.uri()));
    if (httpResponse == null) {
      Responses.send(HttpResponseStatus.NOT_FOUND, context.channel(), request);
    }
    else {
      byte[] content = httpResponse.getContent();

      FullHttpResponse response = Responses.response(httpResponse.getContentType(), content == null ? null : Unpooled.copiedBuffer(content));
      Responses.send(response, context.channel(), request);
    }
  }

  @Nullable
  protected abstract HttpResponse process(ChannelHandlerContext context, FullHttpRequest request, QueryStringDecoder urlDecoder) throws Exception;

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
    NettyUtil.logAndClose(cause, Logger.getInstance(BuiltInServer.class), context.channel());
  }
}
