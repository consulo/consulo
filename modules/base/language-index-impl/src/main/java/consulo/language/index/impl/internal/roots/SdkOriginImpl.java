// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.roots;

import consulo.content.bundle.Sdk;
import consulo.language.index.impl.internal.roots.kind.SdkOrigin;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.Objects;

class SdkOriginImpl implements SdkOrigin {
    private final Sdk mySdk;
    private final Collection<VirtualFile> myRootsToIndex;

    SdkOriginImpl(Sdk sdk, Collection<VirtualFile> rootsToIndex) {
        mySdk = sdk;
        myRootsToIndex = rootsToIndex;
    }

    @Override
    public Sdk getSdk() {
        return mySdk;
    }

    @Override
    public Collection<VirtualFile> getRootsToIndex() {
        return myRootsToIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SdkOriginImpl other)) {
            return false;
        }
        return mySdk.equals(other.mySdk) && myRootsToIndex.equals(other.myRootsToIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mySdk, myRootsToIndex);
    }

    @Override
    public String toString() {
        return "SdkOriginImpl(sdk=" + mySdk.getName() + ", rootsToIndex=" + myRootsToIndex + ")";
    }
}
