/*
 * Copyright 2013 must-be.org
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
package consulo.psi;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import consulo.module.extension.ModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredReadAction;

/**
 * @author VISTALL
 * @since 7:56/20.05.13
 */
public abstract class PsiPackageManager {
  @NotNull
  public static PsiPackageManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, PsiPackageManager.class);
  }

  public abstract void dropCache(@NotNull Class<? extends ModuleExtension> extensionClass);

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findPackage(@NotNull String qualifiedName, @NotNull Class<? extends ModuleExtension> extensionClass);

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findPackage(@NotNull PsiDirectory directory, @NotNull Class<? extends ModuleExtension> extensionClass);

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findAnyPackage(@NotNull PsiDirectory directory);

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findAnyPackage(@NotNull String packageName);

  @RequiredReadAction
  public boolean isValidPackageName(@NotNull PsiDirectory directory, @NotNull String packageName) {
    Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(directory);
    return moduleForPsiElement == null || isValidPackageName(moduleForPsiElement, packageName);
  }

  @RequiredReadAction
  public abstract boolean isValidPackageName(@NotNull Module module, @NotNull String packageName);
}
