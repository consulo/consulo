// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.roots;

import consulo.annotation.access.RequiredReadAction;
import consulo.content.ContentIterator;
import consulo.content.RootProvider;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkType;
import consulo.content.bundle.SdkTypeId;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.kind.SdkOrigin;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class SdkIndexableFilesIteratorImpl implements IndexableFilesIterator {
    private final Sdk mySdk;
    private final String mySdkName;
    private final SdkTypeId mySdkType;
    @Nullable
    private final String mySdkHome;
    private final List<VirtualFile> myRootsToIndex;
    private final SdkOrigin myOrigin;

    private SdkIndexableFilesIteratorImpl(Sdk sdk, List<VirtualFile> rootsToIndex) {
        mySdk = sdk;
        mySdkName = sdk.getName();
        mySdkType = sdk.getSdkType();
        mySdkHome = sdk.getHomePath();
        myRootsToIndex = rootsToIndex;
        myOrigin = new SdkOriginImpl(sdk, rootsToIndex);
    }

    private String getSdkPresentableName() {
        String name = mySdkType instanceof SdkType sdkType ? sdkType.getDisplayName().get() : null;
        return name == null || name.isEmpty() ? IndexingLocalize.indexableFilesProviderIndexingSdkUnnamed().get() : name;
    }

    @Override
    public String getDebugName() {
        return getSdkPresentableName() + " " + mySdkName + " " + mySdkHome
            + " (" + myRootsToIndex.stream().map(VirtualFile::getName).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public LocalizeValue getIndexingProgressText() {
        return IndexingLocalize.indexableFilesProviderIndexingSdk(getSdkPresentableName(), mySdkName);
    }

    @Override
    public LocalizeValue getRootsScanningProgressText() {
        return IndexingLocalize.indexableFilesProviderScanningSdk(getSdkPresentableName(), mySdkName);
    }

    @Override
    public SdkOrigin getOrigin() {
        return myOrigin;
    }

    @Override
    public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
        return IndexableFilesIterationMethods.iterateRoots(project, myRootsToIndex, fileIterator, fileFilter);
    }

    @RequiredReadAction
    public static SdkIndexableFilesIteratorImpl createIterator(Sdk sdk) {
        return createIterator(sdk, List.of());
    }

    @RequiredReadAction
    public static SdkIndexableFilesIteratorImpl createIterator(Sdk sdk, Collection<VirtualFile> rootsToIndex) {
        List<VirtualFile> roots = rootsToIndex.isEmpty() ? getRootsToIndex(sdk) : List.copyOf(rootsToIndex);
        return new SdkIndexableFilesIteratorImpl(sdk, roots);
    }

    @RequiredReadAction
    public static Collection<IndexableFilesIterator> createIterators(Sdk sdk) {
        List<IndexableFilesIterator> iterators = new ArrayList<>();
        for (VirtualFile root : getRootsToIndex(sdk)) {
            iterators.add(new SdkIndexableFilesIteratorImpl(sdk, List.of(root)));
        }
        return iterators;
    }

    @RequiredReadAction
    public static Collection<IndexableFilesIterator> createIterators(Sdk sdk, List<VirtualFile> listOfRootsToFilter) {
        List<VirtualFile> sdkRoots = new ArrayList<>(getRootsToIndex(sdk));
        List<VirtualFile> rootsToIndex = filterRootsToIterate(sdkRoots, listOfRootsToFilter);

        List<IndexableFilesIterator> iterators = new ArrayList<>();
        for (VirtualFile root : rootsToIndex) {
            iterators.add(new SdkIndexableFilesIteratorImpl(sdk, List.of(root)));
        }
        return iterators;
    }

    private static List<VirtualFile> getRootsToIndex(Sdk sdk) {
        RootProvider rootProvider = sdk.getRootProvider();
        List<VirtualFile> roots = new ArrayList<>();
        roots.addAll(List.of(rootProvider.getFiles(SourcesOrderRootType.ID)));
        roots.addAll(List.of(rootProvider.getFiles(BinariesOrderRootType.ID)));
        return roots;
    }

    private static List<VirtualFile> filterRootsToIterate(List<VirtualFile> initialRoots, List<VirtualFile> listOfRootsToFilter) {
        List<VirtualFile> rootsToFilter = new ArrayList<>(listOfRootsToFilter);
        List<VirtualFile> rootsToIndex = new ArrayList<>();

        Iterator<VirtualFile> iteratorToFilter = rootsToFilter.iterator();
        while (iteratorToFilter.hasNext()) {
            VirtualFile next = iteratorToFilter.next();
            for (VirtualFile sdkRoot : initialRoots) {
                if (VirtualFileUtil.isAncestor(next, sdkRoot, false)) {
                    rootsToIndex.add(sdkRoot);
                    initialRoots.remove(sdkRoot);
                    iteratorToFilter.remove();
                    break;
                }
            }
        }
        for (VirtualFile file : rootsToFilter) {
            for (VirtualFile sdkRoot : initialRoots) {
                if (VirtualFileUtil.isAncestor(sdkRoot, file, false)) {
                    rootsToIndex.add(file);
                }
            }
        }
        return rootsToIndex;
    }
}
