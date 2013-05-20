/*
 * Copyright 2013 Consulo.org
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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 7:56/20.05.13
 */
public abstract class PsiPackageManager {
  public static PsiPackageManager getInstance(@NotNull Module module) {
    return ModuleServiceManager.getService(module, PsiPackageManager.class);
  }

  public static PsiPackageManager getInstance(@NotNull PsiElement element) {
    final Module moduleForPsiElement = ModuleUtil.findModuleForPsiElement(element);
    return moduleForPsiElement == null ? ServiceManager.getService(PsiPackageManager.class) : getInstance(moduleForPsiElement);
  }

  public abstract void dropCache();

  @Nullable
  public abstract PsiPackage findPackage(@NotNull String qualifiedName);

  @Nullable
  public abstract PsiPackage findPackage(@NotNull PsiDirectory directory);
}
