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
package consulo.versionControlSystem.change;

import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author max
 */
public class DeletedFilesHolder implements FileHolder {
  private final Map<String, LocallyDeletedChange> myFiles = new HashMap<String, LocallyDeletedChange>();

  public void cleanAll() {
    myFiles.clear();
  }
  
  public void takeFrom(DeletedFilesHolder holder) {
    myFiles.clear();
    myFiles.putAll(holder.myFiles);
  }

  public void cleanAndAdjustScope(VcsModifiableDirtyScope scope) {
    if (scope == null) {
      myFiles.clear();
    }
    List<LocallyDeletedChange> currentFiles = new ArrayList<LocallyDeletedChange>(myFiles.values());
    for (LocallyDeletedChange change : currentFiles) {
      if (scope.belongsTo(change.getPath())) {
        myFiles.remove(change.getPresentableUrl());
      }
    }
  }

  public HolderType getType() {
    return HolderType.DELETED;
  }

  @Override
  public void notifyVcsStarted(AbstractVcs scope) {
  }

  public void addFile(LocallyDeletedChange change) {
    myFiles.put(change.getPresentableUrl(), change);
  }

  public List<LocallyDeletedChange> getFiles() {
    return new ArrayList<LocallyDeletedChange>(myFiles.values());
  }

  public boolean isContainedInLocallyDeleted(FilePath filePath) {
    String url = filePath.getPresentableUrl();
    return myFiles.containsKey(url);
  }

  public DeletedFilesHolder copy() {
    DeletedFilesHolder copyHolder = new DeletedFilesHolder();
    copyHolder.myFiles.putAll(myFiles);
    return copyHolder;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DeletedFilesHolder that = (DeletedFilesHolder)o;

    if (!myFiles.equals(that.myFiles)) return false;

    return true;
  }

  public int hashCode() {
    return myFiles.hashCode();
  }
}
