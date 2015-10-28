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
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.ide.HttpRequestHandler;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public abstract class JsonBaseRequestHandler
        extends HttpRequestHandler {

  private String myApiUrl;

  protected JsonBaseRequestHandler(@NotNull String apiUrl) {
    myApiUrl = "/api/" + apiUrl;
  }

  @Override
  public boolean isSupported(HttpRequest request) {
    return getMethod() == request.getMethod() && myApiUrl.equals(request.getUri());
  }

  @NotNull
  protected abstract HttpMethod getMethod();
}
