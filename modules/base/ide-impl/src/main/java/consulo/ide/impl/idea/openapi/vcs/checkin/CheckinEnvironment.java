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
package consulo.ide.impl.idea.openapi.vcs.checkin;

import consulo.ide.impl.idea.openapi.vcs.CheckinProjectPanel;
import consulo.ide.impl.idea.openapi.vcs.FilePath;
import consulo.ide.impl.idea.openapi.vcs.VcsException;
import consulo.ide.impl.idea.openapi.vcs.VcsProviderMarker;
import consulo.ide.impl.idea.openapi.vcs.changes.Change;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeList;
import consulo.ide.impl.idea.openapi.vcs.ui.RefreshableOnComponent;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.NullableFunction;
import consulo.ide.impl.idea.util.PairConsumer;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Interface for performing VCS checkin / commit / submit operations.
 *
 * @author lesya
 * @see consulo.ide.impl.idea.openapi.vcs.AbstractVcs#getCheckinEnvironment()
 */
public interface CheckinEnvironment extends VcsProviderMarker {
  @javax.annotation.Nullable
  RefreshableOnComponent createAdditionalOptionsPanel(CheckinProjectPanel panel, PairConsumer<Object, Object> additionalDataConsumer);

  @javax.annotation.Nullable
  String getDefaultMessageFor(FilePath[] filesToCheckin);

  @javax.annotation.Nullable
  @NonNls
  String getHelpId();

  String getCheckinOperationName();

  @Nullable
  List<VcsException> commit(List<Change> changes, String preparedComment);

  @javax.annotation.Nullable
  List<VcsException> commit(List<Change> changes,
                            String preparedComment,
                            @Nonnull NullableFunction<Object, Object> parametersHolder,
                            Set<String> feedback);

  @javax.annotation.Nullable
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
