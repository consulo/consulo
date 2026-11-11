// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal.refresh;

import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.event.ChildInfo;
import org.jspecify.annotations.Nullable;

interface ChildScanner {
    record ScannedChildren(ChildInfo[] children, boolean childrenComplete) {
    }

    /**
     * Traverses children of {@code childName} recursively, so they are loaded into VFS during event processing
     *
     * @return {@code null} if error occurred or children must not be loaded proactively. Children to load into VFS otherwise
     */
    @Nullable ScannedChildren scanChildrenRecursively(NewVirtualFile parent, String childName);
}
