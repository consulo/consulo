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

import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 27.10.2015
 * <p/>
 * draft
 */
public abstract class JsonPostRequestHandler<Request extends JsonPostRequestHandler.BaseRequest> extends JsonBaseRequestHandler {
  public static class BaseRequest {

  }

  private Class<Request> myRequestClass;

  protected JsonPostRequestHandler(String apiUrl, Class<Request> requestClass) {
    super(apiUrl);
    myRequestClass = requestClass;
  }

  @NotNull
  @Override
  protected HttpMethod getMethod() {
    return HttpMethod.POST;
  }
}
