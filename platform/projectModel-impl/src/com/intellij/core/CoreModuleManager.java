/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.core;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.impl.ModuleEx;
import com.intellij.openapi.module.impl.ModuleManagerImpl;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class CoreModuleManager extends ModuleManagerImpl {
  private final Disposable myParentDisposable;

  public CoreModuleManager(Project project, Disposable parentDisposable) {
    super(project, project.getMessageBus());
    myParentDisposable = parentDisposable;
  }

  @NotNull
  @Override
  protected ModuleEx createModule(@NotNull String name, String dirUrl) {
    return new CoreModule(myParentDisposable, myProject, name, dirUrl);
  }

  @Override
  protected void deliverPendingEvents() {
  }

  public void loadModules() {
    loadModules(myModuleModel);
  }

  protected Disposable getLifetime() {
    return myParentDisposable;
  }
}
