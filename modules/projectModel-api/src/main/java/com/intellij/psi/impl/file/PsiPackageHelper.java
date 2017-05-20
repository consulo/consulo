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

package com.intellij.psi.impl.file;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDirectoryContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public abstract class PsiPackageHelper {
  public static PsiPackageHelper getInstance(Project project) {
    return ServiceManager.getService(project, PsiPackageHelper.class);
  }

  @NotNull
  public abstract String getQualifiedName(@NotNull PsiDirectory directory, final boolean presentable);

  @Nullable
  public abstract PsiDirectoryContainer getDirectoryContainer(@NotNull PsiDirectory directory);

  public abstract boolean isPackage(PsiDirectory directory);
}
