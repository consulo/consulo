/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.roots;

import consulo.annotation.component.ServiceImpl;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.VcsRootChecker;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.versionControlSystem.root.VcsRootDetector;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author Nadya Zabrodina
 */
@Singleton
@ServiceImpl
public class VcsRootDetectorImpl implements VcsRootDetector {
  private static final int MAXIMUM_SCAN_DEPTH = 2;

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final ProjectRootManager myProjectManager;
  @Nonnull
  private final ProjectLevelVcsManager myVcsManager;
  @Nonnull
  private final List<VcsRootChecker> myCheckers;

  @Inject
  public VcsRootDetectorImpl(@Nonnull Project project,
                             @Nonnull ProjectRootManager projectRootManager,
                             @Nonnull ProjectLevelVcsManager projectLevelVcsManager) {
    myProject = project;
    myProjectManager = projectRootManager;
    myVcsManager = projectLevelVcsManager;
    myCheckers = VcsRootChecker.EXTENSION_POINT_NAME.getExtensionList();
  }

  @Nonnull
  public Collection<VcsRoot> detect() {
    return detect(myProject.getBaseDir());
  }

  @Nonnull
  public Collection<VcsRoot> detect(@Nullable VirtualFile startDir) {
    if (startDir == null || myCheckers.isEmpty()) {
      return Collections.emptyList();
    }

    final Set<VcsRoot> roots = scanForRootsInsideDir(startDir);
    roots.addAll(scanForRootsInContentRoots());
    for (VcsRoot root : roots) {
      if (startDir.equals(root.getPath())) {
        return roots;
      }
    }
    List<VcsRoot> rootsAbove = scanForSingleRootAboveDir(startDir);
    roots.addAll(rootsAbove);
    return roots;
  }

  @Nonnull
  private Set<VcsRoot> scanForRootsInContentRoots() {
    Set<VcsRoot> vcsRoots = new HashSet<VcsRoot>();
    VirtualFile[] roots = myProjectManager.getContentRoots();
    for (VirtualFile contentRoot : roots) {

      Set<VcsRoot> rootsInsideRoot = scanForRootsInsideDir(contentRoot);
      boolean shouldScanAbove = true;
      for (VcsRoot root : rootsInsideRoot) {
        if (contentRoot.equals(root.getPath())) {
          shouldScanAbove = false;
        }
      }
      if (shouldScanAbove) {
        List<VcsRoot> rootsAbove = scanForSingleRootAboveDir(contentRoot);
        rootsInsideRoot.addAll(rootsAbove);
      }
      vcsRoots.addAll(rootsInsideRoot);
    }
    return vcsRoots;
  }

  @Nonnull
  private Set<VcsRoot> scanForRootsInsideDir(@Nonnull final VirtualFile dir, final int depth) {
    final Set<VcsRoot> roots = new HashSet<VcsRoot>();
    if (depth > MAXIMUM_SCAN_DEPTH) {
      // performance optimization via limitation: don't scan deep though the whole VFS, 2 levels under a content root is enough
      return roots;
    }

    if (myProject.isDisposed() || !dir.isDirectory()) {
      return roots;
    }
    List<AbstractVcs> vcsList = getVcsListFor(dir);
    for (AbstractVcs vcs : vcsList) {
      roots.add(new VcsRoot(vcs, dir));
    }
    for (VirtualFile child : dir.getChildren()) {
      roots.addAll(scanForRootsInsideDir(child, depth + 1));
    }
    return roots;
  }

  @Nonnull
  private Set<VcsRoot> scanForRootsInsideDir(@Nonnull VirtualFile dir) {
    return scanForRootsInsideDir(dir, 0);
  }

  @Nonnull
  private List<VcsRoot> scanForSingleRootAboveDir(@Nonnull final VirtualFile dir) {
    List<VcsRoot> roots = new ArrayList<VcsRoot>();
    if (myProject.isDisposed()) {
      return roots;
    }

    VirtualFile par = dir.getParent();
    while (par != null) {
      List<AbstractVcs> vcsList = getVcsListFor(par);
      for (AbstractVcs vcs : vcsList) {
        roots.add(new VcsRoot(vcs, par));
      }
      if (!roots.isEmpty()) {
        return roots;
      }
      par = par.getParent();
    }
    return roots;
  }

  @Nonnull
  private List<AbstractVcs> getVcsListFor(@Nonnull VirtualFile dir) {
    List<AbstractVcs> vcsList = new ArrayList<AbstractVcs>();
    for (VcsRootChecker checker : myCheckers) {
      if (checker.isRoot(dir.getPath())) {
        vcsList.add(myVcsManager.findVcsByName(checker.getSupportedVcs().getName()));
      }
    }
    return vcsList;
  }
}
