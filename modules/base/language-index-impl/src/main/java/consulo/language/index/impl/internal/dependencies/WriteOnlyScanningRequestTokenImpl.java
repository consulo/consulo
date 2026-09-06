// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;

public class WriteOnlyScanningRequestTokenImpl extends ScanningRequestToken {
    private final AppIndexingDependenciesToken myAppIndexingRequestId;
    private final boolean myForceCheckingForOutdatedIndexesUsingFileModCount;

    public WriteOnlyScanningRequestTokenImpl(AppIndexingDependenciesToken appIndexingRequest,
                                             boolean forceCheckingForOutdatedIndexesUsingFileModCount) {
        myAppIndexingRequestId = appIndexingRequest;
        myForceCheckingForOutdatedIndexesUsingFileModCount = forceCheckingForOutdatedIndexesUsingFileModCount;
    }

    @Override
    public AppIndexingDependenciesToken getAppIndexingRequestId() {
        return myAppIndexingRequestId;
    }

    @Override
    public FileIndexingStamp getFileIndexingStamp(VirtualFile file) {
        if (!(file instanceof VirtualFileWithId)) {
            return ProjectIndexingDependenciesService.NULL_STAMP;
        }
        int fileStamp = ManagingFS.getInstance().getModificationCount(file);
        return getFileIndexingStamp(fileStamp);
    }

    public FileIndexingStamp getFileIndexingStamp(int fileStamp) {
        return WriteOnlyFileIndexingStampImpl.create(myAppIndexingRequestId.toInt(), fileStamp,
            myForceCheckingForOutdatedIndexesUsingFileModCount);
    }
}
