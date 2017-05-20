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
package com.intellij.psi.impl.cache;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.IndexPattern;
import com.intellij.psi.search.IndexPatternProvider;
import org.jetbrains.annotations.NotNull;

public abstract class TodoCacheManager {
  @NotNull
  public static TodoCacheManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, TodoCacheManager.class);
  }

  /**
   * @return all files that contains todoitems under project
   */
  @NotNull
  public abstract PsiFile[] getFilesWithTodoItems();

  /**
   * @return -1 if it's not known
   */
  public abstract int getTodoCount(@NotNull VirtualFile file, IndexPatternProvider patternProvider);

  /**
   * @return -1 if it's not known
   */
  public abstract int getTodoCount(@NotNull VirtualFile file, IndexPattern pattern);
}
