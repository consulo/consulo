/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.openapi.roots;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 30.07.14
 */
public interface ModuleRootLayerListener {
  class Adapter implements ModuleRootLayerListener {
    @Override
    public void layerRemove(@NotNull Module module, @NotNull ModuleRootLayer removed) {

    }

    @Override
    public void layerAdded(@NotNull Module module, @NotNull ModuleRootLayer removed) {

    }

    @Override
    public void layerChanged(@NotNull Module module, @NotNull ModuleRootLayer added) {

    }

    @Override
    public void currentLayerChanged(@NotNull Module module,
                                    @NotNull String oldName,
                                    @NotNull ModuleRootLayer oldLayer,
                                    @NotNull String newName,
                                    @NotNull ModuleRootLayer newLayer) {

    }
  }

  void layerRemove(@NotNull Module module, @NotNull ModuleRootLayer removed);

  void layerAdded(@NotNull Module module, @NotNull ModuleRootLayer added);

  void layerChanged(@NotNull Module module, @NotNull ModuleRootLayer added);

  void currentLayerChanged(@NotNull Module module, @NotNull String oldName, @NotNull ModuleRootLayer oldLayer,
                           @NotNull String newName, @NotNull ModuleRootLayer newLayer);
}
