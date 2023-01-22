/*
 * Copyright 2013-2023 consulo.io
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
package consulo.component.extension.preview;

import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22/01/2023
 */
public final class ExtensionPreview<T> {
  private final PluginId myApiPluginId;
  private final String myApiClassName;

  private final String myImplId;
  private final PluginId myImplPluginId;

  public ExtensionPreview(@Nonnull Class<T> apiClass, @Nonnull String implId, @Nonnull T implClass) {
    myApiPluginId = PluginManager.getPluginId(apiClass);
    myApiClassName = apiClass.getName();
    myImplId = implId;
    myImplPluginId = PluginManager.getPluginId(implClass.getClass());
  }

  @Nonnull
  public String getApiClassName() {
    return myApiClassName;
  }

  @Nonnull
  public String getImplId() {
    return myImplId;
  }

  public PluginId getImplPluginId() {
    return myImplPluginId;
  }

  @Nonnull
  public PluginId getApiPluginId() {
    return myApiPluginId;
  }
}
