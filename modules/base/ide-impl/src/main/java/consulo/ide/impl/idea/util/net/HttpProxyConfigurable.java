/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.net;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.IdeaConfigurableBase;
import consulo.configurable.StandardConfigurableIds;
import consulo.http.HttpProxyManager;
import consulo.http.impl.internal.proxy.HttpProxyManagerImpl;
import consulo.localize.LocalizeValue;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public class HttpProxyConfigurable extends IdeaConfigurableBase<HttpProxySettingsUi, HttpProxyManagerImpl> implements ApplicationConfigurable {
  private final HttpProxyManagerImpl settings;

  public HttpProxyConfigurable() {
    this(HttpProxyManager.getInstance());
  }

  @Inject
  public HttpProxyConfigurable(@Nonnull HttpProxyManager settings) {
    super("http.proxy", LocalizeValue.localizeTODO("HTTP Proxy"), "http.proxy");

    this.settings = (HttpProxyManagerImpl)settings;
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.GENERAL_GROUP;
  }

  @Nonnull
  @Override
  protected HttpProxyManagerImpl getSettings() {
    return settings;
  }

  @Override
  protected HttpProxySettingsUi createUi() {
    return new HttpProxySettingsUi(settings);
  }
}