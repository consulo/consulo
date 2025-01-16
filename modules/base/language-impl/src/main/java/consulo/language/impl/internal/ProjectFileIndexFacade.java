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

package consulo.language.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.module.Module;
import consulo.module.content.DirectoryIndex;
import consulo.project.Project;
import consulo.language.content.FileIndexFacade;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.component.util.ModificationTracker;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class ProjectFileIndexFacade extends FileIndexFacade {
  private final ProjectFileIndex myFileIndex;
  private final Provider<DirectoryIndex> myDirectoryIndexProvider;

  @Inject
  public ProjectFileIndexFacade(final Project project, final ProjectRootManager rootManager, final Provider<DirectoryIndex> directoryIndex) {
    super(project);
    myDirectoryIndexProvider = directoryIndex;
    myFileIndex = rootManager.getFileIndex();
  }

  @Override
  public boolean isInContent(@Nonnull final VirtualFile file) {
    return myFileIndex.isInContent(file);
  }

  @Override
  public boolean isInSource(@Nonnull VirtualFile file) {
    return myFileIndex.isInSource(file);
  }

  @Override
  public boolean isInSourceContent(@Nonnull VirtualFile file) {
    return myFileIndex.isInSourceContent(file);
  }

  @Override
  public boolean isInLibraryClasses(@Nonnull VirtualFile file) {
    return myFileIndex.isInLibraryClasses(file);
  }

  @Override
  public boolean isInLibrarySource(@Nonnull VirtualFile file) {
    return myFileIndex.isInLibrarySource(file);
  }

  @Override
  public boolean isExcludedFile(@Nonnull final VirtualFile file) {
    return myFileIndex.isExcluded(file);
  }

  @Override
  public boolean isUnderIgnored(@Nonnull VirtualFile file) {
    return myFileIndex.isUnderIgnored(file);
  }

  @Nullable
  @Override
  public Module getModuleForFile(@Nonnull VirtualFile file) {
    return myFileIndex.getModuleForFile(file);
  }

  @Nonnull
  @Override
  public ModificationTracker getRootModificationTracker() {
    return ProjectRootManager.getInstance(myProject);
  }

  @Override
  public boolean isValidAncestor(@Nonnull final VirtualFile baseDir, @Nonnull VirtualFile childDir) {
    if (!childDir.isDirectory()) {
      childDir = childDir.getParent();
    }
    DirectoryIndex dirIndex = myDirectoryIndexProvider.get();
    while (true) {
      if (childDir == null) return false;
      if (childDir.equals(baseDir)) return true;
      if (!dirIndex.getInfoForFile(childDir).isInProject(childDir)) return false;
      childDir = childDir.getParent();
    }
  }
}
