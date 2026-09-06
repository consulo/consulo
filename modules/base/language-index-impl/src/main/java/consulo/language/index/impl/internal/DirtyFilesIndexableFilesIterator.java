// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.application.internal.ProgressIndicatorUtils;
import consulo.content.ContentIterator;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.IndexableFilesIterationMethods;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.DirtyFilesOrigin;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DirtyFilesIndexableFilesIterator implements IndexableFilesIterator {
    private final CompletableFuture<List<VirtualFile>> myDirtyFileIndexesCleanupFuture;
    private final boolean myFromOrphanQueue;

    public DirtyFilesIndexableFilesIterator(CompletableFuture<List<VirtualFile>> dirtyFileIndexesCleanupFuture, boolean fromOrphanQueue) {
        myDirtyFileIndexesCleanupFuture = dirtyFileIndexesCleanupFuture;
        myFromOrphanQueue = fromOrphanQueue;
    }

    @Override
    public String getDebugName() {
        return "dirty files iterator (from orphan queue=" + myFromOrphanQueue + ")";
    }

    @Override
    public LocalizeValue getIndexingProgressText() {
        return IndexingLocalize.indexableFilesProviderIndexingFilesFromPreviousIdeSession();
    }

    @Override
    public IndexableSetOrigin getOrigin() {
        return DirtyFilesOrigin.INSTANCE;
    }

    @Override
    public LocalizeValue getRootsScanningProgressText() {
        return LocalizeValue.empty();
    }

    @Override
    public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
        List<VirtualFile> projectDirtyVirtualFiles = ProgressIndicatorUtils.awaitWithCheckCanceled(myDirtyFileIndexesCleanupFuture);
        return IndexableFilesIterationMethods.iterateRoots(project, projectDirtyVirtualFiles, fileIterator, fileFilter);
    }
}
