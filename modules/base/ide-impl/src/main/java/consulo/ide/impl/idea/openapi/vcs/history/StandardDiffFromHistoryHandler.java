/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.history;

import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.history.DiffFromHistoryHandler;
import consulo.versionControlSystem.history.VcsFileRevision;

import javax.annotation.Nonnull;

public class StandardDiffFromHistoryHandler implements DiffFromHistoryHandler {

  @Override
  public void showDiffForOne(@Nonnull AnActionEvent e, @Nonnull Project project, @Nonnull FilePath filePath,
                             @Nonnull VcsFileRevision previousRevision, @Nonnull VcsFileRevision revision) {
    VcsHistoryUtil.showDifferencesInBackground(project, filePath, previousRevision, revision);
  }

  @Override
  public void showDiffForTwo(@Nonnull Project project,
                             @Nonnull FilePath filePath,
                             @Nonnull VcsFileRevision revision1,
                             @Nonnull VcsFileRevision revision2) {
    VcsHistoryUtil.showDifferencesInBackground(project, filePath, revision1, revision2);
  }
}
