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
package consulo.versionControlSystem.update;

import consulo.util.lang.Couple;

import java.util.List;
import java.util.function.Consumer;

public class UpdateFilesHelper {
  private UpdateFilesHelper() {
  }

  public static void iterateFileGroupFilesDeletedOnServerFirst(UpdatedFiles updatedFiles, Callback callback) {
    FileGroup changedOnServer = updatedFiles.getGroupById(FileGroup.CHANGED_ON_SERVER_ID);
    if (changedOnServer != null) {
      List<FileGroup> children = changedOnServer.getChildren();
      for (FileGroup child : children) {
        if (FileGroup.REMOVED_FROM_REPOSITORY_ID.equals(child.getId())) {
          iterateGroup(child, callback);
        }
      }
    }

    List<FileGroup> groups = updatedFiles.getTopLevelGroups();
    for (FileGroup group : groups) {
      iterateGroup(group, callback);

      for (FileGroup childGroup : group.getChildren()) {
        if (! FileGroup.REMOVED_FROM_REPOSITORY_ID.equals(childGroup.getId())) {
          iterateGroup(childGroup, callback);
        }
      }
    }
  }

  private static void iterateGroup(FileGroup group, Callback callback) {
    for (String file : group.getFiles()) {
      callback.onFile(file, group.getId());
    }
  }

  public static void iterateFileGroupFiles(UpdatedFiles updatedFiles, Callback callback) {
    List<FileGroup> groups = updatedFiles.getTopLevelGroups();
    for (FileGroup group : groups) {
      iterateGroup(group, callback);

      // for changed on server
      for (FileGroup childGroup : group.getChildren()) {
        iterateGroup(childGroup, callback);
      }
    }
  }

  private static void iterateGroup(FileGroup group, Consumer<Couple<String>> callback) {
    for (FileGroup.UpdatedFile updatedFile : group.getUpdatedFiles()) {
      callback.accept(Couple.of(updatedFile.getPath(), updatedFile.getVcsName()));
    }
  }

  public static void iterateAffectedFiles(UpdatedFiles updatedFiles, Consumer<Couple<String>> callback) {
    List<FileGroup> groups = updatedFiles.getTopLevelGroups();
    for (FileGroup group : groups) {
      iterateGroup(group, callback);

      // for changed on server
      for (FileGroup childGroup : group.getChildren()) {
        iterateGroup(childGroup, callback);
      }
    }
  }

  public interface Callback {
    void onFile(String filePath, String groupId);
  }
}
