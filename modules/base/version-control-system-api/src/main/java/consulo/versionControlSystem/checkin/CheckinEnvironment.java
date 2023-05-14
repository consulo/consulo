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
package consulo.versionControlSystem.checkin;

import consulo.util.lang.function.PairConsumer;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsProviderMarker;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Interface for performing VCS checkin / commit / submit operations.
 *
 * @author lesya
 * @see AbstractVcs#getCheckinEnvironment()
 */
public interface CheckinEnvironment extends VcsProviderMarker {
  @Nullable
  RefreshableOnComponent createAdditionalOptionsPanel(CheckinProjectPanel panel, PairConsumer<Object, Object> additionalDataConsumer);

  @Nullable
  String getDefaultMessageFor(FilePath[] filesToCheckin);

  @Nullable
  @NonNls
  String getHelpId();

  String getCheckinOperationName();

  @Nullable
  List<VcsException> commit(List<Change> changes, String preparedComment);

  @Nullable
  List<VcsException> commit(List<Change> changes, String preparedComment, @Nonnull Function<Object, Object> parametersHolder, Set<String> feedback);

  @Nullable
  List<VcsException> scheduleMissingFileForDeletion(List<FilePath> files);

  @Nullable
  List<VcsException> scheduleUnversionedFilesForAddition(List<VirtualFile> files);

  boolean keepChangeListAfterCommit(ChangeList changeList);

  /**
   * @return true if VFS refresh has to be performed after commit, because files might have changed during commit
   * (for example, due to keyword substitution in SVN or read-only status in Perforce).
   */
  boolean isRefreshAfterCommitNeeded();
}
