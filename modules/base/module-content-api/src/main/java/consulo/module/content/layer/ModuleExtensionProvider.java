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
package consulo.module.content.layer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.localize.LocalizeValue;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 24-Jun-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ModuleExtensionProvider<T extends ModuleExtension<T>> {
  ExtensionPointCacheKey<ModuleExtensionProvider, Map<String, ModuleExtensionProvider>> BY_ID = ExtensionPointCacheKey.groupBy("ByKeyModuleExtensionProvider", ModuleExtensionProvider::getId);

  @Nullable
  static ModuleExtensionProvider findProvider(@Nonnull String id) {
    ExtensionPoint<ModuleExtensionProvider> point = Application.get().getExtensionPoint(ModuleExtensionProvider.class);
    Map<String, ModuleExtensionProvider> map = point.getOrBuildCache(BY_ID);
    return map.get(id);
  }

  @Nonnull
  String getId();

  @Nullable
  default String getParentId() {
    return null;
  }

  default boolean isAllowMixin() {
    return false;
  }

  default boolean isSystemOnly() {
    return false;
  }

  @Nonnull
  LocalizeValue getName();

  @Nonnull
  Image getIcon();

  @Nonnull
  ModuleExtension<T> createImmutableExtension(@Nonnull ModuleRootLayer layer);

  @Nonnull
  MutableModuleExtension<T> createMutableExtension(@Nonnull ModuleRootLayer layer);
}
