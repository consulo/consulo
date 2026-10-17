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

import consulo.application.progress.ProgressBuilder;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.document.FileDocumentManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageButtonRole;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.Presentation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.change.CommitResultHandler;
import consulo.versionControlSystem.change.CommitSession;
import consulo.versionControlSystem.change.InvokeAfterUpdateMode;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.checkin.BaseCheckinHandlerFactory;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.checkin.CommitCheck;
import consulo.versionControlSystem.checkin.CommitInfo;
import consulo.versionControlSystem.checkin.CommitProblem;
import consulo.versionControlSystem.checkin.CommitProblemWithDetails;
import consulo.versionControlSystem.checkin.TextCommitProblem;
import consulo.versionControlSystem.impl.internal.change.PseudoMap;
import consulo.versionControlSystem.impl.internal.change.ui.awt.CommitHelper;
import consulo.versionControlSystem.impl.internal.checkin.CheckinMetaHandler;
import consulo.versionControlSystem.internal.CheckinHandlersManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedCommitWorkflow {
    private static final Logger LOG = Logger.getInstance(UnifiedCommitWorkflow.class);

    private final Project myProject;
    private final ProgressBuilderFactory myProgressBuilderFactory;
    private final List<CommitExecutor> myExecutors;
    private final boolean myModalChecks;

    private final CommitContext myCommitContext = new CommitContext();
    private final PseudoMap<Object, Object> myAdditionalData = new PseudoMap<>();
    private @Nullable CheckinProjectPanel myProjectPanel;
    private final List<CheckinHandler> myHandlers = new ArrayList<>();
    private final Set<Object> mySkippedHandlers = ConcurrentHashMap.newKeySet();
    private boolean mySmartChecksBlocked;

    private boolean myExecuting;

    public UnifiedCommitWorkflow(Project project,
                                 ProgressBuilderFactory progressBuilderFactory,
                                 List<CommitExecutor> executors,
                                 boolean modalChecks) {
        myProject = project;
        myProgressBuilderFactory = progressBuilderFactory;
        myExecutors = List.copyOf(executors);
        myModalChecks = modalChecks;
    }

    public Project getProject() {
        return myProject;
    }

    public List<CommitExecutor> getExecutors() {
        return myExecutors;
    }

    public CommitContext getCommitContext() {
        return myCommitContext;
    }

    public PseudoMap<Object, Object> getAdditionalData() {
        return myAdditionalData;
    }

    public @Nullable CheckinProjectPanel getProjectPanel() {
        return myProjectPanel;
    }

    public List<CheckinHandler> getHandlers() {
        return myHandlers;
    }

    public boolean isModalChecks() {
        return myModalChecks;
    }

    public boolean isExecuting() {
        return myExecuting;
    }

    public void initHandlers(CheckinProjectPanel panel) {
        myProjectPanel = panel;
        myHandlers.clear();

        AbstractVcs[] activeVcses = ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss();
        for (BaseCheckinHandlerFactory factory : CheckinHandlersManager.getInstance().getRegisteredCheckinHandlerFactories(activeVcses)) {
            CheckinHandler handler = factory.createHandler(panel, myCommitContext);
            if (handler != null && !CheckinHandler.DUMMY.equals(handler)) {
                myHandlers.add(handler);
            }
        }
    }

    @RequiredUIAccess
    public CompletableFuture<UnifiedCommitChecksResult> runCommitChecks(UIAccess uiAccess, UnifiedCommitInfo commitInfo) {
        List<CommitCheck> checks = new ArrayList<>();
        List<CheckinMetaHandler> metaHandlers = new ArrayList<>();
        for (CheckinHandler handler : myHandlers) {
            if (!handler.acceptExecutor(commitInfo.getExecutor())) {
                continue;
            }
            if (handler instanceof CheckinMetaHandler metaHandler) {
                metaHandlers.add(metaHandler);
            }

            CommitCheck check = handler instanceof CommitCheck commitCheck ? commitCheck : new ProxyCommitCheck(handler, uiAccess, myAdditionalData);
            if (check.isEnabled() && !mySkippedHandlers.contains(handler)) {
                checks.add(check);
            }
        }
        mySkippedHandlers.clear();

        boolean blockedByDumbMode = DumbService.getInstance(myProject).isDumb()
            && checks.stream().anyMatch(check -> !check.isDumbAware());
        if (blockedByDumbMode && !mySmartChecksBlocked) {
            if (myModalChecks) {
                return MessageBoxBuilder.<Boolean>create()
                    .asWarning()
                    .title(VcsLocalize.commitChecksErrorIndexing())
                    .text(VcsLocalize.commitChecksErrorIndexingMessage(myProject.getApplication().getName()))
                    .button(MessageButtonRole.YES, VcsLocalize.checkinWait(), Boolean.FALSE)
                    .asDefaultButton()
                    .button(MessageButtonRole.NO, commitInfo.getCommitActionText(), Boolean.TRUE)
                    .asExitButton()
                    .showAsync()
                    .thenComposeAsync(commit -> commit
                        ? runCommitChecks(uiAccess, commitInfo, checks, metaHandlers)
                        : CompletableFuture.completedFuture(UnifiedCommitChecksResult.cancelled()), uiAccess);
            }

            mySmartChecksBlocked = true;
            return CompletableFuture.completedFuture(UnifiedCommitChecksResult.failed(new TextCommitProblem(VcsLocalize.commitChecksErrorIndexing())));
        }
        mySmartChecksBlocked = false;

        return runCommitChecks(uiAccess, commitInfo, checks, metaHandlers);
    }

    @RequiredUIAccess
    private CompletableFuture<UnifiedCommitChecksResult> runCommitChecks(UIAccess uiAccess,
                                                                        UnifiedCommitInfo commitInfo,
                                                                        List<CommitCheck> checks,
                                                                        List<CheckinMetaHandler> metaHandlers) {
        Map<CommitCheck.ExecutionOrder, List<CommitCheck>> byOrder = new EnumMap<>(CommitCheck.ExecutionOrder.class);
        for (CommitCheck check : checks) {
            byOrder.computeIfAbsent(check.getExecutionOrder(), order -> new ArrayList<>()).add(check);
        }

        CommitExecutor executor = commitInfo.getExecutor();
        boolean postponePostCommitChecks = !myModalChecks
            && VcsConfiguration.getInstance(myProject).NON_MODAL_COMMIT_POSTPONE_SLOW_CHECKS
            && (executor == null || !executor.requiresSyncCommitChecks());

        ProgressBuilder builder = myProgressBuilderFactory.newProgressBuilder(myProject, VcsLocalize.commitChecksProgressTitle()).cancelable();
        if (myModalChecks) {
            builder = builder.modal();
        }

        return builder.execute(uiAccess, () -> Coroutine.first(CodeExecution.<Object, UnifiedCommitChecksResult>apply((input, continuation) -> {
            ProgressIndicator indicator = ProgressIndicator.from(continuation);
            try {
                UnifiedCommitChecksResult result = runEarlyChecks(uiAccess, commitInfo, byOrder.get(CommitCheck.ExecutionOrder.EARLY), indicator);
                if (result != null) {
                    return result;
                }

                runMetaHandlers(uiAccess, metaHandlers);

                result = runSequentialChecks(uiAccess, commitInfo, byOrder.get(CommitCheck.ExecutionOrder.MODIFICATION), myModalChecks, indicator);
                if (result != null) {
                    return result;
                }

                uiAccess.giveAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());

                result = runSequentialChecks(uiAccess, commitInfo, byOrder.get(CommitCheck.ExecutionOrder.LATE), true, indicator);
                if (result != null) {
                    return result;
                }

                List<CommitCheck> postCommitChecks = byOrder.getOrDefault(CommitCheck.ExecutionOrder.POST_COMMIT, List.of());
                if (postponePostCommitChecks) {
                    return UnifiedCommitChecksResult.passed(postCommitChecks);
                }

                result = myModalChecks
                    ? runSequentialChecks(uiAccess, commitInfo, postCommitChecks, true, indicator)
                    : runEarlyChecks(uiAccess, commitInfo, postCommitChecks, indicator);
                if (result != null) {
                    return result;
                }
                return UnifiedCommitChecksResult.passed(List.of());
            }
            catch (ProcessCanceledException | CancellationException e) {
                return UnifiedCommitChecksResult.cancelled();
            }
        })));
    }

    private @Nullable UnifiedCommitChecksResult runEarlyChecks(UIAccess uiAccess,
                                                              UnifiedCommitInfo commitInfo,
                                                              @Nullable List<CommitCheck> checks,
                                                              ProgressIndicator indicator) {
        if (checks == null || checks.isEmpty()) {
            return null;
        }

        if (myModalChecks) {
            return runSequentialChecks(uiAccess, commitInfo, checks, true, indicator);
        }

        CommitProblem firstProblem = null;
        for (CommitCheck check : checks) {
            indicator.checkCanceled();
            CommitProblem problem = runCommitCheck(check, commitInfo, indicator);
            if (problem != null) {
                rememberFailed(check);
                if (firstProblem == null) {
                    firstProblem = problem;
                }
            }
        }
        return firstProblem == null ? null : UnifiedCommitChecksResult.failed(firstProblem);
    }

    private @Nullable UnifiedCommitChecksResult runSequentialChecks(UIAccess uiAccess,
                                                                   UnifiedCommitInfo commitInfo,
                                                                   @Nullable List<CommitCheck> checks,
                                                                   boolean showModalSolution,
                                                                   ProgressIndicator indicator) {
        if (checks == null) {
            return null;
        }

        for (CommitCheck check : checks) {
            indicator.checkCanceled();
            CommitProblem problem = runCommitCheck(check, commitInfo, indicator);
            if (problem == null) {
                continue;
            }

            if (!showModalSolution) {
                rememberFailed(check);
                return UnifiedCommitChecksResult.failed(problem);
            }

            CheckinHandler.ReturnResult solution = uiAccess.giveAsync(() -> problem.showModalSolution(myProject, commitInfo))
                .thenCompose(future -> future)
                .join();
            switch (solution) {
                case COMMIT -> {
                }
                case CLOSE_WINDOW -> {
                    return UnifiedCommitChecksResult.closeWindow();
                }
                case CANCEL -> {
                    if (!myModalChecks) {
                        rememberFailed(check);
                    }
                    return UnifiedCommitChecksResult.failed(problem instanceof UnknownCommitProblem ? null : problem);
                }
            }
        }
        return null;
    }

    private @Nullable CommitProblem runCommitCheck(CommitCheck check, CommitInfo commitInfo, ProgressIndicator indicator) {
        if (DumbService.getInstance(myProject).isDumb() && !check.isDumbAware()) {
            return null;
        }

        try {
            return check.runCheck(commitInfo, indicator);
        }
        catch (ProcessCanceledException | CancellationException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
            return CommitProblem.createError(e);
        }
    }

    private void runMetaHandlers(UIAccess uiAccess, List<CheckinMetaHandler> metaHandlers) {
        for (CheckinMetaHandler metaHandler : metaHandlers.reversed()) {
            CompletableFuture<?> done = new CompletableFuture<>();
            uiAccess.give(() -> {
                try {
                    metaHandler.runCheckinHandlers(() -> done.complete(null));
                }
                catch (Throwable e) {
                    done.completeExceptionally(e);
                }
            });
            done.join();
        }
    }

    private void rememberFailed(CommitCheck check) {
        mySkippedHandlers.add(check instanceof ProxyCommitCheck proxy ? proxy.getCheckinHandler() : check);
    }

    public CompletableFuture<?> awaitChangesUpdate() {
        CompletableFuture<@Nullable Object> updated = new CompletableFuture<>();
        ChangeListManager.getInstance(myProject).invokeAfterUpdate(
            () -> updated.complete(null),
            InvokeAfterUpdateMode.SILENT_CALLBACK_POOLED,
            "",
            ModalityState.nonModal()
        );
        return updated;
    }

    public void runPostponedChecks(UIAccess uiAccess, UnifiedCommitInfo commitInfo, List<CommitCheck> checks) {
        if (checks.isEmpty()) {
            return;
        }

        UnifiedCommitInfo postCommitInfo = commitInfo.asPostCommit();
        myProgressBuilderFactory.newProgressBuilder(myProject, VcsLocalize.commitChecksProgressTitle())
            .execute(uiAccess, () -> Coroutine.first(CodeExecution.<Object, List<CommitProblem>>apply((input, continuation) -> {
                ProgressIndicator indicator = ProgressIndicator.from(continuation);
                List<CommitProblem> problems = new ArrayList<>();
                for (CommitCheck check : checks) {
                    indicator.checkCanceled();
                    CommitProblem problem = runCommitCheck(check, postCommitInfo, indicator);
                    if (problem != null) {
                        problems.add(problem);
                    }
                }
                return problems;
            })))
            .whenComplete((problems, error) -> {
                if (error != null) {
                    LOG.error(error);
                }
                else if (problems != null) {
                    for (CommitProblem problem : problems) {
                        notifyPostCommitProblem(problem);
                    }
                }
            });
    }

    private void notifyPostCommitProblem(CommitProblem problem) {
        Notification.Builder builder = NotificationService.getInstance()
            .newWarn(VcsNotifier.IMPORTANT_ERROR_NOTIFICATION)
            .title(VcsLocalize.postCommitChecksFailedNotificationTitle())
            .content(problem.getText());
        if (problem instanceof CommitProblemWithDetails withDetails) {
            builder.addClosingAction(withDetails.getShowDetailsAction().map(Presentation.NO_MNEMONIC), () -> withDetails.showDetails(myProject));
        }
        builder.notify(myProject);
    }

    public void performCommit(LocalChangeList changeList,
                              List<Change> changes,
                              String commitMessage,
                              LocalizeValue actionName,
                              boolean allOfDefaultChangeListChangesIncluded,
                              @Nullable CommitResultHandler resultHandler) {
        myExecuting = true;

        CommitHelper helper = new CommitHelper(
            myProject,
            changeList,
            changes,
            actionName,
            commitMessage,
            myHandlers,
            allOfDefaultChangeListChangesIncluded,
            false,
            myAdditionalData,
            new CommitResultHandler() {
                @Override
                public void onSuccess(String commitMessage) {
                    myExecuting = false;
                    if (resultHandler != null) {
                        resultHandler.onSuccess(commitMessage);
                    }
                }

                @Override
                public void onFailure() {
                    myExecuting = false;
                    if (resultHandler != null) {
                        resultHandler.onFailure();
                    }
                }
            }
        );
        helper.doCommit((AbstractVcs) null);
    }

    public CompletableFuture<Boolean> performExecutorCommit(UIAccess uiAccess,
                                                           CommitExecutor executor,
                                                           CommitSession session,
                                                           List<Change> changes,
                                                           String commitMessage) {
        myExecuting = true;

        return myProgressBuilderFactory.newProgressBuilder(myProject, executor.getActionText())
            .cancelable()
            .execute(uiAccess, () -> Coroutine.first(CodeExecution.<Object, Boolean>apply((input, continuation) -> {
                session.execute(changes, commitMessage);
                return Boolean.TRUE;
            })))
            .whenComplete((result, error) -> {
                myExecuting = false;
                if (error == null) {
                    for (CheckinHandler handler : myHandlers) {
                        handler.checkinSuccessful();
                    }
                }
                else {
                    session.executionCanceled();
                    for (CheckinHandler handler : myHandlers) {
                        handler.checkinFailed(List.of(new VcsException(error)));
                    }
                }
            });
    }

    public void moveToFailedList(LocalChangeList changeList, String commitMessage, List<Change> changes) {
        CommitHelper.moveToFailedList(
            changeList,
            commitMessage,
            changes,
            VcsLocalize.commitDialogRejectedCommitTemplate(changeList.getName()).get(),
            myProject
        );
    }

    public static boolean isAllOfDefaultChangeListChangesIncluded(Project project, LocalChangeList changeList, List<Change> changes) {
        LocalChangeList defaultList = ChangeListManager.getInstance(project).getDefaultChangeList();
        return changeList.equals(defaultList) && changes.containsAll(defaultList.getChanges());
    }
}
