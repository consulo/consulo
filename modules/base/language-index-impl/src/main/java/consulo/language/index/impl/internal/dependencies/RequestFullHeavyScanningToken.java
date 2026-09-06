// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import consulo.virtualFileSystem.VirtualFile;

final class RequestFullHeavyScanningToken extends ScanningRequestToken {
    static final RequestFullHeavyScanningToken INSTANCE = new RequestFullHeavyScanningToken();

    private RequestFullHeavyScanningToken() {
    }

    @Override
    public FileIndexingStamp getFileIndexingStamp(VirtualFile file) {
        throw new IllegalStateException("This token is a marker. It should not be used.");
    }

    @Override
    public AppIndexingDependenciesToken getAppIndexingRequestId() {
        throw new IllegalStateException("This token is a marker. It should not be used.");
    }
}
