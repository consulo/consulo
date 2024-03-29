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

import consulo.annotation.access.RequiredReadAction;
import consulo.component.persist.PersistentStateComponent;
import consulo.module.Module;
import consulo.project.Project;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22:39/18.05.13
 */
public interface ModuleExtension<T extends ModuleExtension> extends PersistentStateComponent<Element> {
  ModuleExtension[] EMPTY_ARRAY = new ModuleExtension[0];

  @Nonnull
  String getId();

  boolean isEnabled();

  @Nonnull
  Module getModule();

  @Nonnull
  default Project getProject() {
    return getModule().getProject();
  }

  @RequiredReadAction
  void commit(@Nonnull T mutableModuleExtension);
}
