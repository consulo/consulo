// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.module.content.internal;

import java.util.List;

/**
 * Describes the file sets and the exclusions registered exactly at one file.
 * {@link WorkspaceFileIndexEx#getFileSetsAt} returns it.
 *
 * @param mask          The kinds of all file sets registered at the file, as {@link WorkspaceFileKindMask} bits.
 * @param maskRecursive The kinds of the recursive file sets registered at the file, as {@link WorkspaceFileKindMask} bits.
 * @param excludes      The exclusions registered at the file.
 */
public record WorkspaceFileSets(int mask, int maskRecursive, List<WorkspaceExcludeFileSet> excludes) {
    public static final WorkspaceFileSets EMPTY = new WorkspaceFileSets(0, 0, List.of());
}
