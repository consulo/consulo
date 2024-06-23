/*
 * Copyright 2013-2024 consulo.io
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

import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.virtualFileSystem.status.FileStatus;

/**
 * @author VISTALL
 * @since 23-Jun-24
 */
public class ChangeListChange extends Change {
  private final Change change;
  private final String changeListName;
  private final String changeListId;

  public ChangeListChange(ContentRevision beforeRevision,
                          ContentRevision afterRevision,
                          FileStatus fileStatus,
                          Change change,
                          String changeListName,
                          String changeListId) {
    super(beforeRevision, afterRevision, fileStatus);
    this.change = change;
    this.changeListName = changeListName;
    this.changeListId = changeListId;
    copyFieldsFrom(change);
  }

  public ChangeListChange(Change change, String changeListName, String changeListId) {
    this(change.getBeforeRevision(), change.getAfterRevision(), change.getFileStatus(),
         change, changeListName, changeListId);
  }

  public Change getChange() {
    return change;
  }

  public String getChangeListName() {
    return changeListName;
  }

  public String getChangeListId() {
    return changeListId;
  }
}
