// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.application.ReadAction;
import consulo.content.ContentIterator;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.psi.stub.IndexableSetContributor;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import java.util.Set;

public class IndexableSetContributorFilesIterator implements IndexableFilesIterator {
    private final IndexableSetContributor myIndexableSetContributor;
    private final boolean myProjectAware;

    IndexableSetContributorFilesIterator(IndexableSetContributor indexableSetContributor, boolean projectAware) {
        myIndexableSetContributor = indexableSetContributor;
        myProjectAware = projectAware;
    }

    @Override
    public String getDebugName() {
        return "Indexable set contributor '" + myIndexableSetContributor.getClass().getName() + "' "
            + (myProjectAware ? "(project)" : "(non-project)");
    }

    @Override
    public LocalizeValue getIndexingProgressText() {
        return IndexingLocalize.indexableFilesProviderIndexingAdditionalDependencies();
    }

    @Override
    public LocalizeValue getRootsScanningProgressText() {
        return IndexingLocalize.indexableFilesProviderScanningAdditionalDependencies();
    }

    @Override
    public IndexableSetOrigin getOrigin() {
        return new IndexableSetContributorOriginImpl(myIndexableSetContributor);
    }

    @Override
    public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
        Set<VirtualFile> allRoots = ReadAction.compute(
            () -> myProjectAware
                ? IndexableSetContributor.getProjectRootsToIndex(myIndexableSetContributor, project)
                : IndexableSetContributor.getRootsToIndex(myIndexableSetContributor)
        );
        return IndexableFilesIterationMethods.iterateRoots(project, allRoots, fileIterator, fileFilter, false);
    }
}
