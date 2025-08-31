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
package consulo.versionControlSystem.base;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * Utility class to sort changes by roots.
 *
 * @author irengrig
 * @author Kirill Likhodedov
 */
public class LocalChangesUnderRoots {
  private final ChangeListManager myChangeManager;
  private final ProjectLevelVcsManager myVcsManager;
  private VcsRoot[] myRoots;

  public LocalChangesUnderRoots(@Nonnull ChangeListManager changeListManager, @Nonnull ProjectLevelVcsManager projectLevelVcsManager) {
    myChangeManager = changeListManager;
    myVcsManager = projectLevelVcsManager;
  }

  public Map<String, Map<VirtualFile, Collection<Change>>> getChangesByLists(@Nonnull Collection<VirtualFile> rootsToSave) {
    Map<String, Map<VirtualFile, Collection<Change>>> result = new HashMap<>();
    myRoots = myVcsManager.getAllVcsRoots();

    List<LocalChangeList> changeLists = myChangeManager.getChangeListsCopy();
    for (LocalChangeList list : changeLists) {
      HashMap<VirtualFile, Collection<Change>> subMap = new HashMap<>();
      addChangesToMap(rootsToSave, subMap, list.getChanges());
      result.put(list.getName(), subMap);
    }
    return result;
  }

  /**
   * Sort all changes registered in the {@link ChangeListManager} by VCS roots,
   * filtering out any roots except the specified ones.
   * @param rootsToSave roots to search for changes only in them.
   * @return a map, whose keys are VCS roots (from the specified list) and values are {@link Change changes} from these roots.
   */
  @Nonnull
  public Map<VirtualFile, Collection<Change>> getChangesUnderRoots(@Nonnull Collection<VirtualFile> rootsToSave) {
    Map<VirtualFile, Collection<Change>> result = new HashMap<>();
    Collection<Change> allChanges = myChangeManager.getAllChanges();
    myRoots = myVcsManager.getAllVcsRoots();

    addChangesToMap(rootsToSave, result, allChanges);
    return result;
  }

  private void addChangesToMap(Collection<VirtualFile> rootsToSave,
                               Map<VirtualFile, Collection<Change>> result,
                               Collection<Change> allChanges) {
    for (Change change : allChanges) {
      if (change.getBeforeRevision() != null) {
        addChangeToMap(result, change, change.getBeforeRevision(), rootsToSave);
      }
      if (change.getAfterRevision() != null) {
        addChangeToMap(result, change, change.getAfterRevision(), rootsToSave);
      }
    }
  }

  private void addChangeToMap(@Nonnull Map<VirtualFile, Collection<Change>> result, @Nonnull Change change, @Nonnull ContentRevision revision, @Nonnull Collection<VirtualFile> rootsToSave) {
    VirtualFile root = getRootForPath(revision.getFile(), rootsToSave);
    addChangeToMap(result, root, change);
  }

  @Nullable
  private VirtualFile getRootForPath(@Nonnull FilePath file, @Nonnull Collection<VirtualFile> rootsToSave) {
    VirtualFile vf = ChangesUtil.findValidParentUnderReadAction(file);
    if (vf == null) {
      return null;
    }
    VirtualFile rootCandidate = null;
    for (VcsRoot root : myRoots) {
      if (VirtualFileUtil.isAncestor(root.getPath(), vf, false)) {
        if (rootCandidate == null || VirtualFileUtil.isAncestor(rootCandidate, root.getPath(), true)) { // in the case of nested roots choose the closest root
          rootCandidate = root.getPath();
        }
      }
    }
    if (! rootsToSave.contains(rootCandidate)) return null;
    return rootCandidate;
  }

  private static void addChangeToMap(@Nonnull Map<VirtualFile, Collection<Change>> result, @Nullable VirtualFile root, @Nonnull Change change) {
    if (root == null) {
      return;
    }
    Collection<Change> changes = result.get(root);
    if (changes == null) {
      changes = new HashSet<>();
      result.put(root, changes);
    }
    changes.add(change);
  }

}
