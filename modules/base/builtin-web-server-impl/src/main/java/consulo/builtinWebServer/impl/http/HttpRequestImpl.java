/*
 * Copyright 2013-2022 consulo.io
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
package consulo.builtinWebServer.impl.http;

import consulo.builtinWebServer.http.HttpRequest;
import consulo.http.HTTPMethod;
import consulo.util.collection.ContainerUtil;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author VISTALL
 * @since 13-Sep-22
 */
public class HttpRequestImpl implements HttpRequest {
  private final FullHttpRequest myFullHttpRequest;

  private final HTTPMethod myMethod;

  private final ChannelHandlerContext myContext;

  private final QueryStringDecoder myQueryStringDecoder;

  public HttpRequestImpl(FullHttpRequest fullHttpRequest, QueryStringDecoder urlDecoder, ChannelHandlerContext context) {
    myFullHttpRequest = fullHttpRequest;
    myMethod = HTTPMethod.valueOf(myFullHttpRequest.method().name());
    myContext = context;
    myQueryStringDecoder = urlDecoder;
  }

  @Nonnull
  @Override
  public HTTPMethod method() {
    return myMethod;
  }

  @Nonnull
  @Override
  public String getContentAsString(@Nonnull Charset charset) throws IOException {
    return myFullHttpRequest.content().toString(charset);
  }

  @Override
  public byte[] getContent() throws IOException {
    return ByteBufUtil.getBytes(myFullHttpRequest.content());
  }

  @Nonnull
  @Override
  public String uri() {
    return myFullHttpRequest.uri();
  }

  @Nonnull
  @Override
  public String path() {
    return myQueryStringDecoder.path();
  }

  @Nullable
  @Override
  public String getHeaderValue(@Nonnull String headerName) {
    return myFullHttpRequest.headers().getAsString(headerName);
  }

  @Nullable
  @Override
  public String getParameterValue(@Nonnull String parameter) {
    return ContainerUtil.getFirstItem(myQueryStringDecoder.parameters().get(parameter));
  }

  @Override
  public void terminate() {
    myContext.close();
  }

  public FullHttpRequest getFullHttpRequest() {
    return myFullHttpRequest;
  }
}
