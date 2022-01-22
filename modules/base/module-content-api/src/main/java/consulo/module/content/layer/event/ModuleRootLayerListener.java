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

import consulo.component.messagebus.Topic;
import consulo.module.Module;
import consulo.module.content.layer.ModuleRootLayer;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 30.07.14
 */
public interface ModuleRootLayerListener {
  Topic<ModuleRootLayerListener> TOPIC = Topic.create("module layers added or removed or changed current from module", ModuleRootLayerListener.class);

  class Adapter implements ModuleRootLayerListener {
    @Override
    public void layerRemove(@Nonnull Module module, @Nonnull ModuleRootLayer removed) {

    }

    @Override
    public void layerAdded(@Nonnull Module module, @Nonnull ModuleRootLayer removed) {

    }

    @Override
    public void layerChanged(@Nonnull Module module, @Nonnull ModuleRootLayer added) {

    }

    @Override
    public void currentLayerChanged(@Nonnull Module module, @Nonnull String oldName, @Nonnull ModuleRootLayer oldLayer, @Nonnull String newName, @Nonnull ModuleRootLayer newLayer) {

    }
  }

  void layerRemove(@Nonnull Module module, @Nonnull ModuleRootLayer removed);

  void layerAdded(@Nonnull Module module, @Nonnull ModuleRootLayer added);

  void layerChanged(@Nonnull Module module, @Nonnull ModuleRootLayer added);

  void currentLayerChanged(@Nonnull Module module, @Nonnull String oldName, @Nonnull ModuleRootLayer oldLayer, @Nonnull String newName, @Nonnull ModuleRootLayer newLayer);
}
