// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal;

import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.CacheAvoidingVirtualFile;
import org.jspecify.annotations.Nullable;

public final class TransientVirtualFileVfsRefreshUtils {
    private TransientVirtualFileVfsRefreshUtils() {
    }

    /**
     * This method must only be used for VFS refresh to avoid iterating over cached virtual files.
     * <p>
     * Resulting child may already not exist on disk. Check {@code exists()} before usage
     */
    public static VirtualFile createTransientChild(VirtualFile parent, String childName, NewVirtualFileSystem fs) {
        return createTransientChild(parent, childName, fs, null);
    }

    /**
     * This method must only be used for VFS refresh to avoid iterating over cached virtual files.
     * <p>
     * Resulting child may already not exist on disk. Check {@code exists()} before usage
     */
    public static VirtualFile createTransientChild(VirtualFile parent,
                                                   String childName,
                                                   NewVirtualFileSystem fs,
                                                   @Nullable FileAttributes attributes) {
        String parentPath = parent.getPath();
        String path = parentPath.endsWith("/") ? parent.getPath() + childName : parentPath + "/" + childName;
        return new TransientVirtualFileImpl(childName, path, fs, parent, attributes);
    }

    /**
     * @see CacheAvoidingVirtualFile#isCached()
     */
    public static boolean isCached(VirtualFile file) {
        return file instanceof CacheAvoidingVirtualFile cacheAvoiding && cacheAvoiding.isCached();
    }
}
