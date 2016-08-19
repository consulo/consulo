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

/**
 * @author cdr
 */
package com.intellij.openapi.module;

import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import consulo.module.extension.ModuleExtension;
import org.jetbrains.annotations.NotNull;

public class ModuleUtil extends ModuleUtilCore {
  private ModuleUtil() {
  }

  public static boolean hasModuleExtension(@NotNull ModulesProvider modulesProvider, @NotNull Class<? extends ModuleExtension> clazz) {
    for (Module module : modulesProvider.getModules()) {
      ModuleRootModel rootModel = modulesProvider.getRootModel(module);
      if (rootModel == null) {
        continue;
      }

      ModuleExtension extension = rootModel.getExtension(clazz);
      if (extension != null) {
        return true;
      }
    }
    return false;
  }
}
