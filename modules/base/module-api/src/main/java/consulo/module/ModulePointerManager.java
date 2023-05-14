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
package consulo.module;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.util.pointer.NamedPointer;
import consulo.component.util.pointer.NamedPointerManager;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

@ServiceAPI(ComponentScope.PROJECT)
public interface ModulePointerManager extends NamedPointerManager<Module> {
  @Nonnull
  static ModulePointerManager getInstance(@Nonnull Project project) {
    return project.getInstance(ModulePointerManager.class);
  }

  @Nonnull
  @Override
  @RequiredReadAction
  NamedPointer<Module> create(@Nonnull String name);

  @Nonnull
  @Override
  @RequiredReadAction
  NamedPointer<Module> create(@Nonnull Module value);
}
