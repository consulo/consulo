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
package consulo.module.content.internal;

import consulo.application.AccessRule;
import consulo.content.ContentIterator;
import consulo.content.FileIndex;
import consulo.module.Module;
import consulo.module.content.DirectoryIndex;
import consulo.module.content.DirectoryInfo;
import consulo.module.content.ModuleRootManager;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileDelegate;
import consulo.virtualFileSystem.VirtualFileFilter;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;

/**
 * @author nik
 */
public abstract class FileIndexBase implements FileIndex {
  protected final FileTypeRegistry myFileTypeManager;
  protected final Provider<DirectoryIndex> myDirectoryIndexProvider;
  private final VirtualFileFilter myContentFilter = file -> {
    assert file != null;
    return ObjectUtil.assertNotNull(AccessRule.<Boolean, RuntimeException>read(() -> !isScopeDisposed() && isInContent(file)));
  };

  public FileIndexBase(@Nonnull Provider<DirectoryIndex> directoryIndexProvider, @Nonnull FileTypeRegistry fileTypeManager) {
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
    return VirtualFileUtil.iterateChildrenRecursively(dir, filter, iterator);
  }

  @Nonnull
  public DirectoryInfo getInfoForFileOrDirectory(@Nonnull VirtualFile file) {
    if (file instanceof VirtualFileDelegate) {
      file = ((VirtualFileDelegate)file).getDelegate();
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
