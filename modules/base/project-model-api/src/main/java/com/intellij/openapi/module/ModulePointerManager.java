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
package com.intellij.openapi.module;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import consulo.annotation.access.RequiredReadAction;
import consulo.util.pointers.NamedPointer;
import consulo.util.pointers.NamedPointerManager;
import javax.annotation.Nonnull;

public interface ModulePointerManager extends NamedPointerManager<Module> {
  @Nonnull
  static ModulePointerManager getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, ModulePointerManager.class);
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
