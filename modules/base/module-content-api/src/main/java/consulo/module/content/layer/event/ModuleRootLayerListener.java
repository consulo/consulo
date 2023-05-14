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
package consulo.module.content.layer.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.module.Module;
import consulo.module.content.layer.ModuleRootLayer;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 30.07.14
 */
@TopicAPI(ComponentScope.PROJECT)
public interface ModuleRootLayerListener {
  default void layerRemove(@Nonnull Module module, @Nonnull ModuleRootLayer removed) {
  }

  default void layerAdded(@Nonnull Module module, @Nonnull ModuleRootLayer added) {
  }

  default void layerChanged(@Nonnull Module module, @Nonnull ModuleRootLayer added) {
  }

  default void currentLayerChanged(@Nonnull Module module, @Nonnull String oldName, @Nonnull ModuleRootLayer oldLayer, @Nonnull String newName, @Nonnull ModuleRootLayer newLayer) {
  }
}
