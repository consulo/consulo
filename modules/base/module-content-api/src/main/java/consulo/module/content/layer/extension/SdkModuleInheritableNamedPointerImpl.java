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
import consulo.module.extension.ModuleExtensionWithSdk;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2013-06-15
 */
public class SdkModuleInheritableNamedPointerImpl extends ModuleInheritableNamedPointerImpl<Sdk> {
  private final String myExtensionId;

  public SdkModuleInheritableNamedPointerImpl(ModuleRootLayer layer, String id) {
    super(layer, "sdk");
    myExtensionId = id;
  }

  @Override
  public @Nullable String getItemNameFromModule(Module module) {
    ModuleExtensionWithSdk<?> extension = (ModuleExtensionWithSdk)module.getExtension(myExtensionId);
    if (extension != null) {
      return extension.getInheritableSdk().getName();
    }
    return null;
  }

  @Override
  public @Nullable Sdk getItemFromModule(Module module) {
    ModuleExtensionWithSdk<?> extension = (ModuleExtensionWithSdk)module.getExtension(myExtensionId);
    if (extension != null) {
      return extension.getInheritableSdk().get();
    }
    return null;
  }

  @Override
  public NamedPointer<Sdk> getPointer(ModuleRootLayer layer, String name) {
    return ((ModuleRootLayerEx)layer).getConfigurationAccessor().getSdkPointer(name);
  }
}
