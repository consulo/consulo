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
package consulo.language.psi.stub.todo;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.search.IndexPatternProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class TodoCacheManager {
  @Nonnull
  public static TodoCacheManager getInstance(@Nonnull Project project) {
    return project.getInstance(TodoCacheManager.class);
  }

  /**
   * @return all files that contains todoitems under project
   */
  @Nonnull
  public abstract PsiFile[] getFilesWithTodoItems();

  /**
   * @return -1 if it's not known
   */
  public abstract int getTodoCount(@Nonnull VirtualFile file, IndexPatternProvider patternProvider);

  /**
   * @return -1 if it's not known
   */
  public abstract int getTodoCount(@Nonnull VirtualFile file, IndexPattern pattern);
}
