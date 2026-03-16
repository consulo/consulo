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
package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.extension.ModuleExtension;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2013-05-20
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class PsiPackageManager {
  
  public static PsiPackageManager getInstance(Project project) {
    return project.getInstance(PsiPackageManager.class);
  }

  public abstract void dropCache(Class<? extends ModuleExtension> extensionClass);

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findPackage(String qualifiedName, Class<? extends ModuleExtension> extensionClass);

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findPackage(PsiDirectory directory, Class<? extends ModuleExtension> extensionClass);

  @Nullable
  @RequiredReadAction
  public PsiPackage findAnyPackage(PsiDirectory directory) {
    return findAnyPackage(directory.getVirtualFile());
  }

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findAnyPackage(VirtualFile directory);

  @Nullable
  @RequiredReadAction
  public abstract PsiPackage findAnyPackage(String packageName);

  @RequiredReadAction
  public boolean isValidPackageName(PsiDirectory directory, String packageName) {
    Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(directory);
    return moduleForPsiElement == null || isValidPackageName(moduleForPsiElement, packageName);
  }

  @RequiredReadAction
  public abstract boolean isValidPackageName(Module module, String packageName);
}
