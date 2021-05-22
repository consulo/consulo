// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.IdFilter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Singleton
public final class FileNameIndexServiceImpl implements FileNameIndexService {
  private final FileBasedIndex myIndex;

  @Inject
  public FileNameIndexServiceImpl(FileBasedIndex index) {
    myIndex = index;
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> getVirtualFilesByName(Project project, @Nonnull String name, @Nonnull GlobalSearchScope scope, IdFilter filter) {
    Set<VirtualFile> files = new HashSet<>();
    myIndex.processValues(FilenameIndexImpl.NAME, name, null, (file, value) -> {
      files.add(file);
      return true;
    }, scope, filter);
    return files;
  }

  @Override
  public void processAllFileNames(@Nonnull Processor<? super String> processor, @Nonnull GlobalSearchScope scope, IdFilter filter) {
    myIndex.processAllKeys(FilenameIndexImpl.NAME, processor, scope, filter);
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> getFilesWithFileType(@Nonnull FileType fileType, @Nonnull GlobalSearchScope scope) {
    return myIndex.getContainingFiles(FileTypeIndexImpl.NAME, fileType, scope);
  }

  @Override
  public boolean processFilesWithFileType(@Nonnull FileType fileType, @Nonnull Processor<? super VirtualFile> processor, @Nonnull GlobalSearchScope scope) {
    return myIndex.processValues(FileTypeIndexImpl.NAME, fileType, null, (file, value) -> processor.process(file), scope);
  }
}