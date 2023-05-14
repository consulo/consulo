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
package consulo.versionControlSystem.diff;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsProviderMarker;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.history.VcsRevisionDescription;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nullable;

public interface DiffProvider extends VcsProviderMarker {

  @Nullable
  VcsRevisionNumber getCurrentRevision(VirtualFile file);

  @Nullable
  ItemLatestState getLastRevision(VirtualFile virtualFile);

  @Nullable
  ItemLatestState getLastRevision(final FilePath filePath);

  @Nullable
  ContentRevision createFileContent(VcsRevisionNumber revisionNumber, VirtualFile selectedFile);

  @Nullable
  VcsRevisionNumber getLatestCommittedRevision(VirtualFile vcsRoot);

  @Nullable
  default VcsRevisionDescription getCurrentRevisionDescription(final VirtualFile file) {
    return null;
  }
}
