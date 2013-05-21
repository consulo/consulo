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
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import org.consulo.module.extension.ModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  public abstract PsiPackage findPackage(@NotNull String qualifiedName, @NotNull Class<? extends ModuleExtension> extensionClass);

  @Nullable
  public abstract PsiPackage findPackage(@NotNull PsiDirectory directory, @NotNull Class<? extends ModuleExtension> extensionClass);

  public abstract boolean findAnyPackage(@NotNull PsiDirectory directory);

  @NotNull
  public abstract Project getProject();
}
