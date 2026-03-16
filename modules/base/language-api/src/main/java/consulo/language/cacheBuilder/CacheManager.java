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

package consulo.language.cacheBuilder;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.function.Predicate;

@ServiceAPI(ComponentScope.PROJECT)
public interface CacheManager {
  
  @Deprecated
  static CacheManager getInstance(Project project) {
    return project.getInstance(CacheManager.class);
  }

  
  PsiFile[] getFilesWithWord(String word, short occurenceMask, GlobalSearchScope scope, boolean caseSensitively);

  
  VirtualFile[] getVirtualFilesWithWord(String word,
                                        short occurenceMask,
                                        GlobalSearchScope scope,
                                        boolean caseSensitively);

  boolean processFilesWithWord(Predicate<PsiFile> processor,
                               String word,
                               short occurenceMask,
                               GlobalSearchScope scope,
                               boolean caseSensitively);
}

