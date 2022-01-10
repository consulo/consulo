/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.roots.impl;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.util.ObjectUtil;
import consulo.application.AccessRule;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public abstract class FileIndexBase implements FileIndex {
  protected final FileTypeManager myFileTypeManager;
  protected final Provider<DirectoryIndex> myDirectoryIndexProvider;
  private final VirtualFileFilter myContentFilter = file -> {
    assert file != null;
    return ObjectUtil.assertNotNull(AccessRule.<Boolean, RuntimeException>read(() -> !isScopeDisposed() && isInContent(file)));
  };

  public FileIndexBase(@Nonnull Provider<DirectoryIndex> directoryIndexProvider, @Nonnull FileTypeManager fileTypeManager) {
    myDirectoryIndexProvider = directoryIndexProvider;
    myFileTypeManager = fileTypeManager;
  }

  protected abstract boolean isScopeDisposed();

  @Override
  public boolean iterateContent(@Nonnull ContentIterator processor) {
    return iterateContent(processor, null);
  }

  @Override
  public boolean iterateContentUnderDirectory(@Nonnull VirtualFile dir, @Nonnull ContentIterator processor, @Nullable VirtualFileFilter customFilter) {
    VirtualFileFilter filter = customFilter != null ? file -> myContentFilter.accept(file) && customFilter.accept(file) : myContentFilter;
    return iterateContentUnderDirectoryWithFilter(dir, processor, filter);
  }

  @Override
  public boolean iterateContentUnderDirectory(@Nonnull VirtualFile dir, @Nonnull ContentIterator processor) {
    return iterateContentUnderDirectory(dir, processor, null);
  }

  private static boolean iterateContentUnderDirectoryWithFilter(@Nonnull VirtualFile dir, @Nonnull ContentIterator iterator, @Nonnull VirtualFileFilter filter) {
    return VfsUtilCore.iterateChildrenRecursively(dir, filter, iterator);
  }

  @Nonnull
  public DirectoryInfo getInfoForFileOrDirectory(@Nonnull VirtualFile file) {
    if (file instanceof VirtualFileWindow) {
      file = ((VirtualFileWindow)file).getDelegate();
    }
    return myDirectoryIndexProvider.get().getInfoForFile(file);
  }

  @Override
  public boolean isContentSourceFile(@Nonnull VirtualFile file) {
    return !file.isDirectory() && !myFileTypeManager.isFileIgnored(file) && isInSourceContent(file);
  }

  @Nonnull
  protected static VirtualFile[][] getModuleContentAndSourceRoots(Module module) {
    return new VirtualFile[][]{ModuleRootManager.getInstance(module).getContentRoots(), ModuleRootManager.getInstance(module).getSourceRoots()};
  }
}
