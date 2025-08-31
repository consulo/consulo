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

import consulo.ui.ex.awt.DialogBuilder;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowser;
import consulo.versionControlSystem.history.CurrentRevision;
import consulo.versionControlSystem.history.VcsHistoryUtil;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.history.DiffFromHistoryHandler;
import consulo.versionControlSystem.history.VcsFileRevision;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

public abstract class BaseDiffFromHistoryHandler<T extends VcsFileRevision> implements DiffFromHistoryHandler {

  private static final Logger LOG = Logger.getInstance(BaseDiffFromHistoryHandler.class);

  @Nonnull
  protected final Project myProject;

  protected BaseDiffFromHistoryHandler(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  public void showDiffForOne(@Nonnull AnActionEvent e,
                             @Nonnull Project project, @Nonnull FilePath filePath,
                             @Nonnull VcsFileRevision previousRevision,
                             @Nonnull VcsFileRevision revision) {
    doShowDiff(filePath, previousRevision, revision);
  }

  @Override
  public void showDiffForTwo(@Nonnull Project project,
                             @Nonnull FilePath filePath,
                             @Nonnull VcsFileRevision older,
                             @Nonnull VcsFileRevision newer) {
    doShowDiff(filePath, older, newer);
  }

  @SuppressWarnings("unchecked")
  protected void doShowDiff(@Nonnull FilePath filePath,
                            @Nonnull VcsFileRevision older,
                            @Nonnull VcsFileRevision newer) {
    if (!filePath.isDirectory()) {
      VcsHistoryUtil.showDifferencesInBackground(myProject, filePath, older, newer);
    }
    else if (older.equals(VcsFileRevision.NULL)) {
      T right = (T)newer;
      showAffectedChanges(filePath, right);
    }
    else if (newer instanceof CurrentRevision) {
      T left = (T)older;
      showChangesBetweenRevisions(filePath, left, null);
    }
    else {
      T left = (T)older;
      T right = (T)newer;
      showChangesBetweenRevisions(filePath, left, right);
    }
  }

  protected void showChangesBetweenRevisions(@Nonnull final FilePath path, @Nonnull final T older, @Nullable final T newer) {
    new CollectChangesTask("Comparing revisions...") {

      @Nonnull
      @Override
      public List<Change> getChanges() throws VcsException {
        return getChangesBetweenRevisions(path, older, newer);
      }

      @Nonnull
      @Override
      public String getDialogTitle() {
        return getChangesBetweenRevisionsDialogTitle(path, older, newer);
      }
    }.queue();
  }

  protected void showAffectedChanges(@Nonnull final FilePath path, @Nonnull final T rev) {
    new CollectChangesTask("Collecting affected changes...") {

      @Nonnull
      @Override
      public List<Change> getChanges() throws VcsException {
        return getAffectedChanges(path, rev);
      }

      @Nonnull
      @Override
      public String getDialogTitle() {
        return getAffectedChangesDialogTitle(path, rev);
      }
    }.queue();
  }

  // rev2 == null -> compare rev1 with local
  // rev2 != null -> compare rev1 with rev2
  @Nonnull
  protected abstract List<Change> getChangesBetweenRevisions(@Nonnull FilePath path, @Nonnull T rev1, @Nullable T rev2)
          throws VcsException;

  @Nonnull
  protected abstract List<Change> getAffectedChanges(@Nonnull FilePath path, @Nonnull T rev) throws VcsException;

  @Nonnull
  protected abstract String getPresentableName(@Nonnull T revision);

  protected void showChangesDialog(@Nonnull String title, @Nonnull List<Change> changes) {
    DialogBuilder dialogBuilder = new DialogBuilder(myProject);

    dialogBuilder.setTitle(title);
    dialogBuilder.setActionDescriptors(new DialogBuilder.ActionDescriptor[]{new DialogBuilder.CloseDialogAction()});
    ChangesBrowser changesBrowser =
            new ChangesBrowser(myProject, null, changes, null, false, true, null, ChangesBrowser.MyUseCase.COMMITTED_CHANGES, null);
    changesBrowser.setChangesToDisplay(changes);
    dialogBuilder.setCenterPanel(changesBrowser);
    dialogBuilder.setPreferredFocusComponent(changesBrowser.getPreferredFocusedComponent());
    dialogBuilder.showNotModal();
  }

  protected void showError(@Nonnull VcsException e, @Nonnull String logMessage) {
    LOG.info(logMessage, e);
    VcsBalloonProblemNotifier.showOverVersionControlView(myProject, e.getMessage(), NotificationType.ERROR);
  }

  @Nonnull
  protected String getChangesBetweenRevisionsDialogTitle(@Nonnull FilePath path, @Nonnull T rev1, @Nullable T rev2) {
    String rev1Title = getPresentableName(rev1);

    return rev2 != null
           ? String.format("Difference between %s and %s in %s", rev1Title, getPresentableName(rev2), path.getName())
           : String.format("Difference between %s and local version in %s", rev1Title, path.getName());
  }

  @Nonnull
  protected String getAffectedChangesDialogTitle(@Nonnull FilePath path, @Nonnull T rev) {
    return String.format("Initial commit %s in %s", getPresentableName(rev), path.getName());
  }

  protected abstract class CollectChangesTask extends Task.Backgroundable {

    private List<Change> myChanges;

    public CollectChangesTask(@Nonnull String title) {
      super(BaseDiffFromHistoryHandler.this.myProject, title);
    }

    @Override
    public void run(@Nonnull ProgressIndicator indicator) {
      try {
        myChanges = getChanges();
      }
      catch (VcsException e) {
        showError(e, "Error during task: " + getDialogTitle());
      }
    }

    @Nonnull
    public abstract List<Change> getChanges() throws VcsException;

    @Nonnull
    public abstract String getDialogTitle();

    @Override
    public void onSuccess() {
      showChangesDialog(getDialogTitle(), ContainerUtil.notNullize(myChanges));
    }
  }
}
