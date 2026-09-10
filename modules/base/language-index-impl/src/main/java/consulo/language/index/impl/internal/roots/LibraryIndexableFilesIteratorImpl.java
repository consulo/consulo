// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.roots;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.content.ContentIterator;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.kind.LibraryOrigin;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LibraryIndexableFilesIteratorImpl implements LibraryIndexableFilesIterator {
    private static final boolean ITERATE_OVER_SOURCE_ROOTS =
        SystemProperties.getBooleanProperty("LibraryIndexableFilesIterator.iterate.over.sources", true);

    private final Library myLibrary;
    @Nullable
    private final String myLibraryName;
    private final List<VirtualFile> myClassRoots;
    private final List<VirtualFile> mySourceRoots;
    private final LibraryOrigin myOrigin;

    private LibraryIndexableFilesIteratorImpl(Library library, List<VirtualFile> classRoots, List<VirtualFile> sourceRoots) {
        myLibrary = library;
        myLibraryName = library.getName();
        myClassRoots = classRoots;
        mySourceRoots = sourceRoots;
        myOrigin = new LibraryOriginImpl(library, classRoots, sourceRoots);
    }

    @Override
    public String getDebugName() {
        String debugMessage;
        if (!myClassRoots.isEmpty()) {
            debugMessage = "(class root " + myClassRoots.get(0).getName() + ")";
        }
        else if (!mySourceRoots.isEmpty()) {
            debugMessage = "(source root " + mySourceRoots.get(0).getName() + ")";
        }
        else {
            debugMessage = "(no root)";
        }
        return "Library " + StringUtil.notNullize(myLibraryName) + " " + debugMessage;
    }

    @Override
    public LocalizeValue getIndexingProgressText() {
        return IndexingLocalize.indexableFilesProviderIndexingLibraryName(StringUtil.notNullize(myLibraryName));
    }

    @Override
    public LocalizeValue getRootsScanningProgressText() {
        if (!StringUtil.isEmpty(myLibraryName)) {
            return IndexingLocalize.indexableFilesProviderScanningLibraryName(myLibraryName);
        }
        return IndexingLocalize.indexableFilesProviderScanningAdditionalDependencies();
    }

    @Override
    public LibraryOrigin getOrigin() {
        return myOrigin;
    }

    @Override
    public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
        Set<VirtualFile> roots = ReadAction.compute(() -> {
            Set<VirtualFile> validRoots = new LinkedHashSet<VirtualFile>();
            if (!myLibrary.isDisposed()) {
                for (VirtualFile root : getRoots()) {
                    if (root.isValid()) {
                        validRoots.add(root);
                    }
                }
            }
            return validRoots;
        });
        return IndexableFilesIterationMethods.iterateRoots(project, roots, fileIterator, fileFilter);
    }

    private List<VirtualFile> getRoots() {
        if (!ITERATE_OVER_SOURCE_ROOTS) {
            return myClassRoots;
        }
        List<VirtualFile> roots = new ArrayList<>(myClassRoots);
        roots.addAll(mySourceRoots);
        return roots;
    }

    public static List<VirtualFile> collectFiles(Library library, String rootType, @Nullable Collection<VirtualFile> rootsToFilter) {
        VirtualFile[] libraryRoots = library.getFiles(rootType);
        if (rootsToFilter == null) {
            return List.of(libraryRoots);
        }
        List<VirtualFile> rootsToIterate = new ArrayList<>();
        for (VirtualFile root : rootsToFilter) {
            for (VirtualFile libraryRoot : libraryRoots) {
                if (VirtualFileUtil.isAncestor(libraryRoot, root, false)) {
                    rootsToIterate.add(root);
                    break;
                }
            }
        }
        return rootsToIterate;
    }

    @RequiredReadAction
    @Nullable
    public static LibraryIndexableFilesIteratorImpl createIterator(Library library) {
        return createIterator(library, null, null);
    }

    @RequiredReadAction
    @Nullable
    public static LibraryIndexableFilesIteratorImpl createIterator(
        Library library,
        @Nullable Collection<VirtualFile> roots,
        @Nullable Collection<VirtualFile> sourceRoots
    ) {
        if (library.isDisposed()) {
            return null;
        }
        return new LibraryIndexableFilesIteratorImpl(
            library,
            collectFiles(library, BinariesOrderRootType.ID, roots),
            collectFiles(library, SourcesOrderRootType.ID, sourceRoots)
        );
    }

    @RequiredReadAction
    public static List<IndexableFilesIterator> createIteratorList(Library library) {
        LibraryIndexableFilesIteratorImpl iterator = createIterator(library);
        return iterator == null ? List.of() : List.of(iterator);
    }

    @RequiredReadAction
    public static List<IndexableFilesIterator> createIterators(Library library) {
        return createIterators(library, null, null);
    }

    @RequiredReadAction
    public static List<IndexableFilesIterator> createIterators(
        Library library,
        @Nullable Collection<VirtualFile> roots,
        @Nullable Collection<VirtualFile> sourceRoots
    ) {
        if (library.isDisposed()) {
            return List.of();
        }
        List<IndexableFilesIterator> iterators = new ArrayList<>();
        for (VirtualFile classRoot : collectFiles(library, BinariesOrderRootType.ID, roots)) {
            iterators.add(new LibraryIndexableFilesIteratorImpl(library, List.of(classRoot), List.of()));
        }
        for (VirtualFile sourceRoot : collectFiles(library, SourcesOrderRootType.ID, sourceRoots)) {
            iterators.add(new LibraryIndexableFilesIteratorImpl(library, List.of(), List.of(sourceRoot)));
        }
        return iterators;
    }
}
