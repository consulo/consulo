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
package consulo.module.content.layer.extension;

import consulo.component.util.pointer.NamedPointer;
import consulo.content.bundle.Sdk;
import consulo.module.Module;
import consulo.module.content.internal.ModuleRootLayerEx;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.util.ModuleContentUtil;
import consulo.module.extension.ModuleExtensionWithSdk;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19:35/15.06.13
 */
public class SdkModuleInheritableNamedPointerImpl extends ModuleInheritableNamedPointerImpl<Sdk> {
  private final String myExtensionId;

  public SdkModuleInheritableNamedPointerImpl(@Nonnull ModuleRootLayer layer, @Nonnull String id) {
    super(layer, "sdk");
    myExtensionId = id;
  }

  @Override
  public String getItemNameFromModule(@Nonnull Module module) {
    final ModuleExtensionWithSdk<?> extension = (ModuleExtensionWithSdk)ModuleContentUtil.getExtension(module, myExtensionId);
    if (extension != null) {
      return extension.getInheritableSdk().getName();
    }
    return null;
  }

  @Override
  public Sdk getItemFromModule(@Nonnull Module module) {
    final ModuleExtensionWithSdk<?> extension = (ModuleExtensionWithSdk)ModuleContentUtil.getExtension(module, myExtensionId);
    if (extension != null) {
      return extension.getInheritableSdk().get();
    }
    return null;
  }

  @Nonnull
  @Override
  public NamedPointer<Sdk> getPointer(@Nonnull ModuleRootLayer layer, @Nonnull String name) {
    return ((ModuleRootLayerEx)layer).getConfigurationAccessor().getSdkPointer(name);
  }
}
