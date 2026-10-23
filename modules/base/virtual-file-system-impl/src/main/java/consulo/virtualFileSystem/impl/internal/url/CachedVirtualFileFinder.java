// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal.url;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

class CachedVirtualFileFinder {
    private record Cached(@Nullable VirtualFile file, long timestamp) {
    }

    private final AtomicReference<Cached> myCachedFile = new AtomicReference<>(new Cached(null, -1));

    void cacheVirtualFile(VirtualFileManager fileManager, VirtualFile file) {
        myCachedFile.set(new Cached(file, fileManager.getModificationCount()));
    }

    @Nullable VirtualFile getCachedVirtualFile(VirtualFileManager fileManager) {
        Cached cached = myCachedFile.get();
        return cached.timestamp() == fileManager.getModificationCount() ? cached.file() : null;
    }

    @Nullable VirtualFile findVirtualFile(VirtualFileManager fileManager, String url) {
        Cached cached = myCachedFile.get();
        if (cached.timestamp() == fileManager.getModificationCount()) {
            return cached.file();
        }

        long modCounterBefore = fileManager.getModificationCount();
        VirtualFile file = fileManager.findFileByUrl(url);
        long modCounterAfter = fileManager.getModificationCount();
        if (modCounterBefore == modCounterAfter) {
            myCachedFile.set(new Cached(file, modCounterAfter));
        }
        return file;
    }
}
