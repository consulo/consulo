/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.changes.ui;

import com.intellij.openapi.vcs.changes.shelf.ShelvedBinaryFile;
import com.intellij.openapi.vcs.changes.shelf.ShelvedChange;
import com.intellij.openapi.vcs.changes.shelf.ShelvedChangeList;
import javax.annotation.Nonnull;

import java.util.List;

public class ShelvedChangeListDragBean {
  @Nonnull
  private List<ShelvedChange> myShelvedChanges;
  @Nonnull
  private List<ShelvedBinaryFile> myBinaries;
  @Nonnull
  private List<ShelvedChangeList> myShelvedChangelists;

  public ShelvedChangeListDragBean(@Nonnull List<ShelvedChange> shelvedChanges,
                                   @Nonnull List<ShelvedBinaryFile> binaries,
                                   @Nonnull List<ShelvedChangeList> shelvedChangelists) {
    myShelvedChanges = shelvedChanges;
    myBinaries = binaries;
    myShelvedChangelists = shelvedChangelists;
  }

  @Nonnull
  public List<ShelvedChange> getChanges() {
    return myShelvedChanges;
  }

  @Nonnull
  public List<ShelvedBinaryFile> getBinaryFiles() {
    return myBinaries;
  }

  @Nonnull
  public List<ShelvedChangeList> getShelvedChangelists() {
    return myShelvedChangelists;
  }
}
