/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.project.Project;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.CommitExecutor;

import jakarta.annotation.Nonnull;
import java.util.List;

public abstract class BeforeCheckinDialogHandler {
  @Deprecated
  /**
   * @deprecated see {@link #beforeCommitDialogShown(Project, java.util.List, Iterable, boolean)}
   * @return false to cancel commit
   */
  public boolean beforeCommitDialogShownCallback(Iterable<CommitExecutor> executors, boolean showVcsCommit) {
    throw new AbstractMethodError();
  }

  /**
   * @return false to cancel commit
   */
  public boolean beforeCommitDialogShown(@Nonnull Project project, @Nonnull List<Change> changes, @Nonnull Iterable<CommitExecutor> executors, boolean showVcsCommit) {
    //noinspection deprecation
    return beforeCommitDialogShownCallback(executors, showVcsCommit);
  }
}
