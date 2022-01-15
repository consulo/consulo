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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.RootModelProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ModulesProvider extends RootModelProvider {
  ModulesProvider EMPTY_MODULES_PROVIDER = new ModulesProvider() {
    @Override
    @Nonnull
    public Module[] getModules() {
      return Module.EMPTY_ARRAY;
    }
    @Override
    public Module getModule(String name) {
      return null;
    }

    @Override
    public ModuleRootModel getRootModel(@Nonnull Module module) {
      return ModuleRootManager.getInstance(module);
    }
  };

  @Nullable
  Module getModule(String name);
}
