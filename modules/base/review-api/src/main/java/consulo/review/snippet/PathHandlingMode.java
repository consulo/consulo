// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.snippet;

import consulo.application.ReadAction;
import consulo.localize.LocalizeValue;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.review.localize.ReviewLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Ways to deal with paths for file names before creating a snippet.
 * <p>
 * When creating a snippet from $PROJECT_ROOT/sub/readme.md and $PROJECT_ROOT/sub/a/Main.java, one can choose
 * to have all names normalized to the nearest common parent directory ($PROJECT_ROOT/sub), or the project
 * root, or just leave out directories all together.
 * <p>
 * Example: Using RelativePaths as the setting should result in two files with names readme.md and a/Main.java
 * being a part of the snippet.
 */
public enum PathHandlingMode {
    /**
     * Uses file paths relative to the nearest common parent directory.
     */
    RelativePaths(ReviewLocalize.reviewSnippetCreatePathModeRelative(), ReviewLocalize.reviewSnippetCreatePathModeRelativeTooltip()),

    /**
     * Does not use file paths at all, only file names are used.
     */
    FlattenedPaths(ReviewLocalize.reviewSnippetCreatePathModeNone(), ReviewLocalize.reviewSnippetCreatePathModeNoneTooltip()),

    /**
     * Uses file paths relative to the project root.
     */
    ContentRootRelativePaths(
        ReviewLocalize.reviewSnippetCreatePathModeContentRootRelative(),
        ReviewLocalize.reviewSnippetCreatePathModeContentRootRelativeTooltip()
    ),

    /**
     * Uses file paths relative to the project root.
     */
    ProjectRelativePaths(
        ReviewLocalize.reviewSnippetCreatePathModeProjectRelative(),
        ReviewLocalize.reviewSnippetCreatePathModeProjectRelativeTooltip()
    );

    /**
     * Gets the name of a snippet entry directly from the file name.
     */
    private static final Function<VirtualFile, String> PATH_FROM_NAME = VirtualFile::getName;

    private final LocalizeValue myDisplayName;
    private final @Nullable LocalizeValue myTooltip;

    PathHandlingMode(LocalizeValue displayName, @Nullable LocalizeValue tooltip) {
        myDisplayName = displayName;
        myTooltip = tooltip;
    }

    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }

    public @Nullable LocalizeValue getTooltip() {
        return myTooltip;
    }

    /**
     * Gets the file name extractor function for the given {@link PathHandlingMode} using the given set of {@link VirtualFile files}.
     * If the list of files are empty, the name selector used should not matter (and no dialog should be opened anyway),
     * the default name selector is then returned, which is to just take the file name as snippet file name.
     */
    public static Function<VirtualFile, String> getFileNameExtractor(
        Project project,
        List<VirtualFile> files,
        PathHandlingMode pathHandlingMode
    ) {
        if (files.isEmpty()) {
            return PATH_FROM_NAME;
        }
        return switch (pathHandlingMode) {
            case RelativePaths -> pathFromNearestCommonAncestor(files);
            case ProjectRelativePaths -> pathFromProjectRoot(project);
            case ContentRootRelativePaths -> pathFromContentRoot(project);
            case FlattenedPaths -> PATH_FROM_NAME;
        };
    }

    /**
     * Gets the name of a snippet entry from the relative path from the content root of a file.
     */
    private static Function<VirtualFile, String> pathFromContentRoot(Project project) {
        ProjectFileIndex pfi = ProjectFileIndex.getInstance(project);
        return file -> {
            VirtualFile root = ReadAction.compute(() -> pfi.getContentRootForFile(file));
            String relativePath = root == null ? null : VirtualFileUtil.getRelativePath(file, root);
            return relativePath != null ? relativePath : file.getName();
        };
    }

    /**
     * Gets the name of a snippet entry from the relative path from the project root.
     */
    private static Function<VirtualFile, String> pathFromProjectRoot(Project project) {
        VirtualFile projectRoot = project.getBaseDir();
        if (projectRoot == null) {
            return PATH_FROM_NAME;
        }
        return file -> {
            String relativePath = VirtualFileUtil.getRelativePath(file, projectRoot);
            return relativePath != null ? relativePath : file.getName();
        };
    }

    /**
     * Gets the name of a snippet entry from the nearest common ancestor of all files (could be the file system root directory).
     *
     * @param files The collection of files from which snippet content is gathered. May never be empty.
     */
    private static Function<VirtualFile, String> pathFromNearestCommonAncestor(Collection<VirtualFile> files) {
        Iterator<VirtualFile> iterator = files.iterator();
        VirtualFile closestRoot = iterator.next().getParent();
        while (closestRoot != null && iterator.hasNext()) {
            closestRoot = VirtualFileUtil.getCommonAncestor(closestRoot, iterator.next());
        }
        VirtualFile root = closestRoot;
        return file -> {
            String relativePath = root == null ? null : VirtualFileUtil.getRelativePath(file, root);
            return relativePath != null ? relativePath : file.getName();
        };
    }
}
