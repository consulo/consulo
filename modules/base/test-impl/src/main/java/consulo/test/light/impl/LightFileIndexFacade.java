/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import consulo.module.Module;
import consulo.project.Project;
import consulo.language.content.FileIndexFacade;
import consulo.component.util.ModificationTracker;
import com.intellij.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightFileIndexFacade extends FileIndexFacade {
  private final List<VirtualFile> myLibraryRoots = new ArrayList<>();

  public LightFileIndexFacade(final Project project) {
    super(project);
  }

  @Nonnull
  @Override
  public ModificationTracker getRootModificationTracker() {
    return ModificationTracker.NEVER_CHANGED;
  }

  @Override
  public boolean isInContent(@Nonnull VirtualFile file) {
    return true;
  }

  @Override
  public boolean isInSource(@Nonnull VirtualFile file) {
    return true;
  }

  @Override
  public boolean isInSourceContent(@Nonnull VirtualFile file) {
    return true;
  }

  @Override
  public boolean isInLibraryClasses(@Nonnull VirtualFile file) {
    for (VirtualFile libraryRoot : myLibraryRoots) {
      if (VfsUtilCore.isAncestor(libraryRoot, file, false)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isInLibrarySource(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public boolean isExcludedFile(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public boolean isUnderIgnored(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public Module getModuleForFile(@Nonnull VirtualFile file) {
    return null;
  }

  @Override
  public boolean isValidAncestor(@Nonnull VirtualFile baseDir, @Nonnull VirtualFile child) {
    return VfsUtilCore.isAncestor(baseDir, child, false);
  }

  public void addLibraryRoot(VirtualFile file) {
    myLibraryRoots.add(file);
  }
}
