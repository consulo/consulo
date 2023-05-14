/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.builtinWebServer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.util.io.Url;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.URLConnection;

@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
public abstract class BuiltInServerManager {
  @Nonnull
  public static BuiltInServerManager getInstance() {
    return Application.get().getInstance(BuiltInServerManager.class);
  }

  public abstract int getPort();

  public abstract BuiltInServerManager waitForStart();

  @Nullable
  public abstract Disposable getServerDisposable();

  public abstract boolean isOnBuiltInWebServer(@Nullable Url url);

  public abstract void configureRequestToWebServer(@Nonnull URLConnection connection);

  public abstract Url addAuthToken(@Nonnull Url url);

  public abstract boolean isLocalHost(String host, boolean onlyAnyOrLoopback, boolean hostsOnly);
}