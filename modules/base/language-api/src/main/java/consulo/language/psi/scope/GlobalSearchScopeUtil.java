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
package consulo.language.psi.scope;

import consulo.application.ApplicationManager;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public class GlobalSearchScopeUtil {
  @Nonnull
  public static GlobalSearchScope toGlobalSearchScope(@Nonnull SearchScope scope, @Nonnull Project project) {
    if (scope instanceof GlobalSearchScope) {
      return (GlobalSearchScope)scope;
    }
    return ApplicationManager.getApplication()
                             .runReadAction((Supplier<GlobalSearchScope>)() -> GlobalSearchScope.filesScope(project,
                                                                                                            getLocalScopeFiles((LocalSearchScope)scope)));
  }

  @Nonnull
  public static Set<VirtualFile> getLocalScopeFiles(@Nonnull LocalSearchScope scope) {
    return ApplicationManager.getApplication().runReadAction((Supplier<Set<VirtualFile>>)() -> {
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
