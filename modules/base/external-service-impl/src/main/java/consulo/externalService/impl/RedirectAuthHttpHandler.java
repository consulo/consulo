/*
 * Copyright 2013-2021 consulo.io
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
package consulo.externalService.impl;

import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.builtInServer.http.HttpRequestHandler;
import consulo.builtInServer.http.Responses;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 04/09/2021
 */
public class RedirectAuthHttpHandler extends HttpRequestHandler {
  private final Provider<HubAuthorizationService> myHubAuthorizationServiceProvider;

  @Inject
  public RedirectAuthHttpHandler(Provider<HubAuthorizationService> hubAuthorizationServiceProvider) {
    myHubAuthorizationServiceProvider = hubAuthorizationServiceProvider;
  }

  @Override
  public boolean isSupported(FullHttpRequest request) {
    return request.method() == HttpMethod.GET && checkPrefix(request.uri(), "redirectAuth");
  }

  @Override
  public boolean process(@Nonnull QueryStringDecoder urlDecoder, @Nonnull FullHttpRequest request, @Nonnull ChannelHandlerContext context) throws IOException {
    String token = ContainerUtil.getFirstItem(urlDecoder.parameters().get("token"));
    if (token != null) {
      myHubAuthorizationServiceProvider.get().doGetToken(token);
    }

    FullHttpResponse response = Responses.response("text/html; charset=utf-8", null);

    InputStream stream = RedirectAuthHttpHandler.class.getResourceAsStream("/html/redirectAuth.html");

    String html = StreamUtil.readText(stream, StandardCharsets.UTF_8);

    response.content().writeBytes(html.getBytes(StandardCharsets.UTF_8));

    Responses.send(response, context.channel(), request);
    return true;
  }
}
