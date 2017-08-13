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
package consulo.module.extension.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.Sdk;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.roots.ModuleRootLayer;
import consulo.roots.impl.ModuleRootLayerImpl;
import consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 19:35/15.06.13
 */
public class SdkModuleInheritableNamedPointerImpl extends ModuleInheritableNamedPointerImpl<Sdk> {
  private final String myExtensionId;

  public SdkModuleInheritableNamedPointerImpl(@NotNull ModuleRootLayer layer, @NotNull String id) {
    super(layer, "sdk");
    myExtensionId = id;
  }

  @Override
  public String getItemNameFromModule(@NotNull Module module) {
    final ModuleExtensionWithSdk<?> extension = (ModuleExtensionWithSdk) ModuleUtilCore.getExtension(module, myExtensionId);
    if (extension != null) {
      return extension.getInheritableSdk().getName();
    }
    return null;
  }

  @Override
  public Sdk getItemFromModule(@NotNull Module module) {
    final ModuleExtensionWithSdk<?> extension = (ModuleExtensionWithSdk)  ModuleUtilCore.getExtension(module, myExtensionId);
    if (extension != null) {
      return extension.getInheritableSdk().get();
    }
    return null;
  }

  @NotNull
  @Override
  public NamedPointer<Sdk> getPointer(@NotNull ModuleRootLayer layer, @NotNull String name) {
    return ((ModuleRootLayerImpl)layer).getRootModel().getConfigurationAccessor().getSdkPointer(name);
  }
}
