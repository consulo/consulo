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
package consulo.module.impl.internal.layer.library;

import consulo.component.ComponentManager;
import consulo.content.impl.internal.library.LibraryOwner;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;
import consulo.virtualFileSystem.pointer.VirtualFilePointerListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 09-Apr-22
 */
public class ModuleRootLayerLibraryOwner implements LibraryOwner {
  private final ModuleRootLayerImpl myModuleRootLayer;

  public ModuleRootLayerLibraryOwner(ModuleRootLayerImpl moduleRootLayer) {
    myModuleRootLayer = moduleRootLayer;
  }

  @Nonnull
  @Override
  public VirtualFilePointerListener getListener() {
    return myModuleRootLayer.getRootsChangedListener();
  }

  @Nullable
  @Override
  public ComponentManager getModule() {
    return myModuleRootLayer.getModule();
  }
}
