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

import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.http.HTTPMethod;
import consulo.logging.Logger;
import consulo.util.lang.ExceptionUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public abstract class JsonGetRequestHandler extends JsonBaseRequestHandler {
  private static final Logger LOG = Logger.getInstance(JsonGetRequestHandler.class);

  protected JsonGetRequestHandler(@Nonnull String apiUrl) {
    super(apiUrl);
  }

  @Nonnull
  public abstract JsonResponse handle(@Nullable HttpRequest request);

  @Nonnull
  @Override
  protected HTTPMethod getMethod() {
    return HTTPMethod.GET;
  }

  @Nonnull
  @Override
  public HttpResponse process(@Nullable HttpRequest request) throws IOException {
    Object handle;
    try {
      handle = handle(request);
    }
    catch (Exception e) {
      LOG.error(e);
      
      handle = JsonResponse.asError(ExceptionUtil.getThrowableText(e));
    }
    return writeResponse(handle, request);
  }
}
