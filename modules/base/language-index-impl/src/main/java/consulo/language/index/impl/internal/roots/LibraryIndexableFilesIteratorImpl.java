// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.application.ReadAction;
import consulo.content.ContentIterator;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.disposer.Disposer;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.LibraryIndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.LibraryOrigin;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import java.util.ArrayList;
import java.util.List;

public class LibraryIndexableFilesIteratorImpl implements LibraryIndexableFilesIterator {
    private final Library myLibrary;

    LibraryIndexableFilesIteratorImpl(Library library) {
        myLibrary = library;
    }

    @Override
    public String getDebugName() {
        return "Library " + myLibrary.getName();
    }

    @Override
    public LocalizeValue getIndexingProgressText() {
        return IndexingLocalize.indexableFilesProviderIndexingLibraryName(myLibrary.getName());
    }

    @Override
    public LocalizeValue getRootsScanningProgressText() {
        String libraryName = myLibrary.getName();
        if (!StringUtil.isEmpty(libraryName)) {
            return IndexingLocalize.indexableFilesProviderScanningLibraryName(libraryName);
        }
        return IndexingLocalize.indexableFilesProviderScanningAdditionalDependencies();
    }

    @Override
    public LibraryOrigin getOrigin() {
        return new LibraryOriginImpl(myLibrary);
    }

    @Override
    public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
        List<VirtualFile> roots = ReadAction.compute(() -> {
            if (Disposer.isDisposed(myLibrary)) {
                return List.of();
            }
            List<VirtualFile> files = new ArrayList<>();
            files.addAll(List.of(myLibrary.getFiles(SourcesOrderRootType.ID)));
            files.addAll(List.of(myLibrary.getFiles(BinariesOrderRootType.ID)));
            return files;
        });
        return IndexableFilesIterationMethods.iterateRoots(project, roots, fileIterator, fileFilter);
    }
}
