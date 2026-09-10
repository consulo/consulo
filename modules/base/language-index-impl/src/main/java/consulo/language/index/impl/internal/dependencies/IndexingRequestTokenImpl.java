// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class IndexingRequestTokenImpl implements IndexingRequestToken {
    private final AppIndexingDependenciesToken myAppIndexingRequest;
    private final int myAppIndexingRequestId;

    public IndexingRequestTokenImpl(AppIndexingDependenciesToken appIndexingRequest) {
        myAppIndexingRequest = appIndexingRequest;
        myAppIndexingRequestId = appIndexingRequest.toInt();
    }

    public AppIndexingDependenciesToken getAppIndexingRequest() {
        return myAppIndexingRequest;
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
        // we assume that appIndexingRequestId and file.modificationStamp never decrease => their sum only grow up
        // in the case of overflow we hope that new value does not match any previously used value
        // (which is hopefully true in most cases, because (new value)==(old value) was used veeeery long time ago)
        return WriteOnlyFileIndexingStampImpl.create(myAppIndexingRequestId, fileStamp, true);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexingRequestTokenImpl other)) {
            return false;
        }
        return myAppIndexingRequest.equals(other.myAppIndexingRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(myAppIndexingRequest);
    }

    @Override
    public String toString() {
        return "IndexingRequestTokenImpl(appIndexingRequest=" + myAppIndexingRequest + ")";
    }
}
