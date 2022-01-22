/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.module.content;

import consulo.annotation.DeprecationInfo;
import consulo.component.messagebus.Topic;
import consulo.module.content.layer.event.ModuleRootLayerListener;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.event.ModuleListener;

@Deprecated
@DeprecationInfo("See fields")
public interface ProjectTopics {
  Topic<ModuleRootListener> PROJECT_ROOTS = ModuleRootListener.TOPIC;
  Topic<ModuleListener> MODULES = ModuleListener.TOPIC;
  Topic<ModuleRootLayerListener> MODULE_LAYERS = ModuleRootLayerListener.TOPIC;
}