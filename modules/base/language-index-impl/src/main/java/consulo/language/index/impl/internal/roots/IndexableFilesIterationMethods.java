// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.application.ReadAction;
import consulo.application.util.registry.Registry;
import consulo.content.ContentIterator;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.util.HashSet;
import java.util.Set;

final class IndexableFilesIterationMethods {
    private static boolean followSymlinks() {
        return Registry.is("indexer.follows.symlinks");
    }

    static boolean iterateRoots(Project project, Iterable<VirtualFile> roots, ContentIterator contentIterator, VirtualFileFilter fileFilter) {
        return iterateRoots(project, roots, contentIterator, fileFilter, true);
    }

    static boolean iterateRoots(
        Project project,
        Iterable<VirtualFile> roots,
        ContentIterator contentIterator,
        VirtualFileFilter fileFilter,
        boolean excludeNonProjectRoots
    ) {
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
        Set<VirtualFile> rootsSet = new HashSet<>();
        for (VirtualFile root : roots) {
            rootsSet.add(root);
        }
        VirtualFileFilter finalFileFilter =
            file -> fileFilter.accept(file) && shouldIndexFile(file, projectFileIndex, rootsSet, excludeNonProjectRoots);
        for (VirtualFile root : roots) {
            if (!VirtualFileUtil.iterateChildrenRecursively(root, finalFileFilter, contentIterator)) {
                return false;
            }
        }
        return true;
    }

    private static boolean shouldIndexFile(
        VirtualFile file,
        ProjectFileIndex projectFileIndex,
        Set<VirtualFile> rootsSet,
        boolean excludeNonProjectRoots
    ) {
        if (file.is(VFileProperty.SYMLINK)) {
            if (!followSymlinks()) {
                return false;
            }
            VirtualFile targetFile = file.getCanonicalFile();
            if (targetFile == null || targetFile.is(VFileProperty.SYMLINK)) {
                // Broken or recursive symlink. The second check should not happen but let's guarantee no StackOverflowError.
                return false;
            }
            if (rootsSet.contains(file)) {
                return true;
            }
            return shouldIndexFile(targetFile, projectFileIndex, rootsSet, excludeNonProjectRoots);
        }
        if (!(file instanceof VirtualFileWithId virtualFileWithId) || virtualFileWithId.getId() <= 0) {
            return false;
        }
        if (excludeNonProjectRoots && ReadAction.compute(() -> projectFileIndex.isExcluded(file))) {
            return false;
        }
        return true;
    }
}
