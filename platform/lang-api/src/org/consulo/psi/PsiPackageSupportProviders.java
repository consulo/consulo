/*
 * Copyright 2013-2015 must-be.org
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
package org.consulo.psi;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import org.consulo.module.extension.ModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.RequiredReadAction;

/**
 * @author VISTALL
 * @since 27.04.2015
 */
public class PsiPackageSupportProviders {
  @RequiredReadAction
  public static boolean isPackageSupported(@NotNull Project project) {
    PsiPackageSupportProvider[] extensions = PsiPackageSupportProvider.EP_NAME.getExtensions();
    ModuleManager moduleManager = ModuleManager.getInstance(project);
    for (Module module : moduleManager.getModules()) {
      ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
      for (ModuleExtension moduleExtension : rootManager.getExtensions()) {
        for (PsiPackageSupportProvider extension : extensions) {
          if(extension.isSupported(moduleExtension)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
