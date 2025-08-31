/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import consulo.util.collection.MultiMap;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.FilePathComparator;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.FileHolder;
import consulo.versionControlSystem.change.VcsDirtyScope;
import consulo.versionControlSystem.change.VcsModifiableDirtyScope;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;

import java.util.*;

// true = recursively, branch name
public class SwitchedFileHolder implements FileHolder {
  private final Project myProject;
  private final HolderType myHolderType;
  private final ProjectLevelVcsManager myVcsManager;
  private final TreeMap<VirtualFile, Pair<Boolean, String>> myMap; // true = recursively, branch name

  public SwitchedFileHolder(Project project, HolderType holderType) {
    myProject = project;
    myHolderType = holderType;
    myVcsManager = ProjectLevelVcsManager.getInstance(project);
    myMap = new TreeMap<>(FilePathComparator.getInstance());
  }

  @Override
  public void cleanAll() {
    myMap.clear();
  }

  @Override
  public HolderType getType() {
    return myHolderType;
  }

  @Override
  public synchronized SwitchedFileHolder copy() {
    SwitchedFileHolder copyHolder = new SwitchedFileHolder(myProject, myHolderType);
    copyHolder.myMap.putAll(myMap);
    return copyHolder;
  }

  @Override
  public void cleanAndAdjustScope(VcsModifiableDirtyScope scope) {
    if (myProject.isDisposed()) return;
    Iterator<VirtualFile> iterator = myMap.keySet().iterator();
    while (iterator.hasNext()) {
      VirtualFile file = iterator.next();
      if (isFileDirty(scope, file)) {
        iterator.remove();
      }
    }
  }

  private boolean isFileDirty(VcsDirtyScope scope, VirtualFile file) {
    if (scope == null) return true;
    if (fileDropped(file)) return true;
    return scope.belongsTo(VcsUtil.getFilePath(file));
  }

  private boolean fileDropped(VirtualFile file) {
    return !file.isValid() || myVcsManager.getVcsFor(file) == null;
  }

  public Map<VirtualFile, String> getFilesMapCopy() {
    HashMap<VirtualFile, String> result = new HashMap<>();
    for (VirtualFile vf : myMap.keySet()) {
      result.put(vf, myMap.get(vf).getSecond());
    }
    return result;
  }

  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  @Nonnull
  public Collection<VirtualFile> values() {
    return myMap.keySet();
  }

  public void addFile(VirtualFile file, String branch, boolean recursive) {
    // without optimization here
    myMap.put(file, new Pair<>(recursive, branch));
  }

  public void removeFile(@Nonnull VirtualFile file) {
    myMap.remove(file);
  }

  public synchronized MultiMap<String, VirtualFile> getBranchToFileMap() {
    MultiMap<String, VirtualFile> result = new MultiMap<>();
    for (VirtualFile vf : myMap.keySet()) {
      result.putValue(myMap.get(vf).getSecond(), vf);
    }
    return result;
  }

  public synchronized boolean containsFile(@Nonnull VirtualFile file) {
    VirtualFile floor = myMap.floorKey(file);
    if (floor == null) return false;
    SortedMap<VirtualFile, Pair<Boolean, String>> floorMap = myMap.headMap(floor, true);
    for (VirtualFile parent : floorMap.keySet()) {
      if (VirtualFileUtil.isAncestor(parent, file, false)) {
        Pair<Boolean, String> value = floorMap.get(parent);
        return parent.equals(file) || value.getFirst();
      }
    }
    return false;
  }

  @Nullable
  public String getBranchForFile(VirtualFile file) {
    VirtualFile floor = myMap.floorKey(file);
    if (floor == null) return null;
    SortedMap<VirtualFile, Pair<Boolean, String>> floorMap = myMap.headMap(floor);
    for (VirtualFile parent : floorMap.keySet()) {
      if (VirtualFileUtil.isAncestor(parent, file, false)) {
        return floorMap.get(parent).getSecond();
      }
    }
    return null;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SwitchedFileHolder that = (SwitchedFileHolder)o;
    return Objects.equals(myMap.entrySet(), that.myMap.entrySet());
  }

  public int hashCode() {
    return myMap.hashCode();
  }

}
