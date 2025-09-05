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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.application.Application;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.util.WaitForProgressToShow;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.change.ChangeListManagerImpl;
import consulo.versionControlSystem.impl.internal.change.VcsGuess;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.rollback.DefaultRollbackEnvironment;
import consulo.versionControlSystem.rollback.RollbackEnvironment;
import consulo.versionControlSystem.update.RefreshVFsSynchronously;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RollbackWorker {
    private final Project myProject;
    private final String myOperationName;
    private final boolean myInvokedFromModalContext;
    private final List<VcsException> myExceptions;

    public RollbackWorker(Project project) {
        this(project, DefaultRollbackEnvironment.ROLLBACK_OPERATION_NAME, false);
    }

    public RollbackWorker(Project project, String operationName, boolean invokedFromModalContext) {
        myProject = project;
        myOperationName = operationName;
        myInvokedFromModalContext = invokedFromModalContext;
        myExceptions = new ArrayList<>(0);
    }

    public void doRollback(
        Collection<Change> changes,
        boolean deleteLocallyAddedFiles,
        @Nullable Runnable afterVcsRefreshInAwt,
        @Nullable String localHistoryActionName
    ) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
        Runnable notifier = changeListManager.prepareForChangeDeletion(changes);
        Runnable afterRefresh = () -> {
            InvokeAfterUpdateMode updateMode = myInvokedFromModalContext
                ? InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE : InvokeAfterUpdateMode.SILENT;
            changeListManager.invokeAfterUpdate(
                () -> {
                    notifier.run();
                    if (afterVcsRefreshInAwt != null) {
                        afterVcsRefreshInAwt.run();
                    }
                },
                updateMode,
                "Refresh changelists after update",
                Application.get().getCurrentModalityState()
            );
        };

        final Runnable rollbackAction = new MyRollbackRunnable(changes, deleteLocallyAddedFiles, afterRefresh, localHistoryActionName);

        if (myProject.getApplication().isDispatchThread() && !myInvokedFromModalContext) {
            ProgressManager.getInstance().run(new Task.Backgroundable(
                myProject,
                myOperationName,
                true,
                new PerformInBackgroundOption() {
                    @Override
                    public boolean shouldStartInBackground() {
                        return VcsConfiguration.getInstance(myProject).PERFORM_ROLLBACK_IN_BACKGROUND;
                    }

                    @Override
                    public void processSentToBackground() {
                        VcsConfiguration.getInstance(myProject).PERFORM_ROLLBACK_IN_BACKGROUND = true;
                    }
                }
            ) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    rollbackAction.run();
                }
            });
        }
        else if (myInvokedFromModalContext) {
            ProgressManager.getInstance().run(new Task.Modal(myProject, myOperationName, true) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    rollbackAction.run();
                }
            });
        }
        else {
            rollbackAction.run();
        }
        ((ChangeListManagerImpl) changeListManager).showLocalChangesInvalidated();
    }

    private class MyRollbackRunnable implements Runnable {
        private final Collection<Change> myChanges;
        private final boolean myDeleteLocallyAddedFiles;
        private final Runnable myAfterRefresh;
        private final String myLocalHistoryActionName;
        private ProgressIndicator myIndicator;

        private MyRollbackRunnable(
            Collection<Change> changes,
            boolean deleteLocallyAddedFiles,
            Runnable afterRefresh,
            String localHistoryActionName
        ) {
            myChanges = changes;
            myDeleteLocallyAddedFiles = deleteLocallyAddedFiles;
            myAfterRefresh = afterRefresh;
            myLocalHistoryActionName = localHistoryActionName;
        }

        @Override
        public void run() {
            ChangesUtil.markInternalOperation(myChanges, true);
            try {
                doRun();
            }
            finally {
                ChangesUtil.markInternalOperation(myChanges, false);
            }
        }

        private void doRun() {
            myIndicator = ProgressManager.getInstance().getProgressIndicator();

            List<Change> changesToRefresh = new ArrayList<>();
            try {
                ChangesUtil.processChangesByVcs(myProject, myChanges, (vcs, changes) -> {
                    RollbackEnvironment environment = vcs.getRollbackEnvironment();
                    if (environment != null) {
                        changesToRefresh.addAll(changes);

                        if (myIndicator != null) {
                            myIndicator.setTextValue(LocalizeValue.localizeTODO(
                                vcs.getDisplayName() + ": performing " + StringUtil.toLowerCase(myOperationName) + "..."
                            ));
                            myIndicator.setIndeterminate(false);
                            myIndicator.checkCanceled();
                        }
                        environment.rollbackChanges(changes, myExceptions, new RollbackProgressModifier(changes.size(), myIndicator));
                        if (myIndicator != null) {
                            myIndicator.setText2Value(LocalizeValue.empty());
                            myIndicator.checkCanceled();
                        }

                        if (myExceptions.isEmpty() && myDeleteLocallyAddedFiles) {
                            deleteAddedFilesLocally(changes);
                        }
                    }
                });
            }
            catch (ProcessCanceledException e) {
                // still do refresh
            }

            if (myIndicator != null) {
                myIndicator.startNonCancelableSection();
                myIndicator.setIndeterminate(true);
                myIndicator.setText2Value(LocalizeValue.empty());
                myIndicator.setTextValue(VcsLocalize.progressTextSynchronizingFiles());
            }

            doRefresh(myProject, changesToRefresh);
            AbstractVcsHelper.getInstance(myProject).showErrors(myExceptions, myOperationName);
        }

        private void doRefresh(Project project, List<Change> changesToRefresh) {
            LocalHistoryAction action = LocalHistory.getInstance().startAction(myOperationName);

            Runnable forAwtThread = () -> {
                action.finish();
                LocalHistory.getInstance().putSystemLabel(
                    myProject,
                    LocalizeValue.localizeTODO(myLocalHistoryActionName == null ? myOperationName : myLocalHistoryActionName),
                    -1
                );
                VcsDirtyScopeManager manager = project.getComponent(VcsDirtyScopeManager.class);
                VcsGuess vcsGuess = new VcsGuess(myProject);

                for (Change change : changesToRefresh) {
                    ContentRevision beforeRevision = change.getBeforeRevision();
                    ContentRevision afterRevision = change.getAfterRevision();
                    if ((!change.isIsReplaced()) && beforeRevision != null && Comparing.equal(beforeRevision, afterRevision)) {
                        manager.fileDirty(beforeRevision.getFile());
                    }
                    else {
                        markDirty(manager, vcsGuess, beforeRevision);
                        markDirty(manager, vcsGuess, afterRevision);
                    }
                }

                myAfterRefresh.run();
            };

            RefreshVFsSynchronously.updateChangesForRollback(changesToRefresh);

            WaitForProgressToShow.runOrInvokeLaterAboveProgress(forAwtThread, null, project);
        }

        private void markDirty(@Nonnull VcsDirtyScopeManager manager, @Nonnull VcsGuess vcsGuess, @Nullable ContentRevision revision) {
            if (revision != null) {
                FilePath parent = revision.getFile().getParentPath();
                if (parent != null && couldBeMarkedDirty(vcsGuess, parent)) {
                    manager.dirDirtyRecursively(parent);
                }
                else {
                    manager.fileDirty(revision.getFile());
                }
            }
        }

        private boolean couldBeMarkedDirty(@Nonnull VcsGuess vcsGuess, @Nonnull FilePath path) {
            return vcsGuess.getVcsForDirty(path) != null;
        }

        private void deleteAddedFilesLocally(List<Change> changes) {
            if (myIndicator != null) {
                myIndicator.setText("Deleting added files locally...");
                myIndicator.setFraction(0);
            }
            int changesSize = changes.size();
            for (int i = 0; i < changesSize; i++) {
                Change c = changes.get(i);
                if (c.getType() == Change.Type.NEW) {
                    ContentRevision rev = c.getAfterRevision();
                    assert rev != null;
                    File ioFile = rev.getFile().getIOFile();
                    if (myIndicator != null) {
                        myIndicator.setText2(ioFile.getAbsolutePath());
                        myIndicator.setFraction(((double) i) / changesSize);
                    }
                    FileUtil.delete(ioFile);
                }
            }
            if (myIndicator != null) {
                myIndicator.setText2Value(LocalizeValue.empty());
            }
        }
    }
}
