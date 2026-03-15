// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.content.scope.SearchScope;
import consulo.language.internal.FileNameIndexService;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IdFilter;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@Singleton
@ServiceImpl
public final class FileNameIndexServiceImpl implements FileNameIndexService {
    private final FileBasedIndex myIndex;

    @Inject
    public FileNameIndexServiceImpl(FileBasedIndex index) {
        myIndex = index;
    }

    
    @Override
    public Collection<VirtualFile> getVirtualFilesByName(
        Project project,
        String name,
        SearchScope scope,
        IdFilter filter
    ) {
        Set<VirtualFile> files = new HashSet<>();
        myIndex.processValues(FilenameIndexImpl.NAME, name, null, (file, value) -> {
            files.add(file);
            return true;
        }, scope, filter);
        return files;
    }

    @Override
    public void processAllFileNames(Predicate<? super String> processor, SearchScope scope, IdFilter filter) {
        myIndex.processAllKeys(FilenameIndexImpl.NAME, processor, scope, filter);
    }

    
    @Override
    public Collection<VirtualFile> getFilesWithFileType(FileType fileType, SearchScope scope) {
        return myIndex.getContainingFiles(FileTypeIndexImpl.NAME, fileType, scope);
    }

    @Override
    public boolean processFilesWithFileType(
        FileType fileType,
        Predicate<? super VirtualFile> processor,
        SearchScope scope
    ) {
        return myIndex.processValues(FileTypeIndexImpl.NAME, fileType, null, (file, value) -> processor.test(file), scope);
    }
}