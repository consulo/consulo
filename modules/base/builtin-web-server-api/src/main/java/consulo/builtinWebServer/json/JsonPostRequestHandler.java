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

import consulo.application.json.JsonService;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.http.HTTPMethod;
import consulo.logging.Logger;
import consulo.util.lang.ExceptionUtil;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public abstract class JsonPostRequestHandler<Request> extends JsonBaseRequestHandler {
  private static final Logger LOG = Logger.getInstance(JsonPostRequestHandler.class);

  private Class<Request> myRequestClass;

  protected JsonPostRequestHandler(@Nonnull String apiUrl, @Nonnull Class<Request> requestClass) {
    super(apiUrl);
    myRequestClass = requestClass;
  }

  @Nonnull
  @Override
  protected HTTPMethod getMethod() {
    return HTTPMethod.POST;
  }

  @Nonnull
  public abstract JsonResponse handle(@Nonnull Request request);

  @Nonnull
  public Class<Request> getRequestClass() {
    return myRequestClass;
  }

  @Nonnull
  @Override
  public HttpResponse process(@Nonnull HttpRequest request) throws IOException {
    Object handle = null;
    try {
      String json = request.getContentAsString(StandardCharsets.UTF_8);

      Request body = JsonService.getInstance().fromJson(json, myRequestClass);

      handle = handle(body);
    }
    catch (Exception e) {
      LOG.error(e);
      
      handle = JsonResponse.asError(ExceptionUtil.getThrowableText(e));
    }
    return writeResponse(handle, request);
  }
}
