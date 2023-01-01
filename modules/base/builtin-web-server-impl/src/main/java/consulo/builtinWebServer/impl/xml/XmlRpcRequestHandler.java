/*
 * Copyright 2013-2022 consulo.io
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
package consulo.builtinWebServer.impl.xml;

import consulo.annotation.component.ExtensionImpl;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.xml.XmlRpcServer;
import consulo.http.HTTPMethod;

import javax.annotation.Nonnull;
import java.io.IOException;

@ExtensionImpl
public final class XmlRpcRequestHandler extends HttpRequestHandler {
  @Override
  public boolean isSupported(@Nonnull HttpRequest request) {
    return request.method() == HTTPMethod.POST || request.method() == HTTPMethod.OPTIONS;
  }

  @Nonnull
  @Override
  public HttpResponse process(@Nonnull HttpRequest request) throws IOException {
    return XmlRpcServer.getInstance().process(request.path(), request, null);
  }
}
