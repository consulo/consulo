/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.changes.committed;

import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.util.text.StringUtil.join;
import static com.intellij.util.containers.ContainerUtil.addAll;
import static com.intellij.util.containers.ContainerUtil.newArrayList;
import static com.intellij.vcsUtil.VcsUtil.getFilePath;
import static java.util.function.Function.identity;

public class RootsCalculator {
  private final static Logger LOG = Logger.getInstance(RootsCalculator.class);

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final AbstractVcs<?> myVcs;
  @Nonnull
  private final ProjectLevelVcsManager myPlManager;
  @Nonnull
  private final RepositoryLocationCache myLocationCache;

  public RootsCalculator(@Nonnull Project project, @Nonnull AbstractVcs vcs, @Nonnull RepositoryLocationCache locationCache) {
    myProject = project;
    myLocationCache = locationCache;
    myPlManager = ProjectLevelVcsManager.getInstance(myProject);
    myVcs = vcs;
  }

  @Nonnull
  public Map<VirtualFile, RepositoryLocation> getRoots() {
    LOG.debug("Collecting roots for " + myVcs);
    // TODO: It is not quite clear why using just ProjectLevelVcsManager.getRootsUnderVcs() is not sufficient
    List<VirtualFile> roots = getRootsFromMappings();
    addAll(roots, myPlManager.getRootsUnderVcs(myVcs));

    logRoots("Candidates", roots);

    roots.removeIf(file -> getLocation(file) == null);

    logRoots("Candidates with repository location", roots);

    List<VirtualFile> list = myVcs.filterUniqueRoots(roots, identity());
    Map<VirtualFile, RepositoryLocation> map = new LinkedHashMap<>();
    for (VirtualFile file : list) {
      if (map.containsKey(file)) {
        continue;
      }

      RepositoryLocation location = getLocation(file);
      if (location != null) {
        map.put(file, location);
      }
    }
    logRoots("Unique roots", map.keySet());
    return map;
  }

  @Nonnull
  private List<VirtualFile> getRootsFromMappings() {
    List<VirtualFile> result = newArrayList();

    for (VcsDirectoryMapping mapping : myPlManager.getDirectoryMappings(myVcs)) {
      if (mapping.isDefaultMapping()) {
        if (myVcs.equals(myPlManager.getVcsFor(myProject.getBaseDir()))) {
          result.add(myProject.getBaseDir());
        }
      }
      else {
        VirtualFile newFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(mapping.getDirectory());
        if (newFile != null) {
          result.add(newFile);
        }
        else {
          LOG.info("Can not find virtual file for root: " + mapping.getDirectory());
        }
      }
    }

    return result;
  }

  @javax.annotation.Nullable
  private RepositoryLocation getLocation(@Nonnull VirtualFile file) {
    return myLocationCache.getLocation(myVcs, getFilePath(file), false);
  }

  private static void logRoots(@Nonnull String prefix, @Nonnull Collection<VirtualFile> roots) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(prefix + ": " + join(roots, VirtualFile::getPath, ", "));
    }
  }
}
