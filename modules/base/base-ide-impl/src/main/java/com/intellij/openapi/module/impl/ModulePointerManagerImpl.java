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
package com.intellij.openapi.module.impl;

import com.intellij.ProjectTopics;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.project.ModuleAdapter;
import com.intellij.openapi.project.Project;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposer;
import consulo.util.pointers.NamedPointerImpl;
import consulo.util.pointers.NamedPointerManagerImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author nik
 */
@Singleton
public class ModulePointerManagerImpl extends NamedPointerManagerImpl<Module> implements ModulePointerManager {
  private final Project myProject;

  @Inject
  public ModulePointerManagerImpl(Project project) {
    myProject = project;
    project.getMessageBus().connect().subscribe(ProjectTopics.MODULES, new ModuleAdapter() {
      @Override
      public void beforeModuleRemoved(Project project, Module module) {
        unregisterPointer(module);
      }

      @Override
      public void moduleAdded(Project project, Module module) {
        updatePointers(module);
      }

      @Override
      public void modulesRenamed(Project project, List<Module> modules) {
        for (Module module : modules) {
          updatePointers(module);
        }
      }
    });
  }

  @Override
  protected void registerPointer(final Module value, final NamedPointerImpl<Module> pointer) {
    super.registerPointer(value, pointer);

    Disposer.register(value, () -> unregisterPointer(value));
  }

  @Nullable
  @Override
  @RequiredReadAction
  protected Module findByName(@Nonnull String name) {
    if (!myProject.isModulesReady()) {
      return null;
    }
    return ModuleManager.getInstance(myProject).findModuleByName(name);
  }
}
