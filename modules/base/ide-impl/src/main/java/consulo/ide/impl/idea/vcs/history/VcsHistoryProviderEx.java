/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.history;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.history.VcsAppendableHistorySessionPartner;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsHistoryProvider;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface VcsHistoryProviderEx extends VcsHistoryProvider {
  @jakarta.annotation.Nullable
  VcsFileRevision getLastRevision(FilePath filePath) throws VcsException;

  void reportAppendableHistory(@Nonnull FilePath path,
                               @Nullable VcsRevisionNumber startingRevision,
                               @Nonnull VcsAppendableHistorySessionPartner partner) throws VcsException;
}
