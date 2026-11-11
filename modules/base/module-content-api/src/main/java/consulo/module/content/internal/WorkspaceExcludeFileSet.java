// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.module.content.internal;

import consulo.virtualFileSystem.VirtualFile;

/**
 * An exclusion rule which a top-down traversal applies to files one by one.
 * <p>
 * The masks use {@link WorkspaceFileKindMask} bits. They describe the kinds of file sets which include a file.
 */
public sealed interface WorkspaceExcludeFileSet permits WorkspaceExcludeFileSet.ByFileKind,
    WorkspaceExcludeFileSet.DirectoryRoot,
    WorkspaceExcludeFileSet.ByPattern,
    WorkspaceExcludeFileSet.ByCondition,
    WorkspaceExcludeFileSet.ByUnscopedCondition {
    /**
     * Returns {@code mask} without the kinds which this rule excludes for {@code file}.
     * <p>
     * {@code file} is the root of this rule or a file below it. The function checks only {@code file} itself.
     * The traversal applies the rule to the parents of {@code file} before, so the function ignores them.
     * {@code file} can be a transient file, so do not compare it with cached files.
     */
    int inPlaceComputeMasks(VirtualFile file, int mask);

    /**
     * Excludes the root and its descendants from the kinds selected by {@link #getMask()}.
     * A nested file set can include files again.
     */
    non-sealed interface ByFileKind extends WorkspaceExcludeFileSet {
        int getMask();

        @Override
        default int inPlaceComputeMasks(VirtualFile file, int mask) {
            return unsetKinds(mask, getMask());
        }
    }

    /**
     * Excludes the root and its descendants from all kinds while the root is a directory.
     * A nested file set can include files again.
     */
    non-sealed interface DirectoryRoot extends WorkspaceExcludeFileSet {
        @Override
        default int inPlaceComputeMasks(VirtualFile file, int mask) {
            return file.isDirectory() ? unsetKinds(mask, WorkspaceFileKindMask.ALL) : mask;
        }
    }

    /**
     * Excludes the files whose names match the patterns.
     * A nested file set can include files again.
     */
    non-sealed interface ByPattern extends WorkspaceExcludeFileSet {
        boolean matches(CharSequence fileName);

        @Override
        default int inPlaceComputeMasks(VirtualFile file, int mask) {
            return matches(file.getNameSequence()) ? unsetKinds(mask, WorkspaceFileKindMask.ALL) : mask;
        }
    }

    /**
     * Excludes the root and the files below it which satisfy {@link #getCondition()}.
     * A nested file set can include files again.
     */
    non-sealed interface ByCondition extends WorkspaceExcludeFileSet {
        WorkspaceFileSetExclusionCondition getCondition();

        @Override
        default int inPlaceComputeMasks(VirtualFile file, int mask) {
            return getCondition().shouldExclude(file) ? unsetKinds(mask, WorkspaceFileKindMask.ALL) : mask;
        }
    }

    /**
     * Uses the same condition check as {@link ByCondition}.
     * The exclusion also applies inside a nested file set, to the root of that file set and the files below it.
     */
    non-sealed interface ByUnscopedCondition extends WorkspaceExcludeFileSet {
        WorkspaceFileSetExclusionCondition getCondition();

        @Override
        default int inPlaceComputeMasks(VirtualFile file, int mask) {
            return getCondition().shouldExclude(file) ? unsetKinds(mask, WorkspaceFileKindMask.ALL) : mask;
        }
    }

    private static int unsetKinds(int mask, int kinds) {
        return mask & ~kinds;
    }
}
