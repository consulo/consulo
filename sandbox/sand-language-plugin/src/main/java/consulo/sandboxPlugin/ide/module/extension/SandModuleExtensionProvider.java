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
package consulo.sandboxPlugin.ide.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 24-Jun-22
 */
@ExtensionImpl
public class SandModuleExtensionProvider implements ModuleExtensionProvider<SandModuleExtension> {
  @Nonnull
  @Override
  public String getId() {
    return "sand";
  }

  @Nonnull
  @Override
  public LocalizeValue getName() {
    return LocalizeValue.localizeTODO("Sand");
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return PlatformIconGroup.nodesStatic();
  }

  @Nonnull
  @Override
  public ModuleExtension<SandModuleExtension> createImmutableExtension(@Nonnull ModuleRootLayer layer) {
    return new SandModuleExtension(getId(), layer);
  }

  @Nonnull
  @Override
  public MutableModuleExtension<SandModuleExtension> createMutableExtension(@Nonnull ModuleRootLayer layer) {
    return new SandMutableModuleExtension(getId(), layer);
  }
}
