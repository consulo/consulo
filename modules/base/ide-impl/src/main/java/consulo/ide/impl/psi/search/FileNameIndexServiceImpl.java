// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.search;

import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IdFilter;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
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
  public Collection<VirtualFile> getVirtualFilesByName(Project project, @Nonnull String name, @Nonnull SearchScope scope, IdFilter filter) {
    Set<VirtualFile> files = new HashSet<>();
    myIndex.processValues(FilenameIndexImpl.NAME, name, null, (file, value) -> {
      files.add(file);
      return true;
    }, scope, filter);
    return files;
  }

  @Override
  public void processAllFileNames(@Nonnull Processor<? super String> processor, @Nonnull SearchScope scope, IdFilter filter) {
    myIndex.processAllKeys(FilenameIndexImpl.NAME, processor, scope, filter);
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> getFilesWithFileType(@Nonnull FileType fileType, @Nonnull SearchScope scope) {
    return myIndex.getContainingFiles(FileTypeIndexImpl.NAME, fileType, scope);
  }

  @Override
  public boolean processFilesWithFileType(@Nonnull FileType fileType, @Nonnull Processor<? super VirtualFile> processor, @Nonnull SearchScope scope) {
    return myIndex.processValues(FileTypeIndexImpl.NAME, fileType, null, (file, value) -> processor.process(file), scope);
  }
}