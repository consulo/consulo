/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.roots;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
public abstract class FileIndexFacade {
  protected final Project myProject;

  protected FileIndexFacade(final Project project) {
    myProject = project;
  }

  public static FileIndexFacade getInstance(Project project) {
    return ServiceManager.getService(project, FileIndexFacade.class);
  }

  @Nonnull
  public abstract ModificationTracker getRootModificationTracker();

  public abstract boolean isInContent(@Nonnull VirtualFile file);
  public abstract boolean isInSource(@Nonnull VirtualFile file);
  public abstract boolean isInSourceContent(@Nonnull VirtualFile file);
  public abstract boolean isInLibraryClasses(@Nonnull VirtualFile file);

  public abstract boolean isInLibrarySource(@Nonnull VirtualFile file);

  public abstract boolean isExcludedFile(@Nonnull VirtualFile file);
  public abstract boolean isUnderIgnored(@Nonnull VirtualFile file);

  @Nullable
  public abstract Module getModuleForFile(@Nonnull VirtualFile file);

  /**
   * Checks if <code>file</code> is an ancestor of <code>baseDir</code> and none of the files
   * between them are excluded from the project.
   *
   * @param baseDir the parent directory to check for ancestry.
   * @param child the child directory or file to check for ancestry.
   * @return true if it's a valid ancestor, false otherwise.
   */
  public abstract boolean isValidAncestor(@Nonnull VirtualFile baseDir, @Nonnull VirtualFile child);

  public boolean shouldBeFound(GlobalSearchScope scope, VirtualFile virtualFile) {
    return (scope.isSearchOutsideRootModel() || isInContent(virtualFile) || isInLibrarySource(virtualFile)) && !virtualFile.getFileType().isBinary();
  }
}
