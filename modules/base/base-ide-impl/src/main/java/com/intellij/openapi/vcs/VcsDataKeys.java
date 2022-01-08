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

package com.intellij.openapi.vcs;

import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangesSelection;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author yole
 */
public interface VcsDataKeys {
  Key<File[]> IO_FILE_ARRAY = Key.create("IO_FILE_ARRAY");
  Key<File> IO_FILE = Key.create("IO_FILE");
  Key<VcsKey> VCS = Key.create("VCS");
  Key<Boolean> VCS_NON_LOCAL_HISTORY_SESSION = Key.create("VCS_NON_LOCAL_HISTORY_SESSION");
  Key<VcsHistorySession> HISTORY_SESSION = Key.create("VCS_HISTORY_SESSION");
  Key<VcsFileRevision> VCS_FILE_REVISION = Key.create("VCS_FILE_REVISION");
  Key<VcsFileRevision[]> VCS_FILE_REVISIONS = Key.create("VCS_FILE_REVISIONS");
  Key<VirtualFile> VCS_VIRTUAL_FILE = Key.create("VCS_VIRTUAL_FILE");
  Key<FilePath> FILE_PATH = Key.create("FILE_PATH");
  Key<FilePath[]> FILE_PATH_ARRAY = Key.create("FILE_PATH_ARRAY");
  Key<Object> FILE_HISTORY_PANEL = Key.create("FILE_HISTORY_PANEL");
  Key<ChangeList[]> CHANGE_LISTS = Key.create("vcs.ChangeList");
  Key<Change> CURRENT_CHANGE = Key.create("vcs.CurrentChange");
  Key<Change[]> CHANGES = Key.create("vcs.Change");
  Key<ChangesSelection> CHANGES_SELECTION = Key.create("vcs.ChangesSelection");
  Key<Change[]> CHANGES_WITH_MOVED_CHILDREN = Key.create("ChangeListView.ChangesWithDetails");
  Key<Change[]> SELECTED_CHANGES_IN_DETAILS = Key.create("ChangeListView.SelectedChangesWithMovedSubtrees");
  Key<List<Change>> CHANGES_IN_LIST_KEY = Key.create("ChangeListView.ChangesInList");
  Key<List<VirtualFile>> MODIFIED_WITHOUT_EDITING_DATA_KEY = Key.create("ChangeListView.ModifiedWithoutEditing");
  Key<Boolean> HAVE_MODIFIED_WITHOUT_EDITING = Key.create("ChangeListView.HaveModifiedWithoutEditing");
  Key<Boolean> HAVE_LOCALLY_DELETED = Key.create("ChangeListView.HaveLocallyDeleted");
  Key<Change[]> SELECTED_CHANGES = Key.create("ChangeListView.SelectedChange");
  Key<Boolean> HAVE_SELECTED_CHANGES = Key.create("ChangeListView.HaveSelectedChanges");
  Key<Change[]> CHANGE_LEAD_SELECTION = Key.create("ChangeListView.ChangeLeadSelection");
  Key<FilePath> UPDATE_VIEW_SELECTED_PATH = Key.create("AbstractCommonUpdateAction.UpdateViewSelectedPath");
  Key<Iterable<Pair<FilePath, FileStatus>>> UPDATE_VIEW_FILES_ITERABLE = Key.create("AbstractCommonUpdateAction.UpdatedFilesIterable");
  Key<Object> LABEL_BEFORE = Key.create("LABEL_BEFORE");
  Key<Object> LABEL_AFTER = Key.create("LABEL_AFTER");
  Key<String> PRESET_COMMIT_MESSAGE = Key.create("PRESET_COMMIT_MESSAGE");
  Key<CommitMessageI> COMMIT_MESSAGE_CONTROL = Key.create("COMMIT_MESSAGE_CONTROL");
  Key<Consumer<String>> REMOTE_HISTORY_CHANGED_LISTENER = Key.create("REMOTE_HISTORY_CHANGED_LISTENER");
  Key<RepositoryLocation> REMOTE_HISTORY_LOCATION = Key.create("REMOTE_HISTORY_LOCATION");
  Key<VcsRevisionNumber> VCS_REVISION_NUMBER = Key.create("VCS_REVISION_NUMBER");
  Key<VcsRevisionNumber[]> VCS_REVISION_NUMBERS = Key.create("VCS_REVISION_NUMBERS");
  Key<VcsHistoryProvider> HISTORY_PROVIDER = Key.create("VCS_HISTORY_PROVIDER");
  Key<Stream<VirtualFile>> VIRTUAL_FILE_STREAM = Key.create("virtualFileStream");
}
