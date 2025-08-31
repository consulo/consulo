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
package consulo.versionControlSystem;

import consulo.util.lang.Pair;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface VcsOutgoingChangesProvider<T extends CommittedChangeList> extends VcsProviderMarker {
  Pair<VcsRevisionNumber, List<T>> getOutgoingChanges(VirtualFile vcsRoot, boolean findRemote) throws VcsException;

  @Nullable
  VcsRevisionNumber getMergeBaseNumber(VirtualFile anyFileUnderRoot) throws VcsException;

  Collection<Change> filterLocalChangesBasedOnLocalCommits(Collection<Change> localChanges, VirtualFile vcsRoot) throws VcsException;

  @Nullable
  Date getRevisionDate(VcsRevisionNumber revision, FilePath file);
}
