// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal;

import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.versionControlSystem.VcsRootChecker;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class VcsDefaultMappingDetector {
  private final Project myProject;
  private final VcsRootChecker myRootChecker;
  private final ProjectFileIndex myFileIndex;

  private final Map<VirtualFile, Boolean> myCheckedDirs = new HashMap<>();

  VcsDefaultMappingDetector(Project project, VcsRootChecker rootChecker) {
    myProject = project;
    myRootChecker = rootChecker;
    myFileIndex = ProjectFileIndex.getInstance(project);
  }

  Set<VirtualFile> detectProjectMappings(Collection<VirtualFile> projectRoots, Set<VirtualFile> mappedDirs) {
    for (VirtualFile dir : mappedDirs) {
      myCheckedDirs.put(dir, true);
    }

    Set<VirtualFile> vcsRoots = new LinkedHashSet<>();
    for (VirtualFile projectRoot : projectRoots) {
      VirtualFile root = detectVcsForProjectRoot(projectRoot);
      if (root != null) {
        vcsRoots.add(root);
      }
    }

    vcsRoots.removeAll(mappedDirs); // do not report known mappings
    return vcsRoots;
  }

  private @Nullable VirtualFile detectVcsForProjectRoot(VirtualFile projectRoot) {
    for (VirtualFile file = projectRoot; file != null; file = file.getParent()) {
      if (isVcsRoot(file)) {
        return file;
      }

      VirtualFile parent = file.getParent();
      if (parent != null && !isUnderProject(parent)) {
        if (myRootChecker.areChildrenValidMappings() &&
            isUnderVcsRoot(parent)) {
          return file;
        }
        else {
          return null;
        }
      }
    }

    return null;
  }

  private boolean isUnderVcsRoot(VirtualFile file) {
    for (VirtualFile current = file; current != null; current = current.getParent()) {
      if (isVcsRoot(current)) {
        return true;
      }
    }
    return false;
  }

  private boolean isVcsRoot(VirtualFile file) {
    ProgressManager.checkCanceled();
    return myCheckedDirs.computeIfAbsent(file, key -> myRootChecker.isRoot(key));
  }

  private boolean isUnderProject(VirtualFile f) {
    return ReadAction.compute(() -> {
      if (myProject.isDisposed()) {
        throw new ProcessCanceledException();
      }
      return myFileIndex.isInContent(f);
    });
  }
}
