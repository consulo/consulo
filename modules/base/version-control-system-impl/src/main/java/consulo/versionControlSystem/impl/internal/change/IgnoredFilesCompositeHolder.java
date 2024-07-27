/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.FileHolder;
import consulo.versionControlSystem.change.IgnoredFilesHolder;
import consulo.versionControlSystem.change.VcsIgnoredFilesHolder;
import consulo.versionControlSystem.change.VcsModifiableDirtyScope;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class IgnoredFilesCompositeHolder implements IgnoredFilesHolder {
  private final Map<AbstractVcs, IgnoredFilesHolder> myVcsIgnoredHolderMap;
  private IgnoredFilesHolder myIdeIgnoredFilesHolder;
  private final Project myProject;
  private AbstractVcs myCurrentVcs;
  private final ProjectLevelVcsManager myVcsManager;

  public IgnoredFilesCompositeHolder(final Project project) {
    super();
    myProject = project;
    myVcsIgnoredHolderMap = new HashMap<>();
    myIdeIgnoredFilesHolder = new RecursiveFilePathHolderImpl(myProject, HolderType.IGNORED);
    myVcsManager = ProjectLevelVcsManager.getInstance(myProject);
  }

  @Override
  public void cleanAll() {
    myVcsIgnoredHolderMap.values().forEach(IgnoredFilesHolder::cleanAll);
    myVcsIgnoredHolderMap.clear();
    myIdeIgnoredFilesHolder.cleanAll();
  }

  @Override
  public void cleanAndAdjustScope(VcsModifiableDirtyScope scope) {
    final AbstractVcs vcs = scope.getVcs();
    if (myVcsIgnoredHolderMap.containsKey(vcs)) {
      myVcsIgnoredHolderMap.get(vcs).cleanAndAdjustScope(scope);
    }
    myIdeIgnoredFilesHolder.cleanAndAdjustScope(scope);
  }

  @Override
  public FileHolder copy() {
    final IgnoredFilesCompositeHolder result = new IgnoredFilesCompositeHolder(myProject);
    for (Map.Entry<AbstractVcs, IgnoredFilesHolder> entry : myVcsIgnoredHolderMap.entrySet()) {
      result.myVcsIgnoredHolderMap.put(entry.getKey(), (IgnoredFilesHolder)entry.getValue().copy());
    }
    result.myIdeIgnoredFilesHolder = (IgnoredFilesHolder)myIdeIgnoredFilesHolder.copy();
    return result;
  }

  @Override
  public HolderType getType() {
    return HolderType.IGNORED;
  }

  @Override
  public void addFile(VirtualFile file) {
    myIdeIgnoredFilesHolder.addFile(file);
  }

  @Override
  public void addFile(FilePath filePath) {
    myIdeIgnoredFilesHolder.addFile(filePath);
  }

  public boolean isInUpdatingMode() {
    return myVcsIgnoredHolderMap.values()
                                .stream()
                                .anyMatch((holder) -> (holder instanceof VcsIgnoredFilesHolder) && ((VcsIgnoredFilesHolder)holder).isInUpdatingMode());
  }

  @Override
  public boolean containsFile(VirtualFile file) {
    if (myIdeIgnoredFilesHolder.containsFile(file)) return true;
    final AbstractVcs vcs = myVcsManager.getVcsFor(file);
    if (vcs == null) return false;
    final IgnoredFilesHolder ignoredFilesHolder = myVcsIgnoredHolderMap.get(vcs);
    return ignoredFilesHolder != null && ignoredFilesHolder.containsFile(file);
  }

  public boolean containsFile(FilePath file, VcsRoot vcsRoot) {
    // TODO incorrect
    VirtualFile root = myVcsManager.getVcsRootFor(file);
    return containsFile(file, root);
  }

  @Override
  public boolean containsFile(FilePath file, VirtualFile vcsRoot) {
    if (myIdeIgnoredFilesHolder.containsFile(file, vcsRoot)) return true;
    final AbstractVcs vcs = myVcsManager.getVcsFor(vcsRoot);
    if (vcs == null) return false;
    final IgnoredFilesHolder ignoredFilesHolder = myVcsIgnoredHolderMap.get(vcs);
    return ignoredFilesHolder != null && ignoredFilesHolder.containsFile(file, vcsRoot);
  }

  @Override
  public Collection<VirtualFile> values() {
    final HashSet<VirtualFile> result = new HashSet<>();
    result.addAll(myIdeIgnoredFilesHolder.values());
    result.addAll(myVcsIgnoredHolderMap.values()
                                       .stream()
                                       .map(IgnoredFilesHolder::values)
                                       .flatMap(set -> set.stream())
                                       .collect(Collectors.toSet()));
    return result;
  }

  @Override
  public void notifyVcsStarted(AbstractVcs vcs) {
    myCurrentVcs = vcs;
    if (myVcsIgnoredHolderMap.containsKey(vcs)) return;

    IgnoredFilesHolder ignoredFilesHolder =
      ObjectUtil.chooseNotNull(getHolderFromEP(vcs, myProject), new RecursiveFilePathHolderImpl(myProject, HolderType.IGNORED));
    ignoredFilesHolder.notifyVcsStarted(vcs);
    myVcsIgnoredHolderMap.put(vcs, ignoredFilesHolder);
  }

  @Nullable
  public IgnoredFilesHolder getActiveVcsHolder() {
    return getIgnoredHolderByVcs(myCurrentVcs);
  }

  @Nullable
  private IgnoredFilesHolder getIgnoredHolderByVcs(AbstractVcs vcs) {
    if (!myVcsIgnoredHolderMap.containsKey(vcs)) return null;
    return myVcsIgnoredHolderMap.get(vcs);
  }


  @Nullable
  private static VcsIgnoredFilesHolder getHolderFromEP(AbstractVcs vcs, @Nonnull Project project) {
    Optional<VcsIgnoredFilesHolder> ignoredFilesHolder = VcsIgnoredFilesHolder.VCS_IGNORED_FILES_HOLDER_EP.getExtensionList(project)
                                                                                                          .stream()
                                                                                                          .filter(holder -> holder.getVcs()
                                                                                                                                  .equals(
                                                                                                                                    vcs))
                                                                                                          .findFirst();
    return ignoredFilesHolder.isPresent() ? ignoredFilesHolder.get() : null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof IgnoredFilesCompositeHolder)) {
      return false;
    }
    IgnoredFilesCompositeHolder other = (IgnoredFilesCompositeHolder)obj;
    return myVcsIgnoredHolderMap.equals(other.myVcsIgnoredHolderMap) && myIdeIgnoredFilesHolder.equals(other.myIdeIgnoredFilesHolder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myVcsIgnoredHolderMap, myIdeIgnoredFilesHolder);
  }
}
