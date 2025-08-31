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
package consulo.versionControlSystem.impl.internal.update;

import consulo.versionControlSystem.update.FileGroup;
import consulo.versionControlSystem.update.UpdatedFiles;
import consulo.virtualFileSystem.VirtualFile;

import java.util.*;

public class UpdatedFilesReverseSide {
  // just list of same groups = another presentation/container of same
  private final UpdatedFiles myFiles;

  // children are also here
  private final Map<String, FileGroup> myGroupHolder;

  // file path, group
  private final Map<String, FileGroup> myFileIdx;

  private final static List<String> ourStoppingGroups = Arrays.asList(
      FileGroup.MERGED_WITH_CONFLICT_ID, FileGroup.UNKNOWN_ID, FileGroup.SKIPPED_ID);

  public UpdatedFilesReverseSide(UpdatedFiles files) {
    myFiles = files;
    myGroupHolder = new HashMap<String, FileGroup>();
    myFileIdx = new HashMap<String, FileGroup>();
  }

  public boolean isEmpty() {
    return myFileIdx.isEmpty();
  }

  public FileGroup getGroup(String id) {
    return myGroupHolder.get(id);
  }

  public void addFileToGroup(String groupId, String file, DuplicateLevel duplicateLevel, String vcsName) {
    FileGroup newGroup = myGroupHolder.get(groupId);
    addFileToGroup(newGroup, file, duplicateLevel, vcsName);
  }

  public void addFileToGroup(FileGroup group, String file, DuplicateLevel duplicateLevel, String vcsName) {
    if (duplicateLevel.searchPreviousContainment(group.getId())) {
      FileGroup oldGroup = myFileIdx.get(file);
      if (oldGroup != null) {
        if (duplicateLevel.doesExistingWin(group.getId(), oldGroup.getId())) {
          return;
        }
        oldGroup.remove(file);
      }
    }

    group.add(file, vcsName, null);
    myFileIdx.put(file, group);
  }

  public UpdatedFiles getUpdatedFiles() {
    return myFiles;
  }

  public void rebuildFromUpdatedFiles() {
    myFileIdx.clear();
    myGroupHolder.clear();
    
    for (FileGroup group : myFiles.getTopLevelGroups()) {
      addGroupToIndexes(group);
    }
  }

  private void addGroupToIndexes(FileGroup fromGroup) {
    myGroupHolder.put(fromGroup.getId(), fromGroup);

    for (String file : fromGroup.getFiles()) {
      myFileIdx.put(file, fromGroup);
    }

    for (FileGroup fromChild : fromGroup.getChildren()) {
      addGroupToIndexes(fromChild);
    }
  }

  private void copyGroup(Parent parent, FileGroup from, DuplicateLevel duplicateLevel) {
    FileGroup to = createOrGet(parent, from);

    for (FileGroup.UpdatedFile updatedFile : from.getUpdatedFiles()) {
      addFileToGroup(to, updatedFile.getPath(), duplicateLevel, updatedFile.getVcsName());
    }
    for (FileGroup fromChild : from.getChildren()) {
      copyGroup(new GroupParent(to), fromChild, duplicateLevel);
    }
  }

  private interface Parent {
    void accept(FileGroup group);
  }

  private class TopLevelParent implements Parent {
    public void accept(FileGroup group) {
      myFiles.getTopLevelGroups().add(group);
    }
  }

  private static class GroupParent implements Parent {
    private final FileGroup myGroup;

    private GroupParent(FileGroup group) {
      myGroup = group;
    }

    public void accept(FileGroup group) {
      myGroup.addChild(group);
    }
  }

  private FileGroup createOrGet(Parent possibleParent, FileGroup fromGroup) {
    FileGroup ownGroup = myGroupHolder.get(fromGroup.getId());
    if (ownGroup == null) {
      ownGroup = new FileGroup(fromGroup.getUpdateName(), fromGroup.getStatusName(), fromGroup.getSupportsDeletion(),
                               fromGroup.getId(), fromGroup.myCanBeAbsent);
      possibleParent.accept(ownGroup);
      myGroupHolder.put(fromGroup.getId(), ownGroup);
    }
    return ownGroup;
  }

  public static Set<String> getPathsFromUpdatedFiles(UpdatedFiles from) {
    UpdatedFilesReverseSide helper = new UpdatedFilesReverseSide(UpdatedFiles.create());
    helper.accomulateFiles(from, DuplicateLevel.DUPLICATE_ERRORS);
    return helper.myFileIdx.keySet();
  }

  public void accomulateFiles(UpdatedFiles from, DuplicateLevel duplicateLevel) {
    Parent topLevel = new TopLevelParent();
    for (FileGroup fromGroup : from.getTopLevelGroups()) {
      copyGroup(topLevel, fromGroup, duplicateLevel);
    }
  }

  public boolean containErrors() {
    return containErrors(myFiles);
  }

  public static boolean containErrors(UpdatedFiles files) {
    for (String groupId : ourStoppingGroups) {
      FileGroup group = files.getGroupById(groupId);
      if ((group != null) && (! group.isEmpty())) {
        return true;
      }
    }
    return false;
  }

  public boolean containsFile(VirtualFile file) {
    return myFileIdx.containsKey(file.getPresentableUrl());
  }

  public abstract static class DuplicateLevel {
    private final static List<String> ourErrorGroups = Arrays.asList(FileGroup.UNKNOWN_ID, FileGroup.SKIPPED_ID);
    private final static List<String> ourLocals = Arrays.asList(FileGroup.LOCALLY_ADDED_ID, FileGroup.LOCALLY_REMOVED_ID);

    abstract boolean searchPreviousContainment(String groupId);
    abstract boolean doesExistingWin(String groupId, String existingGroupId);

    private DuplicateLevel() {
    }

    public static final DuplicateLevel NO_DUPLICATES = new DuplicateLevel() {
      boolean searchPreviousContainment(String groupId) {
        return true;
      }

      boolean doesExistingWin(String groupId, String existingGroupId) {
        return false;
      }
    };
    public static final DuplicateLevel DUPLICATE_ERRORS_LOCALS = new DuplicateLevel() {
      boolean searchPreviousContainment(String groupId) {
        return (! ourLocals.contains(groupId)) && (! ourErrorGroups.contains(groupId));
      }

      boolean doesExistingWin(String groupId, String existingGroupId) {
        return ourLocals.contains(groupId);
      }
    };

    public static final DuplicateLevel DUPLICATE_ERRORS = new DuplicateLevel() {
      boolean searchPreviousContainment(String groupId) {
        return ! ourErrorGroups.contains(groupId);
      }

      boolean doesExistingWin(String groupId, String existingGroupId) {
        return false;
      }
    };
    public static final DuplicateLevel ALLOW_DUPLICATES = new DuplicateLevel() {
      boolean searchPreviousContainment(String groupId) {
        return false;
      }

      boolean doesExistingWin(String groupId, String existingGroupId) {
        return false;
      }
    };
  }
}
