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
package consulo.buildInWebServer.api;

import com.google.gson.Gson;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.ExceptionUtil;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public abstract class JsonPostRequestHandler<Request> extends JsonBaseRequestHandler {
  private Class<Request> myRequestClass;

  protected JsonPostRequestHandler(@NotNull String apiUrl, @NotNull Class<Request> requestClass) {
    super(apiUrl);
    myRequestClass = requestClass;
  }

  @NotNull
  @Override
  protected HttpMethod getMethod() {
    return HttpMethod.POST;
  }

  @NotNull
  public abstract JsonResponse handle(@NotNull Request request);

  @Override
  public boolean process(QueryStringDecoder urlDecoder, HttpRequest request, ChannelHandlerContext context) throws IOException {
    Object handle = null;
    try {
      String json = request.getContent().toString(CharsetToolkit.UTF8_CHARSET);

      final Request body = new Gson().fromJson(json, myRequestClass);

      handle = handle(body);
    }
    catch (Exception e) {
      handle = JsonResponse.asError(ExceptionUtil.getThrowableText(e));
    }
    return writeResponse(handle, request, context);
  }
}
