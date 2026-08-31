// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.application.ReadAction;
import consulo.content.ContentIterator;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkType;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import java.util.ArrayList;
import java.util.List;

public class SdkIndexableFilesIteratorImpl implements IndexableFilesIterator {
    private final Sdk mySdk;

    SdkIndexableFilesIteratorImpl(Sdk sdk) {
        mySdk = sdk;
    }

    private String getSdkPresentableName() {
        String name = mySdk.getSdkType() instanceof SdkType sdkType ? sdkType.getDisplayName().get() : null;
        return name == null || name.isEmpty() ? IndexingLocalize.indexableFilesProviderIndexingSdkUnnamed().get() : name;
    }

    @Override
    public String getDebugName() {
        return getSdkPresentableName() + " " + mySdk.getName();
    }

    @Override
    public LocalizeValue getIndexingProgressText() {
        return IndexingLocalize.indexableFilesProviderIndexingSdk(getSdkPresentableName(), mySdk.getName());
    }

    @Override
    public LocalizeValue getRootsScanningProgressText() {
        return IndexingLocalize.indexableFilesProviderScanningSdk(getSdkPresentableName(), mySdk.getName());
    }

    @Override
    public IndexableSetOrigin getOrigin() {
        return new SdkOriginImpl(mySdk);
    }

    @Override
    public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
        List<VirtualFile> roots = ReadAction.compute(() -> {
            List<VirtualFile> files = new ArrayList<>();
            files.addAll(List.of(mySdk.getRootProvider().getFiles(SourcesOrderRootType.ID)));
            files.addAll(List.of(mySdk.getRootProvider().getFiles(BinariesOrderRootType.ID)));
            return files;
        });
        return IndexableFilesIterationMethods.iterateRoots(project, roots, fileIterator, fileFilter);
    }
}
