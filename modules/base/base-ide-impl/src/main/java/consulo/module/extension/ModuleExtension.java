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
package consulo.module.extension;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import consulo.annotation.access.RequiredReadAction;
import consulo.roots.ModuleRootLayer;
import com.intellij.util.messages.Topic;
import org.jdom.Element;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22:39/18.05.13
 */
public interface ModuleExtension<T extends ModuleExtension> extends PersistentStateComponent<Element> {
  ModuleExtension[] EMPTY_ARRAY = new ModuleExtension[0];

  Topic<ModuleExtensionChangeListener> CHANGE_TOPIC = Topic.create("module extension change topic", ModuleExtensionChangeListener.class);

  @Nonnull
  String getId();

  boolean isEnabled();

  @Nonnull
  Module getModule();

  @Nonnull
  ModuleRootLayer getModuleRootLayer();

  @Nonnull
  Project getProject();

  @RequiredReadAction
  void commit(@Nonnull T mutableModuleExtension);
}
