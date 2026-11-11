// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.module.content.internal;

import consulo.virtualFileSystem.VirtualFile;

/**
 * A condition for excluding files from a root.
 */
public interface WorkspaceFileSetExclusionCondition {
    /**
     * Returns {@code true} if {@code file} should be excluded.
     */
    boolean shouldExclude(VirtualFile file);
}
