/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.versionControlSystem.history;

import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.FilePath;

import jakarta.annotation.Nonnull;

/**
 * This handler is called when the user selects one or two revisions in the file history and invokes "Show Diff",
 * or selected one revision and invokes "Show Diff with Local"
 * Default handler is implemented in {@code vcs-impl}.
 * Custom handlers should be returned via {@link VcsHistoryProvider#getHistoryDiffHandler()}.
 */
public interface DiffFromHistoryHandler {

  /**
   * Show diff when a single revision is selected in the file history panel.
   *
   * @param e                AnActionEvent which happened, when user invoked "Show Diff".
   * @param filePath         the file which history is shown.
   * @param previousRevision the previous revision in the list displayed file history panel, may be {@link VcsFileRevision#NULL}.
   * @param revision         the revision selected in the file history panel.
   */
  void showDiffForOne(@Nonnull AnActionEvent e, @Nonnull Project project, @Nonnull FilePath filePath, @Nonnull VcsFileRevision previousRevision, @Nonnull VcsFileRevision revision);

  /**
   * Show diff for 2 revisions selected from the file history panel,
   * or "Show Diff with Local" (then the second revision is a {@link CurrentRevision}.
   * The order of selected revisions is not defined.
   *
   * @param filePath  the file which history is shown.
   * @param revision1 one of the selected revisions.
   * @param revision2 another selected revision.
   */
  void showDiffForTwo(@Nonnull Project project, @Nonnull FilePath filePath, @Nonnull VcsFileRevision revision1, @Nonnull VcsFileRevision revision2);

}
