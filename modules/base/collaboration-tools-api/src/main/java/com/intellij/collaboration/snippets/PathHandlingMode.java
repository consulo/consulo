// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.snippets;

import consulo.application.ReadAction;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Ways to deal with paths for file names before creating a snippet.
 */
public enum PathHandlingMode {
    /**
     * Uses file paths relative to the nearest common parent directory.
     */
    RelativePaths(
        CollaborationToolsLocalize.snippetCreatePathModeRelative().get(),
        CollaborationToolsLocalize.snippetCreatePathModeRelativeTooltip().get()
    ),

    /**
     * Does not use file paths at all, only file names are used.
     */
    FlattenedPaths(
        CollaborationToolsLocalize.snippetCreatePathModeNone().get(),
        CollaborationToolsLocalize.snippetCreatePathModeNoneTooltip().get()
    ),

    /**
     * Uses file paths relative to the content root.
     */
    ContentRootRelativePaths(
        CollaborationToolsLocalize.snippetCreatePathModeContentRootRelative().get(),
        CollaborationToolsLocalize.snippetCreatePathModeContentRootRelativeTooltip().get()
    ),

    /**
     * Uses file paths relative to the project root.
     */
    ProjectRelativePaths(
        CollaborationToolsLocalize.snippetCreatePathModeProjectRelative().get(),
        CollaborationToolsLocalize.snippetCreatePathModeProjectRelativeTooltip().get()
    );

    @Nonnull
    private final @Nls String displayName;
    @Nullable
    private final @Nls String tooltip;

    PathHandlingMode(@Nonnull @Nls String displayName, @Nullable @Nls String tooltip) {
        this.displayName = displayName;
        this.tooltip = tooltip;
    }

    public @Nonnull @Nls String getDisplayName() {
        return displayName;
    }

    public @Nullable @Nls String getTooltip() {
        return tooltip;
    }

    /**
     * Gets the file name extractor function for the given {@link PathHandlingMode} using the given set of files.
     */
    public static @Nonnull Function<VirtualFile, String> getFileNameExtractor(
        @Nonnull Project project,
        @Nonnull List<VirtualFile> files,
        @Nonnull PathHandlingMode pathHandlingMode
    ) {
        if (files.isEmpty()) {
            return VirtualFile::getName;
        }
        return switch (pathHandlingMode) {
            case RelativePaths -> pathFromNearestCommonAncestor(files);
            case ProjectRelativePaths -> pathFromProjectRoot(project);
            case ContentRootRelativePaths -> pathFromContentRoot(project);
            case FlattenedPaths -> VirtualFile::getName;
        };
    }

    private static @Nonnull Function<VirtualFile, String> pathFromContentRoot(@Nonnull Project project) {
        ProjectFileIndex pfi = ProjectFileIndex.getInstance(project);
        return file -> {
            VirtualFile root = ReadAction.compute(() -> pfi.getContentRootForFile(file));
            if (root != null) {
                String relativePath = VfsUtilCore.getRelativePath(file, root);
                if (relativePath != null) {
                    return relativePath;
                }
            }
            return file.getName();
        };
    }

    private static @Nonnull Function<VirtualFile, String> pathFromProjectRoot(@Nonnull Project project) {
        VirtualFile projectRoot = ProjectUtil.guessProjectDir(project);
        if (projectRoot == null) {
            return VirtualFile::getName;
        }
        return file -> {
            String relativePath = VfsUtilCore.getRelativePath(file, projectRoot);
            return relativePath != null ? relativePath : file.getName();
        };
    }

    private static @Nonnull Function<VirtualFile, String> pathFromNearestCommonAncestor(@Nonnull Collection<VirtualFile> files) {
        Iterator<VirtualFile> iterator = files.iterator();
        VirtualFile closestRoot = iterator.next().getParent();
        while (iterator.hasNext()) {
            closestRoot = VfsUtilCore.getCommonAncestor(closestRoot, iterator.next());
        }
        VirtualFile root = closestRoot;
        return file -> {
            String relativePath = VfsUtilCore.getRelativePath(file, root);
            return relativePath != null ? relativePath : file.getName();
        };
    }
}
