/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.psi.search;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.LinkedHashSet;
import java.util.Set;

public class GlobalSearchScopeUtil {
  @Nonnull
  public static GlobalSearchScope toGlobalSearchScope(@Nonnull final SearchScope scope, @Nonnull Project project) {
    if (scope instanceof GlobalSearchScope) {
      return (GlobalSearchScope)scope;
    }
    return ApplicationManager.getApplication()
            .runReadAction((Computable<GlobalSearchScope>)() -> GlobalSearchScope.filesScope(project, getLocalScopeFiles((LocalSearchScope)scope)));
  }

  @Nonnull
  public static Set<VirtualFile> getLocalScopeFiles(@Nonnull final LocalSearchScope scope) {
    return ApplicationManager.getApplication().runReadAction((Computable<Set<VirtualFile>>)() -> {
      Set<VirtualFile> files = new LinkedHashSet<>();
      for (PsiElement element : scope.getScope()) {
        PsiFile file = element.getContainingFile();
        if (file != null) {
          ContainerUtil.addIfNotNull(files, file.getVirtualFile());
          ContainerUtil.addIfNotNull(files, file.getNavigationElement().getContainingFile().getVirtualFile());
        }
      }
      return files;
    });
  }
}
