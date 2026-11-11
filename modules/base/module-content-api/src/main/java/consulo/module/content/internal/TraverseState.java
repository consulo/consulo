// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.module.content.internal;

import consulo.application.Application;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.internal.CacheAvoidingVirtualFile;
import consulo.virtualFileSystem.url.VirtualFileUrl;
import consulo.virtualFileSystem.url.VirtualFileUrlManager;
import org.jspecify.annotations.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes how the workspace includes a file during a top-down traversal of a file tree.
 * Use {@link #goDown} to get the state of a child file.
 * <p>
 * The rules are:
 * <ul>
 * <li>the nearest file set of a kind includes a file, if no exclusion between them excludes the file;</li>
 * <li>a nested file set can include files again, unless an unscoped exclusion excludes them;</li>
 * <li>an ignored file is included only by the file sets registered at it.</li>
 * </ul>
 */
public final class TraverseState {
    private static final int INDEXABLE_KINDS =
        WorkspaceFileKindMask.ALL & ~(WorkspaceFileKindMask.CONTENT_NON_INDEXABLE | WorkspaceFileKindMask.EXTERNAL_NON_INDEXABLE);

    /**
     * The kinds of file sets which include the current file, as {@link WorkspaceFileKindMask} bits.
     */
    private final int mask;
    /**
     * The kinds of recursive file sets which include the current file, as {@link WorkspaceFileKindMask} bits.
     * The children of the current file inherit these kinds.
     */
    private final int maskRecursive;

    private final List<ActiveExclude> activeExcludes;

    /**
     * The exclusions which can still exclude the descendants of the current file.
     */
    private final List<WorkspaceExcludeFileSet> excludes;

    private record ActiveExclude(WorkspaceExcludeFileSet rule, int kinds) {
        ActiveExclude withKinds(int kinds) {
            return kinds == this.kinds ? this : new ActiveExclude(rule, kinds);
        }
    }

    private TraverseState(int mask, int maskRecursive, List<ActiveExclude> activeExcludes) {
        this.mask = mask;
        this.maskRecursive = maskRecursive;
        this.activeExcludes = activeExcludes;
        this.excludes = activeExcludes.isEmpty() ? List.of() : new AbstractList<>() {
            @Override
            public int size() {
                return activeExcludes.size();
            }

            @Override
            public WorkspaceExcludeFileSet get(int index) {
                return activeExcludes.get(index).rule();
            }
        };
    }

    public int getMask() {
        return mask;
    }

    public int getMaskRecursive() {
        return maskRecursive;
    }

    public List<WorkspaceExcludeFileSet> getExcludes() {
        return excludes;
    }

    /**
     * {@code true} if a file set includes the current file.
     */
    public boolean isInWorkspace() {
        return mask != 0;
    }

    /**
     * {@code true} if an indexable file set includes the current file.
     */
    public boolean isIndexable() {
        return (mask & INDEXABLE_KINDS) != 0;
    }

    /**
     * {@code true} if an indexable recursive file set includes the current file.
     */
    public boolean isIndexableRecursive() {
        return (maskRecursive & INDEXABLE_KINDS) != 0;
    }

    /**
     * Returns the state of {@code file}, a child of the current file.
     * {@code fileIndex} supplies the file sets for {@code file} by {@code url}, the URL of {@code file}.
     * Pass {@code null} as {@code url} if no {@link VirtualFileUrl} exists for {@code file}. Then no file sets are registered at {@code file}.
     * {@code file} can be a transient file.
     * Returns {@code null} if an unscoped exclusion excludes {@code file} and all its descendants.
     */
    public @Nullable TraverseState goDown(VirtualFile file, @Nullable VirtualFileUrl url, WorkspaceFileIndexEx fileIndex) {
        return goDown(file, url != null ? fileIndex.getFileSetsAt(url) : WorkspaceFileSets.EMPTY);
    }

    /**
     * The same as {@link #goDown(VirtualFile, VirtualFileUrl, WorkspaceFileIndexEx)}, but uses {@code fileSetsAt} instead of the data
     * registered at {@code file}.
     */
    @Nullable TraverseState goDown(VirtualFile file, WorkspaceFileSets fileSetsAt) {
        int inherited = FileTypeRegistry.getInstance().isFileIgnored(file) ? 0 : maskRecursive;
        int newMask = inherited | fileSetsAt.mask();
        int newMaskRecursive = inherited | fileSetsAt.maskRecursive();
        if (activeExcludes.isEmpty() && fileSetsAt.excludes().isEmpty()) {
            return hasSameMasks(newMask, newMaskRecursive) ? this : new TraverseState(newMask, newMaskRecursive, List.of());
        }

        if (isExcludedByUnscoped(file, fileSetsAt)) {
            return null;
        }

        int[] finalMasks = applyExcludes(file, fileSetsAt, newMask, newMaskRecursive);
        int finalMask = finalMasks[0];
        int finalMaskRecursive = finalMasks[1];
        List<ActiveExclude> newExcludes = updateExcludes(finalMaskRecursive, fileSetsAt);
        return new TraverseState(finalMask, finalMaskRecursive, newExcludes);
    }

    /**
     * Applies scoped exclusions to the given masks.
     * Returns a pair of (newMask, newMaskRecursive) with excluded kinds removed.
     */
    private int[] applyExcludes(VirtualFile file, WorkspaceFileSets fileSetsAt, int mask, int maskRecursive) {
        int[] masks = {mask, maskRecursive};

        for (ActiveExclude activeExclude : activeExcludes) {
            int kinds = activeExclude.kinds();
            applyScopedExclude(masks, file, activeExclude.rule(), kinds & ~fileSetsAt.mask(), kinds & ~fileSetsAt.maskRecursive());
        }
        for (WorkspaceExcludeFileSet exclude : fileSetsAt.excludes()) {
            applyScopedExclude(masks, file, exclude, WorkspaceFileKindMask.ALL, WorkspaceFileKindMask.ALL);
        }
        return masks;
    }

    /**
     * apply {@code exclude} to {@code masks}
     */
    private static void applyScopedExclude(int[] masks,
                                           VirtualFile file,
                                           WorkspaceExcludeFileSet exclude,
                                           int kindsForFile,
                                           int kindsForChildren) {
        if (isUnscoped(exclude) || ((masks[0] & kindsForFile) | (masks[1] & kindsForChildren)) == 0) {
            return;
        }
        int excludedKinds = excludedKinds(exclude, file);
        masks[0] = masks[0] & ~(excludedKinds & kindsForFile);
        masks[1] = masks[1] & ~(excludedKinds & kindsForChildren);
    }

    private boolean isExcludedByUnscoped(VirtualFile file, WorkspaceFileSets fileSetsAt) {
        for (WorkspaceExcludeFileSet exclude : excludes) {
            if (isUnscoped(exclude) && excludedKinds(exclude, file) != 0) {
                return true;
            }
        }
        for (WorkspaceExcludeFileSet exclude : fileSetsAt.excludes()) {
            if (isUnscoped(exclude) && excludedKinds(exclude, file) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * update exclude kinds for excludes
     */
    private List<ActiveExclude> updateExcludes(int newMaskRecursive, WorkspaceFileSets fileSetsAt) {
        // A scoped exclusion matters only for the kinds which the children inherit.
        // A nested file set of other kinds resets the exclusions for its kinds.
        int keptScopedKinds = newMaskRecursive & ~fileSetsAt.maskRecursive();
        List<ActiveExclude> newExcludes = new ArrayList<>(activeExcludes.size() + fileSetsAt.excludes().size());
        for (ActiveExclude exclude : activeExcludes) {
            int newExcludeKinds = isUnscoped(exclude.rule()) ? exclude.kinds() : exclude.kinds() & keptScopedKinds;
            if (newExcludeKinds == 0) {
                continue;
            }

            ActiveExclude newExclude = exclude.withKinds(newExcludeKinds);
            newExcludes.add(newExclude);
        }
        for (WorkspaceExcludeFileSet exclude : fileSetsAt.excludes()) {
            int newKinds = isUnscoped(exclude) ? WorkspaceFileKindMask.ALL : newMaskRecursive;
            if (newKinds != 0) {
                newExcludes.add(new ActiveExclude(exclude, newKinds));
            }
        }

        return newExcludes;
    }

    private boolean hasSameMasks(int newMask, int newMaskRecursive) {
        return newMask == mask && newMaskRecursive == maskRecursive;
    }

    @Override
    public String toString() {
        return "TraverseState{mask=" + mask + ", maskRecursive=" + maskRecursive + ", excludes=" + excludes + "}";
    }

    /**
     * Returns the top-down traverse state for {@code file} in {@code project}.
     * Collects the included kinds and the exclusions from the ancestors of {@code file}.
     *
     * @return TraverseState for file or {@code null} if file and all descendants are excluded
     * @throws IllegalArgumentException if {@code file} is a {@link CacheAvoidingVirtualFile}
     */
    public static @Nullable TraverseState getTraverseState(VirtualFile file, Project project) {
        if (file instanceof CacheAvoidingVirtualFile) {
            throw new IllegalArgumentException(file + " must be cached in VFS");
        }
        List<VirtualFile> path = new ArrayList<>();
        VirtualFile current = file;
        while (current != null) {
            path.add(current);
            // A thin-client file can return itself as its parent.
            VirtualFile parent = current.getParent();
            current = Objects.equals(parent, current) ? null : parent;
        }

        WorkspaceFileIndexEx fileIndex = WorkspaceFileIndexEx.getInstance(project);
        VirtualFileUrlManager urlManager = Application.get().getInstance(VirtualFileUrlManager.class);
        VirtualFile root = path.get(path.size() - 1);
        String rootUrl = root.getUrl();
        VirtualFileUrl url = urlManager.findNode(rootUrl.endsWith("/") ? rootUrl.substring(0, rootUrl.length() - 1) : rootUrl);
        TraverseState state = emptyState().goDown(root, rootUrl.endsWith("/") ? (url == null ? null : url.findChild("")) : url, fileIndex);
        for (int i = path.size() - 2; i >= 0; i--) {
            if (state == null) {
                return null;
            }
            VirtualFile child = path.get(i);
            url = url == null ? null : url.findChild(child.getName());
            state = state.goDown(child, url, fileIndex);
        }
        return state;
    }

    /**
     * Returns the state of a file which no file set includes, and which no exclusion affects.
     */
    private static TraverseState emptyState() {
        return new TraverseState(0, 0, List.of());
    }

    /**
     * An unscoped exclusion applies also inside a file set nested below its root.
     */
    private static boolean isUnscoped(WorkspaceExcludeFileSet exclude) {
        return exclude instanceof WorkspaceExcludeFileSet.ByUnscopedCondition;
    }

    /**
     * Returns the kinds which this exclusion removes for {@code file}, as {@link WorkspaceFileKindMask} bits.
     */
    private static int excludedKinds(WorkspaceExcludeFileSet exclude, VirtualFile file) {
        return WorkspaceFileKindMask.ALL & ~exclude.inPlaceComputeMasks(file, WorkspaceFileKindMask.ALL);
    }
}
