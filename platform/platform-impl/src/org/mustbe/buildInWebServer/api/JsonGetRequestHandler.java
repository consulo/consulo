/*
 * Copyright 2013-2015 must-be.org
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
package org.mustbe.buildInWebServer.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.consulo.lombok.annotations.LazyInstance;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.Responses;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public abstract class JsonGetRequestHandler extends JsonBaseRequestHandler {
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

    public static JsonResponse asError(@NotNull String message) {
      JsonResponse response = new JsonResponse();
      response.success = false;
      response.message = message;
      return response;
    }
  }

  protected JsonGetRequestHandler(@NotNull String apiUrl) {
    super(apiUrl);
  }

  @Nullable
  public abstract JsonResponse handle();

  @NotNull
  @Override
  protected HttpMethod getMethod() {
    return HttpMethod.GET;
  }

  @Override
  public boolean process(QueryStringDecoder urlDecoder, HttpRequest request, ChannelHandlerContext context) throws IOException {
    Object handle = handle();
    if (handle == null) {
      return false;
    }

    HttpResponse response = Responses.create("application/json; charset=utf-8");

    String jsonResponse = buildGson().toJson(handle);

    response.setContent(ChannelBuffers.copiedBuffer(jsonResponse, CharsetToolkit.UTF8_CHARSET));

    Responses.send(response, request, context);
    return true;
  }

  @NotNull
  @LazyInstance
  private Gson buildGson() {
    return new GsonBuilder().setPrettyPrinting().create();
  }
}
