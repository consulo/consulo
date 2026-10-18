/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.impl.internal.commit;

import consulo.document.FileDocumentManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.change.CommitResultHandler;
import consulo.versionControlSystem.change.CommitSession;
import consulo.versionControlSystem.change.CommitSessionContextAware;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CommitCheck;
import consulo.versionControlSystem.checkin.CommitProblem;
import consulo.versionControlSystem.checkin.TextCommitProblem;
import consulo.versionControlSystem.impl.internal.change.ChangeListManagerImpl;
import consulo.versionControlSystem.impl.internal.change.ui.SessionDialogDescriptor;
import consulo.versionControlSystem.internal.ChangeListManagerEx;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedCommitWorkflowHandler {
    private static final Logger LOG = Logger.getInstance(UnifiedCommitWorkflowHandler.class);

    private final Project myProject;
    private final UnifiedCommitWorkflow myWorkflow;
    private final UnifiedCommitWorkflowUi myUi;
    private final UnifiedCommitMessagePolicy myMessagePolicy;
    private final LocalizeValue myCommitActionName;

    public UnifiedCommitWorkflowHandler(UnifiedCommitWorkflow workflow, UnifiedCommitWorkflowUi ui, LocalizeValue commitActionName) {
        myProject = workflow.getProject();
        myWorkflow = workflow;
        myUi = ui;
        myCommitActionName = commitActionName;
        myMessagePolicy = new UnifiedCommitMessagePolicy(myProject, workflow.isModalChecks());
    }

    public UnifiedCommitWorkflow getWorkflow() {
        return myWorkflow;
    }

    @RequiredUIAccess
    public void activate() {
        myWorkflow.initHandlers(new UnifiedCommitProjectPanelAdapter(myProject, myUi, myCommitActionName));
        myUi.setCommitMessage(myMessagePolicy.getInitialMessage(myUi.getChangeList()));
    }

    @RequiredUIAccess
    public void changeListChanged(LocalChangeList oldList, LocalChangeList newList) {
        myMessagePolicy.saveMessage(oldList, myUi.getCommitMessage(), false);
        myUi.setCommitMessage(myMessagePolicy.getInitialMessage(newList));
    }

    @RequiredUIAccess
    public void cancelled() {
        myMessagePolicy.saveMessage(myUi.getChangeList(), myUi.getCommitMessage(), false);
    }

    @RequiredUIAccess
    public CompletableFuture<Boolean> executorCalled(@Nullable CommitExecutor executor) {
        if (myWorkflow.isExecuting()) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        UIAccess uiAccess = UIAccess.current();
        return confirmEmptyMessage()
            .thenComposeAsync(confirmed -> confirmed ? execute(uiAccess, executor) : CompletableFuture.completedFuture(Boolean.FALSE), uiAccess);
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> confirmEmptyMessage() {
        if (!VcsConfiguration.getInstance(myProject).FORCE_NON_EMPTY_COMMENT || !myUi.getCommitMessage().isEmpty()) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        return MessageBoxes.yesNo()
            .title(VcsLocalize.confirmationTitleCheckInWithEmptyComment())
            .text(VcsLocalize.confirmationTextCheckInWithEmptyComment())
            .asWarning()
            .showAsync();
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> execute(UIAccess uiAccess, @Nullable CommitExecutor executor) {
        LocalChangeList changeList = myUi.getChangeList();
        String commitMessage = myUi.getCommitMessage();

        myMessagePolicy.saveMessage(changeList, commitMessage, true);

        if (!addUnversionedFiles(changeList)) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        FileDocumentManager.getInstance().saveAllDocuments();

        myUi.startBeforeCommitChecks();
        return myWorkflow.awaitChangesUpdate()
            .thenComposeAsync(updated -> myUi.refreshChangesBeforeCommit(), uiAccess)
            .thenComposeAsync(refreshed -> runChecksAndCommit(uiAccess, executor, changeList), uiAccess);
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> runChecksAndCommit(UIAccess uiAccess, @Nullable CommitExecutor executor, LocalChangeList changeList) {
        if (myUi.getIncludedChanges().isEmpty()) {
            myUi.endBeforeCommitChecks(
                CheckinHandler.ReturnResult.CANCEL,
                new TextCommitProblem(VcsLocalize.commitDialogNoChangesDetectedText())
            );
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        LocalizeValue actionText = executor != null ? executor.getActionText() : myCommitActionName;
        UnifiedCommitInfo commitInfo = new UnifiedCommitInfo(
            myProject,
            myWorkflow.getCommitContext(),
            executor,
            myUi.getIncludedChanges(),
            myUi.getCommitMessage(),
            actionText.removeEllipsis()
        );

        return myWorkflow.runCommitChecks(uiAccess, commitInfo).handleAsync((result, error) -> {
            if (error != null) {
                LOG.error(error);
                myUi.endBeforeCommitChecks(CheckinHandler.ReturnResult.CANCEL, CommitProblem.createError(error));
                return CompletableFuture.completedFuture(Boolean.FALSE);
            }

            switch (result.getKind()) {
                case PASSED -> {
                    myUi.endBeforeCommitChecks(CheckinHandler.ReturnResult.COMMIT, null);
                    return commit(uiAccess, executor, changeList, commitInfo, result.getPostponedChecks());
                }
                case CLOSE_WINDOW -> {
                    myUi.endBeforeCommitChecks(CheckinHandler.ReturnResult.CLOSE_WINDOW, null);
                    myUi.deactivate(false);
                }
                case FAILED, CANCELLED -> myUi.endBeforeCommitChecks(CheckinHandler.ReturnResult.CANCEL, result.getProblem());
            }
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }, uiAccess).thenCompose(future -> future);
    }

    @RequiredUIAccess
    private boolean addUnversionedFiles(LocalChangeList changeList) {
        List<VirtualFile> files = myUi.getIncludedUnversionedFiles();
        if (files.isEmpty()) {
            return true;
        }

        ChangeListManagerEx manager = (ChangeListManagerEx) ChangeListManager.getInstance(myProject);
        List<VcsException> exceptions = manager.addUnversionedFiles(
            changeList,
            files,
            ChangeListManagerImpl.getDefaultUnversionedFileCondition(),
            myUi::includeChanges
        );
        for (VcsException exception : exceptions) {
            LOG.warn(exception);
        }
        return exceptions.isEmpty();
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> commit(UIAccess uiAccess,
                                              @Nullable CommitExecutor executor,
                                              LocalChangeList changeList,
                                              UnifiedCommitInfo commitInfo,
                                              List<CommitCheck> postponedChecks) {
        CommitSession session = executor == null ? CommitSession.VCS_COMMIT : executor.createCommitSession();
        if (session instanceof CommitSessionContextAware contextAware) {
            contextAware.setContext(myWorkflow.getCommitContext());
        }

        List<Change> changes = commitInfo.getCommittedChanges();
        boolean allIncluded = UnifiedCommitWorkflow.isAllOfDefaultChangeListChangesIncluded(myProject, changeList, changes);

        if (session == CommitSession.VCS_COMMIT) {
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            myWorkflow.performCommit(changeList, changes, commitInfo.getCommitMessage(), myCommitActionName, allIncluded, new CommitResultHandler() {
                @Override
                public void onSuccess(String commitMessage) {
                    uiAccess.give(() -> {
                        myMessagePolicy.onCommitSuccess(changeList, allIncluded);
                        myWorkflow.runPostponedChecks(uiAccess, commitInfo, postponedChecks);
                        myUi.deactivate(true);
                    });
                    result.complete(Boolean.TRUE);
                }

                @Override
                public void onFailure() {
                    result.complete(Boolean.FALSE);
                }
            });
            return result;
        }

        if (executor == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        String commitMessage = commitInfo.getCommitMessage();
        return SessionDialogDescriptor.configureCommitSession(myProject, executor.getActionText(), session, changes, commitMessage)
            .thenComposeAsync(configured -> {
                if (!configured) {
                    return CompletableFuture.completedFuture(Boolean.FALSE);
                }
                return myWorkflow.performExecutorCommit(uiAccess, executor, session, changes, commitMessage)
                    .handleAsync((completed, error) -> {
                        if (error != null) {
                            MessageBoxes.okError(VcsLocalize.errorExecutingCommit(executor.getActionText(), error.getLocalizedMessage()))
                                .showAsync();
                            return Boolean.FALSE;
                        }

                        myMessagePolicy.onCommitSuccess(changeList, allIncluded);
                        myWorkflow.runPostponedChecks(uiAccess, commitInfo, postponedChecks);
                        myUi.deactivate(true);
                        return Boolean.TRUE;
                    }, uiAccess);
            }, uiAccess);
    }
}
