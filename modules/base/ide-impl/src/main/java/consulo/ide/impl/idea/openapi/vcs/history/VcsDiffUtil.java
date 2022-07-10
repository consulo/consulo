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
package consulo.ide.impl.idea.openapi.vcs.history;

import consulo.diff.DiffManager;
import consulo.diff.request.MessageDiffRequest;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogBuilder;
import consulo.vcs.FilePath;
import consulo.vcs.VcsBundle;
import consulo.vcs.change.Change;
import consulo.vcs.change.ContentRevision;
import consulo.ide.impl.idea.openapi.vcs.changes.CurrentContentRevision;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.diff.ShowDiffAction;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.diff.ShowDiffContext;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowser;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static consulo.ide.impl.idea.diff.util.DiffUserDataKeysEx.VCS_DIFF_LEFT_CONTENT_TITLE;
import static consulo.ide.impl.idea.diff.util.DiffUserDataKeysEx.VCS_DIFF_RIGHT_CONTENT_TITLE;

public class VcsDiffUtil {

  @RequiredUIAccess
  public static void showDiffFor(@Nonnull Project project,
                                 @Nonnull final Collection<Change> changes,
                                 @Nonnull final String revNumTitle1,
                                 @Nonnull final String revNumTitle2,
                                 @Nonnull final FilePath filePath) {
    if (filePath.isDirectory()) {
      showChangesDialog(project, getDialogTitle(filePath, revNumTitle1, revNumTitle2), ContainerUtil.newArrayList(changes));
    }
    else {
      if (changes.isEmpty()) {
        DiffManager.getInstance().showDiff(project, new MessageDiffRequest("No Changes Found"));
      }
      else {
        final Map<Key, Object> revTitlesMap = new HashMap<>(2);
        revTitlesMap.put(VCS_DIFF_LEFT_CONTENT_TITLE, revNumTitle1);
        revTitlesMap.put(VCS_DIFF_RIGHT_CONTENT_TITLE, revNumTitle2);
        ShowDiffContext showDiffContext = new ShowDiffContext() {
          @Nonnull
          @Override
          public Map<Key, Object> getChangeContext(@Nonnull Change change) {
            return revTitlesMap;
          }
        };
        ShowDiffAction.showDiffForChange(project, changes, 0, showDiffContext);
      }
    }
  }

  @Nonnull
  private static String getDialogTitle(@Nonnull final FilePath filePath, @Nonnull final String revNumTitle1,
                                       @Nonnull final String revNumTitle2) {
    return String.format("Difference between %s and %s versions in %s", revNumTitle1, revNumTitle2, filePath.getName());
  }

  @Nonnull
  public static String getRevisionTitle(@Nonnull String revision, boolean localMark) {
    return revision +
           (localMark ? " (" + VcsBundle.message("diff.title.local") + ")" : "");
  }

  @RequiredUIAccess
  public static void showChangesDialog(@Nonnull Project project, @Nonnull String title, @Nonnull List<Change> changes) {
    DialogBuilder dialogBuilder = new DialogBuilder(project);

    dialogBuilder.setTitle(title);
    dialogBuilder.setActionDescriptors(new DialogBuilder.CloseDialogAction());
    final ChangesBrowser changesBrowser =
            new ChangesBrowser(project, null, changes, null, false, true, null, ChangesBrowser.MyUseCase.COMMITTED_CHANGES, null);
    changesBrowser.setChangesToDisplay(changes);
    dialogBuilder.setCenterPanel(changesBrowser);
    dialogBuilder.setPreferredFocusComponent(changesBrowser.getPreferredFocusedComponent());
    dialogBuilder.showNotModal();
  }

  @Nonnull
  public static List<Change> createChangesWithCurrentContentForFile(@Nonnull FilePath filePath,
                                                                    @Nullable ContentRevision beforeContentRevision) {
    return Collections.singletonList(new Change(beforeContentRevision, CurrentContentRevision.create(filePath)));
  }
}
