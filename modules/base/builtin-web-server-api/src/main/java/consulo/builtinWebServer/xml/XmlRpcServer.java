/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.builtinWebServer.xml;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@ServiceAPI(ComponentScope.APPLICATION)
public interface XmlRpcServer {
  static XmlRpcServer getInstance() {
    return Application.get().getInstance(XmlRpcServer.class);
  }

  void addHandler(String name, Object handler);

  boolean hasHandler(String name);

  void removeHandler(String name);

  @Nonnull
  HttpResponse process(@Nonnull String path, @Nonnull HttpRequest request, @Nullable Map<String, Object> handlers);
}