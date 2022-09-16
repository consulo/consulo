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
package consulo.builtinWebServer.json;

import consulo.annotation.DeprecationInfo;
import consulo.application.json.JsonService;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.http.HTTPMethod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public abstract class JsonBaseRequestHandler extends HttpRequestHandler {
  public static final class JsonResponse {
    public boolean success;
    public String message;
    public Object data;

    @Deprecated
    @DeprecationInfo("don't use it, used for serialize")
    public JsonResponse() {
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
  public boolean isSupported(HttpRequest request) {
    return getMethod() == request.method() && myApiUrl.equals(request.uri());
  }

  @Nonnull
  protected HttpResponse writeResponse(@Nonnull Object responseObject, @Nonnull HttpRequest request) throws IOException {
    String jsonResponse = JsonService.getInstance().toJson(responseObject);

    return HttpResponse.ok("application/json; charset=utf-8", jsonResponse.getBytes(StandardCharsets.UTF_8));
  }

  @Nonnull
  public String getApiUrl() {
    return myApiUrl;
  }

  @Nonnull
  protected abstract HTTPMethod getMethod();
}
