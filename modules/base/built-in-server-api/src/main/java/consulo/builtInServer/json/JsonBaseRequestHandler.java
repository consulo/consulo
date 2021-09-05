/*
 * Copyright 2013-2016 consulo.io
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
package consulo.builtInServer.json;

import com.google.gson.GsonBuilder;
import consulo.builtInServer.http.HttpRequestHandler;
import consulo.builtInServer.http.Responses;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public abstract class JsonBaseRequestHandler extends HttpRequestHandler {
  protected static final class JsonResponse {
    public boolean success;
    public String message;
    public Object data;

    private JsonResponse() {
    }

    public static JsonResponse asSuccess(@Nullable Object data) {
      JsonResponse response = new JsonResponse();
      response.success = true;
      response.message = null;
      response.data = data;
      return response;
    }

    public static JsonResponse asError(@Nonnull String message) {
      JsonResponse response = new JsonResponse();
      response.success = false;
      response.message = message;
      return response;
    }
  }

  private String myApiUrl;

  protected JsonBaseRequestHandler(@Nonnull String apiUrl) {
    myApiUrl = "/api/" + apiUrl;
  }

  @Override
  public boolean isSupported(FullHttpRequest request) {
    return getMethod() == request.method() && myApiUrl.equals(request.uri());
  }

  protected boolean writeResponse(@Nonnull Object responseObject, HttpRequest request, ChannelHandlerContext context) throws IOException {
    FullHttpResponse response = Responses.response("application/json; charset=utf-8", null);

    String jsonResponse = new GsonBuilder().setPrettyPrinting().create().toJson(responseObject);

    response.content().writeBytes(Unpooled.copiedBuffer(jsonResponse, StandardCharsets.UTF_8));

    Responses.send(response, context.channel(), request);
    return true;
  }

  @Nonnull
  public String getApiUrl() {
    return myApiUrl;
  }

  @Nonnull
  protected abstract HttpMethod getMethod();
}
