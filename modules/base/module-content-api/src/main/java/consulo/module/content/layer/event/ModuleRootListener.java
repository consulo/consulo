/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import java.util.EventListener;

/**
 * @author dsl
 */
@TopicAPI(ComponentScope.PROJECT)
public interface ModuleRootListener extends EventListener {

  default void beforeRootsChange(ModuleRootEvent event) {
  }

  default void rootsChanged(ModuleRootEvent event) {
  }
}
