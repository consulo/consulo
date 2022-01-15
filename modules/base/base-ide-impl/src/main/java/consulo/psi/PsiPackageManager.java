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
package consulo.psi;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import consulo.annotation.access.RequiredReadAction;
import consulo.module.extension.ModuleExtension;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 7:56/20.05.13
 */
public abstract class PsiPackageManager {
  @Nonnull
  public static PsiPackageManager getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, PsiPackageManager.class);
  }

  public abstract void dropCache(@Nonnull Class<? extends ModuleExtension> extensionClass);

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findPackage(@Nonnull String qualifiedName, @Nonnull Class<? extends ModuleExtension> extensionClass);

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findPackage(@Nonnull PsiDirectory directory, @Nonnull Class<? extends ModuleExtension> extensionClass);

  @Nullable
  @RequiredReadAction
  public PsiPackage findAnyPackage(@Nonnull PsiDirectory directory) {
    return findAnyPackage(directory.getVirtualFile());
  }

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findAnyPackage(@Nonnull VirtualFile directory);

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findAnyPackage(@Nonnull String packageName);

  @RequiredReadAction
  public boolean isValidPackageName(@Nonnull PsiDirectory directory, @Nonnull String packageName) {
    Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(directory);
    return moduleForPsiElement == null || isValidPackageName(moduleForPsiElement, packageName);
  }

  @RequiredReadAction
  public abstract boolean isValidPackageName(@Nonnull Module module, @Nonnull String packageName);
}
