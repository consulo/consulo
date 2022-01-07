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

import com.google.gson.Gson;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.HttpRequests;
import consulo.builtInServer.http.HttpRequestHandler;
import consulo.builtInServer.http.Responses;
import consulo.external.api.UserAccount;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 04/09/2021
 */
public class RedirectAuthHttpHandler extends HttpRequestHandler {
  private static final Logger LOG = Logger.getInstance(RedirectAuthHttpHandler.class);

  public static class OAuthRequestResult {
    public UserAccount userAccount;
    public String token;
  }

  @Override
  public boolean isSupported(FullHttpRequest request) {
    return request.method() == HttpMethod.GET && checkPrefix(request.uri(), "redirectAuth");
  }

  @Override
  public boolean process(@Nonnull QueryStringDecoder urlDecoder, @Nonnull FullHttpRequest request, @Nonnull ChannelHandlerContext context) throws IOException {
    String token = ContainerUtil.getFirstItem(urlDecoder.parameters().get("token"));
    if (token != null) {
      doGetToken(token);
    }

    FullHttpResponse response = Responses.response("text/html; charset=utf-8", null);

    InputStream stream = RedirectAuthHttpHandler.class.getResourceAsStream("/html/redirectAuth.html");

    String html = StreamUtil.readText(stream, StandardCharsets.UTF_8);

    response.content().writeBytes(html.getBytes(StandardCharsets.UTF_8));

    Responses.send(response, context.channel(), request);
    return true;
  }

  private static void doGetToken(String sharedToken) {
    Task.Backgroundable.queue(null, "Requesting oauth token...", indicator -> {
      UIAccess uiAccess = Application.get().getLastUIAccess();

      try {
        String json = HttpRequests.request(WebServiceApi.OAUTH_API.buildUrl("request?token=" + sharedToken)).readString(indicator);

        OAuthRequestResult requestResult = new Gson().fromJson(json, OAuthRequestResult.class);

        ExternalServiceConfiguration externalServiceConfiguration = Application.get().getInstance(ExternalServiceConfiguration.class);

        ((ExternalServiceConfigurationImpl)externalServiceConfiguration).authorize(requestResult.userAccount.username, requestResult.token);

        externalServiceConfiguration.updateIcon();

        uiAccess.give(() -> Alerts.okInfo(LocalizeValue.localizeTODO("Successfuly logged as " + requestResult.userAccount.username)).showAsync());
      }
      catch (IOException e) {
        LOG.warn(e);

        uiAccess.give(() -> Alerts.okError(LocalizeValue.localizeTODO("Failed to request oauth token")).showAsync());
      }
    });
  }
}
