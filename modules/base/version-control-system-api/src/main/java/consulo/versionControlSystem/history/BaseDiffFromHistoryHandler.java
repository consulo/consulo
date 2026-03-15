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
package consulo.versionControlSystem.history;

import consulo.annotation.UsedInPlugin;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.DialogBuilder;
import consulo.util.collection.Lists;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesBrowser;
import consulo.versionControlSystem.change.ChangesBrowserFactory;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import org.jspecify.annotations.Nullable;

import java.util.List;

@UsedInPlugin
public abstract class BaseDiffFromHistoryHandler<T extends VcsFileRevision> implements DiffFromHistoryHandler {

    private static final Logger LOG = Logger.getInstance(BaseDiffFromHistoryHandler.class);

    
    protected final Project myProject;

    protected BaseDiffFromHistoryHandler(Project project) {
        myProject = project;
    }

    @Override
    public void showDiffForOne(AnActionEvent e,
                               Project project, FilePath filePath,
                               VcsFileRevision previousRevision,
                               VcsFileRevision revision) {
        doShowDiff(filePath, previousRevision, revision);
    }

    @Override
    public void showDiffForTwo(Project project,
                               FilePath filePath,
                               VcsFileRevision older,
                               VcsFileRevision newer) {
        doShowDiff(filePath, older, newer);
    }

    @SuppressWarnings("unchecked")
    protected void doShowDiff(FilePath filePath,
                              VcsFileRevision older,
                              VcsFileRevision newer) {
        if (!filePath.isDirectory()) {
            VcsHistoryUtil.showDifferencesInBackground(myProject, filePath, older, newer);
        }
        else if (older.equals(VcsFileRevision.NULL)) {
            T right = (T) newer;
            showAffectedChanges(filePath, right);
        }
        else if (newer instanceof CurrentRevision) {
            T left = (T) older;
            showChangesBetweenRevisions(filePath, left, null);
        }
        else {
            T left = (T) older;
            T right = (T) newer;
            showChangesBetweenRevisions(filePath, left, right);
        }
    }

    protected void showChangesBetweenRevisions(final FilePath path, final T older, @Nullable final T newer) {
        new CollectChangesTask("Comparing revisions...") {

            
            @Override
            public List<Change> getChanges() throws VcsException {
                return getChangesBetweenRevisions(path, older, newer);
            }

            
            @Override
            public String getDialogTitle() {
                return getChangesBetweenRevisionsDialogTitle(path, older, newer);
            }
        }.queue();
    }

    protected void showAffectedChanges(final FilePath path, final T rev) {
        new CollectChangesTask("Collecting affected changes...") {

            
            @Override
            public List<Change> getChanges() throws VcsException {
                return getAffectedChanges(path, rev);
            }

            
            @Override
            public String getDialogTitle() {
                return getAffectedChangesDialogTitle(path, rev);
            }
        }.queue();
    }

    // rev2 == null -> compare rev1 with local
    // rev2 != null -> compare rev1 with rev2
    
    protected abstract List<Change> getChangesBetweenRevisions(FilePath path, T rev1, @Nullable T rev2)
        throws VcsException;

    
    protected abstract List<Change> getAffectedChanges(FilePath path, T rev) throws VcsException;

    
    protected abstract String getPresentableName(T revision);

    protected void showChangesDialog(String title, List<Change> changes) {
        DialogBuilder dialogBuilder = new DialogBuilder(myProject);

        dialogBuilder.setTitle(title);
        dialogBuilder.setActionDescriptors(new DialogBuilder.CloseDialogAction());
        ChangesBrowserFactory browserFactory = Application.get().getInstance(ChangesBrowserFactory.class);

        ChangesBrowser<Change> changesBrowser =
            browserFactory.createChangeBrowser(myProject, null, changes, null, false, true, null, ChangesBrowser.MyUseCase.COMMITTED_CHANGES, null);
        changesBrowser.setChangesToDisplay(changes);
        dialogBuilder.setCenterPanel(changesBrowser.getComponent());
        dialogBuilder.setPreferredFocusComponent(changesBrowser.getPreferredFocusedComponent());
        dialogBuilder.showNotModal();
    }

    protected void showError(VcsException e, String logMessage) {
        LOG.info(logMessage, e);
        VcsBalloonProblemNotifier.showOverVersionControlView(myProject, e.getMessage(), NotificationType.ERROR);
    }

    
    protected String getChangesBetweenRevisionsDialogTitle(FilePath path, T rev1, @Nullable T rev2) {
        String rev1Title = getPresentableName(rev1);

        return rev2 != null
            ? String.format("Difference between %s and %s in %s", rev1Title, getPresentableName(rev2), path.getName())
            : String.format("Difference between %s and local version in %s", rev1Title, path.getName());
    }

    
    protected String getAffectedChangesDialogTitle(FilePath path, T rev) {
        return String.format("Initial commit %s in %s", getPresentableName(rev), path.getName());
    }

    protected abstract class CollectChangesTask extends Task.Backgroundable {

        private List<Change> myChanges;

        public CollectChangesTask(String title) {
            super(BaseDiffFromHistoryHandler.this.myProject, title);
        }

        @Override
        public void run(ProgressIndicator indicator) {
            try {
                myChanges = getChanges();
            }
            catch (VcsException e) {
                showError(e, "Error during task: " + getDialogTitle());
            }
        }

        
        public abstract List<Change> getChanges() throws VcsException;

        
        public abstract String getDialogTitle();

        @RequiredUIAccess
        @Override
        public void onSuccess() {
            showChangesDialog(getDialogTitle(), Lists.notNullize(myChanges));
        }
    }
}
