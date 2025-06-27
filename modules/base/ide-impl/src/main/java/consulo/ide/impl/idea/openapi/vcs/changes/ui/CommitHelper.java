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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.Application;
import consulo.application.progress.DelegatingProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.Semaphore;
import consulo.document.Document;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.VetoSavingCommittingDocumentsAdapter;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.MoveChangesToAnotherListAction;
import consulo.ide.impl.idea.util.NullableFunction;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.NotificationType;
import consulo.project.util.WaitForProgressToShow;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.impl.internal.change.commited.CommittedChangesCache;
import consulo.versionControlSystem.impl.internal.ui.awt.ConfirmationDialog;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.versionControlSystem.update.RefreshVFsSynchronously;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

public class CommitHelper {
    public static final Key<Object> DOCUMENT_BEING_COMMITTED_KEY = new Key<>("DOCUMENT_BEING_COMMITTED");

    private final static Logger LOG = Logger.getInstance(CommitHelper.class);
    @Nonnull
    private final Project myProject;

    private final ChangeList myChangeList;
    private final List<Change> myIncludedChanges;

    private final LocalizeValue myActionName;
    private final String myCommitMessage;

    private final List<CheckinHandler> myHandlers;
    private final boolean myAllOfDefaultChangeListChangesIncluded;
    private final boolean myForceSyncCommit;
    private final NullableFunction<Object, Object> myAdditionalData;
    @Nullable
    private final CommitResultHandler myCustomResultHandler;
    private final List<Document> myCommittingDocuments = new ArrayList<>();
    private final VcsConfiguration myConfiguration;
    private final VcsDirtyScopeManager myDirtyScopeManager;
    private final HashSet<String> myFeedback;

    public CommitHelper(
        @Nonnull Project project,
        ChangeList changeList,
        List<Change> includedChanges,
        @Nonnull LocalizeValue actionName,
        String commitMessage,
        List<CheckinHandler> handlers,
        boolean allOfDefaultChangeListChangesIncluded,
        boolean synchronously,
        NullableFunction<Object, Object> additionalDataHolder,
        @Nullable CommitResultHandler customResultHandler
    ) {
        myProject = project;
        myChangeList = changeList;
        myIncludedChanges = includedChanges;
        myActionName = actionName;
        myCommitMessage = commitMessage;
        myHandlers = handlers;
        myAllOfDefaultChangeListChangesIncluded = allOfDefaultChangeListChangesIncluded;
        myForceSyncCommit = synchronously;
        myAdditionalData = additionalDataHolder;
        myCustomResultHandler = customResultHandler;
        myConfiguration = VcsConfiguration.getInstance(myProject);
        myDirtyScopeManager = VcsDirtyScopeManager.getInstance(myProject);
        myFeedback = new HashSet<>();
    }

    public CommitHelper(
        @Nonnull Project project,
        ChangeList changeList,
        List<Change> includedChanges,
        String actionName,
        String commitMessage,
        List<CheckinHandler> handlers,
        boolean allOfDefaultChangeListChangesIncluded,
        boolean synchronously,
        NullableFunction<Object, Object> additionalDataHolder,
        @Nullable CommitResultHandler customResultHandler
    ) {
        this(
            project,
            changeList,
            includedChanges,
            LocalizeValue.ofNullable(actionName),
            commitMessage,
            handlers,
            allOfDefaultChangeListChangesIncluded,
            synchronously,
            additionalDataHolder,
            customResultHandler
        );
    }

    public boolean doCommit() {
        return doCommit((AbstractVcs)null);
    }

    public boolean doCommit(@Nullable AbstractVcs vcs) {
        return doCommit(new CommitProcessor(vcs));
    }

    public boolean doAlienCommit(AbstractVcs vcs) {
        return doCommit(new AlienCommitProcessor(vcs));
    }

    private boolean doCommit(final GeneralCommitProcessor processor) {

        final Runnable action = () -> delegateCommitToVcsThread(processor);

        if (myForceSyncCommit) {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(action, myActionName, true, myProject);
            boolean success = doesntContainErrors(processor.getVcsExceptions());
            if (success) {
                reportResult(processor);
            }
            return success;
        }
        else {
            Task.Backgroundable task = new Task.Backgroundable(myProject, myActionName, true, myConfiguration.getCommitOption()) {
                @Override
                public void run(@Nonnull ProgressIndicator indicator) {
                    ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance((Project)myProject);
                    vcsManager.startBackgroundVcsOperation();
                    try {
                        action.run();
                    }
                    finally {
                        vcsManager.stopBackgroundVcsOperation();
                    }
                }

                @Override
                public NotificationInfo notifyFinished() {
                    if (myCustomResultHandler == null) {
                        String text = reportResult(processor);
                        return new NotificationInfo("VCS Commit", "VCS Commit Finished", text, true);
                    }
                    return null;
                }
            };
            ProgressManager.getInstance().run(task);
            return false;
        }
    }

    private void delegateCommitToVcsThread(GeneralCommitProcessor processor) {
        ProgressIndicator indicator = new DelegatingProgressIndicator();

        Semaphore endSemaphore = new Semaphore();
        endSemaphore.down();

        ChangeListManagerImpl.getInstanceImpl(myProject).executeOnUpdaterThread(() -> {
            indicator.setText("Performing VCS commit...");
            try {
                ProgressManager.getInstance().runProcess(
                    () -> {
                        indicator.checkCanceled();
                        generalCommit(processor);
                    },
                    indicator
                );
            }
            finally {
                endSemaphore.up();
            }
        });

        indicator.setText("Waiting for VCS background tasks to finish...");
        while (!endSemaphore.waitFor(20)) {
            indicator.checkCanceled();
        }
    }

    private String reportResult(GeneralCommitProcessor processor) {
        List<Change> changesFailedToCommit = processor.getChangesFailedToCommit();

        int failed = changesFailedToCommit.size();
        int committed = myIncludedChanges.size() - failed;

        String text = committed + " " + StringUtil.pluralize("file", committed) + " committed";
        if (failed > 0) {
            text += ", " + failed + " " + StringUtil.pluralize("file", failed) + " failed to commit";
        }
        StringBuilder content = new StringBuilder(StringUtil.isEmpty(myCommitMessage) ? text : text + ": " + escape(myCommitMessage));
        for (String s : myFeedback) {
            content.append("\n");
            content.append(s);
        }
        NotificationType notificationType = resolveNotificationType(processor);
        NotificationService.getInstance()
            .newOfType(VcsBalloonProblemNotifier.NOTIFICATION_GROUP, notificationType)
            .content(LocalizeValue.of(content.toString()))
            .notify(myProject);
        return text;
    }

    private static NotificationType resolveNotificationType(@Nonnull GeneralCommitProcessor processor) {
        boolean hasExceptions = !processor.getVcsExceptions().isEmpty();
        boolean hasOnlyWarnings = doesntContainErrors(processor.getVcsExceptions());

        return hasExceptions ? (hasOnlyWarnings ? NotificationType.WARNING : NotificationType.ERROR) : NotificationType.INFORMATION;
    }

    /*
      Commit message is passed to NotificationManagerImpl#doNotify and displayed as HTML.
      Thus HTML tag braces (< and >) should be escaped,
      but only they since the text is passed directly to HTML <BODY> tag and is not a part of an attribute or else.
     */
    private static String escape(String s) {
        String[] FROM = {"<", ">"};
        String[] TO = {"&lt;", "&gt;"};
        return StringUtil.replace(s, FROM, TO);
    }

    private static boolean doesntContainErrors(List<VcsException> vcsExceptions) {
        for (VcsException vcsException : vcsExceptions) {
            if (!vcsException.isWarning()) {
                return false;
            }
        }
        return true;
    }

    private void generalCommit(GeneralCommitProcessor processor) {
        try {
            Application appManager = myProject.getApplication();
            appManager.runReadAction((Runnable)this::markCommittingDocuments);

            try {
                processor.callSelf();
            }
            finally {
                appManager.runReadAction((Runnable)this::unmarkCommittingDocuments);
            }

            processor.doBeforeRefresh();

            AbstractVcsHelper.getInstance(myProject).showErrors(processor.getVcsExceptions(), myActionName);
        }
        catch (RuntimeException e) {
            LOG.error(e);
            processor.myVcsExceptions.add(new VcsException(e));
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
            processor.myVcsExceptions.add(new VcsException(e));
            throw new RuntimeException(e);
        }
        finally {
            commitCompleted(processor.getVcsExceptions(), processor);
            processor.customRefresh();
            WaitForProgressToShow.runOrInvokeLaterAboveProgress(
                () -> {
                    Runnable runnable = processor.postRefresh();
                    if (runnable != null) {
                        runnable.run();
                    }
                },
                null,
                myProject
            );
        }
    }

    private class AlienCommitProcessor extends GeneralCommitProcessor {
        private final AbstractVcs myVcs;

        private AlienCommitProcessor(AbstractVcs vcs) {
            myVcs = vcs;
        }

        @Override
        public void callSelf() {
            ChangesUtil.processItemsByVcs(myIncludedChanges, item -> myVcs, this);
        }

        @Override
        public void process(AbstractVcs vcs, @Nonnull List<Change> items) {
            if (myVcs.getName().equals(vcs.getName())) {
                CheckinEnvironment environment = vcs.getCheckinEnvironment();
                if (environment != null) {
                    Collection<FilePath> paths = ChangesUtil.getPaths(items);
                    myPathsToRefresh.addAll(paths);

                    List<VcsException> exceptions = environment.commit(items, myCommitMessage, myAdditionalData, myFeedback);
                    if (exceptions != null && exceptions.size() > 0) {
                        myVcsExceptions.addAll(exceptions);
                        myChangesFailedToCommit.addAll(items);
                    }
                }
            }
        }

        @Override
        public void afterSuccessfulCheckIn() {

        }

        @Override
        public void afterFailedCheckIn() {
        }

        @Override
        public void doBeforeRefresh() {
        }

        @Override
        public void customRefresh() {
        }

        @Override
        public Runnable postRefresh() {
            return null;
        }

        @Override
        public void doVcsRefresh() {
        }
    }

    private abstract static class GeneralCommitProcessor implements ChangesUtil.PerVcsProcessor<Change>, ActionsAroundRefresh {
        protected final List<FilePath> myPathsToRefresh;
        protected final List<VcsException> myVcsExceptions;
        protected final List<Change> myChangesFailedToCommit;

        protected GeneralCommitProcessor() {
            myPathsToRefresh = new ArrayList<>();
            myVcsExceptions = new ArrayList<>();
            myChangesFailedToCommit = new ArrayList<>();
        }

        public abstract void callSelf();

        public abstract void afterSuccessfulCheckIn();

        public abstract void afterFailedCheckIn();

        public List<FilePath> getPathsToRefresh() {
            return myPathsToRefresh;
        }

        public List<VcsException> getVcsExceptions() {
            return myVcsExceptions;
        }

        public List<Change> getChangesFailedToCommit() {
            return myChangesFailedToCommit;
        }
    }

    private interface ActionsAroundRefresh {
        void doBeforeRefresh();

        void customRefresh();

        void doVcsRefresh();

        Runnable postRefresh();
    }

    private static enum ChangeListsModificationAfterCommit {
        DELETE_LIST,
        MOVE_OTHERS,
        NOTHING
    }

    private class CommitProcessor extends GeneralCommitProcessor {
        private boolean myKeepChangeListAfterCommit;
        private LocalHistoryAction myAction;
        private ChangeListsModificationAfterCommit myAfterVcsRefreshModification;
        private boolean myCommitSuccess;
        @Nullable
        private final AbstractVcs myVcs;

        private CommitProcessor(@Nullable AbstractVcs vcs) {
            myVcs = vcs;
            myAfterVcsRefreshModification = ChangeListsModificationAfterCommit.NOTHING;
            if (myChangeList instanceof LocalChangeList localList) {
                boolean containsAll = new HashSet<>(myIncludedChanges).containsAll(new HashSet<>(myChangeList.getChanges()));
                if (containsAll && !localList.isDefault() && !localList.isReadOnly()) {
                    myAfterVcsRefreshModification = ChangeListsModificationAfterCommit.DELETE_LIST;
                }
                else if (myConfiguration.OFFER_MOVE_TO_ANOTHER_CHANGELIST_ON_PARTIAL_COMMIT && !containsAll
                    && localList.isDefault() && myAllOfDefaultChangeListChangesIncluded) {
                    myAfterVcsRefreshModification = ChangeListsModificationAfterCommit.MOVE_OTHERS;
                }
            }
        }

        @Override
        public void callSelf() {
            if (myVcs != null && myIncludedChanges.isEmpty()) {
                process(myVcs, myIncludedChanges);
            }
            ChangesUtil.processChangesByVcs(myProject, myIncludedChanges, this);
        }

        @Override
        public void process(AbstractVcs vcs, @Nonnull List<Change> items) {
            CheckinEnvironment environment = vcs.getCheckinEnvironment();
            if (environment != null) {
                Collection<FilePath> paths = ChangesUtil.getPaths(items);
                myPathsToRefresh.addAll(paths);
                if (environment.keepChangeListAfterCommit(myChangeList)) {
                    myKeepChangeListAfterCommit = true;
                }
                List<VcsException> exceptions = environment.commit(items, myCommitMessage, myAdditionalData, myFeedback);
                if (exceptions != null && exceptions.size() > 0) {
                    myVcsExceptions.addAll(exceptions);
                    myChangesFailedToCommit.addAll(items);
                }
            }
        }

        @Override
        public void afterSuccessfulCheckIn() {
            myCommitSuccess = true;
        }

        @Override
        @RequiredUIAccess
        public void afterFailedCheckIn() {
            moveToFailedList(
                myChangeList,
                myCommitMessage,
                getChangesFailedToCommit(),
                VcsLocalize.commitDialogFailedCommitTemplate(myChangeList.getName()).get(),
                myProject
            );
        }

        @Override
        public void doBeforeRefresh() {
            ChangeListManagerImpl clManager = (ChangeListManagerImpl)ChangeListManager.getInstance(myProject);
            clManager.showLocalChangesInvalidated();

            myAction = myProject.getApplication()
                .runReadAction((Supplier<LocalHistoryAction>)() -> LocalHistory.getInstance().startAction(myActionName.get()));
        }

        @Override
        public void customRefresh() {
            List<Change> toRefresh = new ArrayList<>();
            ChangesUtil.processChangesByVcs(myProject, myIncludedChanges, (vcs, items) -> {
                CheckinEnvironment ce = vcs.getCheckinEnvironment();
                if (ce != null && ce.isRefreshAfterCommitNeeded()) {
                    toRefresh.addAll(items);
                }
            });

            if (toRefresh.isEmpty()) {
                return;
            }

            ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            if (indicator != null) {
                indicator.setTextValue(VcsLocalize.commitDialogRefreshFiles());
            }
            RefreshVFsSynchronously.updateChanges(toRefresh);
        }

        @Override
        public Runnable postRefresh() {
            return () -> {
                // to be completely sure
                if (myAction != null) {
                    myAction.finish();
                }
                if (!myProject.isDisposed()) {
                    // after vcs refresh is completed, outdated notifiers should be removed if some exists...
                    ChangeListManager clManager = ChangeListManager.getInstance(myProject);
                    clManager.invokeAfterUpdate(
                        () -> {
                            if (myCommitSuccess) {
                                // do delete/ move of change list if needed
                                if (ChangeListsModificationAfterCommit.DELETE_LIST.equals(myAfterVcsRefreshModification)) {
                                    if (!myKeepChangeListAfterCommit) {
                                        clManager.removeChangeList(myChangeList.getName());
                                    }
                                }
                                else if (ChangeListsModificationAfterCommit.MOVE_OTHERS.equals(myAfterVcsRefreshModification)) {
                                    ChangelistMoveOfferDialog dialog = new ChangelistMoveOfferDialog(myConfiguration);
                                    if (dialog.showAndGet()) {
                                        Collection<Change> changes = clManager.getDefaultChangeList().getChanges();
                                        MoveChangesToAnotherListAction.askAndMove(myProject, changes, null);
                                    }
                                }
                            }
                            CommittedChangesCache cache = CommittedChangesCache.getInstance(myProject);
                            // in background since commit must have authorized
                            cache.refreshAllCachesAsync(false, true);
                            cache.refreshIncomingChangesAsync();
                        },
                        InvokeAfterUpdateMode.SILENT,
                        null,
                        vcsDirtyScopeManager -> {
                            for (FilePath path : myPathsToRefresh) {
                                vcsDirtyScopeManager.fileDirty(path);
                            }
                        },
                        null
                    );

                    LocalHistory.getInstance().putSystemLabel(myProject, myActionName + ": " + myCommitMessage);
                }
            };
        }

        private void vcsRefresh() {
            for (FilePath path : myPathsToRefresh) {
                myDirtyScopeManager.fileDirty(path);
            }
        }

        @Override
        public void doVcsRefresh() {
            myProject.getApplication().runReadAction(this::vcsRefresh);
        }
    }

    private void markCommittingDocuments() {
        myCommittingDocuments.addAll(markCommittingDocuments(myProject, myIncludedChanges));
    }

    private void unmarkCommittingDocuments() {
        unmarkCommittingDocuments(myCommittingDocuments);
        myCommittingDocuments.clear();
    }

    /**
     * Marks {@link Document documents} related to the given changes as "being committed".
     *
     * @return documents which were marked that way.
     * @see #unmarkCommittingDocuments(Collection)
     * @see VetoSavingCommittingDocumentsAdapter
     */
    @Nonnull
    public static Collection<Document> markCommittingDocuments(@Nonnull Project project, @Nonnull List<Change> changes) {
        Collection<Document> committingDocs = new ArrayList<>();
        for (Change change : changes) {
            Document doc = ChangesUtil.getFilePath(change).getDocument();
            if (doc != null) {
                doc.putUserData(DOCUMENT_BEING_COMMITTED_KEY, project);
                committingDocs.add(doc);
            }
        }
        return committingDocs;
    }

    /**
     * Removes the "being committed marker" from the given {@link Document documents}.
     *
     * @see #markCommittingDocuments(Project, List)
     * @see VetoSavingCommittingDocumentsAdapter
     */
    public static void unmarkCommittingDocuments(@Nonnull Collection<Document> committingDocs) {
        for (Document doc : committingDocs) {
            doc.putUserData(DOCUMENT_BEING_COMMITTED_KEY, null);
        }
    }

    private void commitCompleted(List<VcsException> allExceptions, GeneralCommitProcessor processor) {
        List<VcsException> errors = collectErrors(allExceptions);
        int errorsSize = errors.size();
        int warningsSize = allExceptions.size() - errorsSize;

        if (errorsSize == 0) {
            for (CheckinHandler handler : myHandlers) {
                handler.checkinSuccessful();
            }

            processor.afterSuccessfulCheckIn();
            if (myCustomResultHandler != null) {
                myCustomResultHandler.onSuccess(myCommitMessage);
            }
        }
        else {
            for (CheckinHandler handler : myHandlers) {
                handler.checkinFailed(errors);
            }
        }

        if (errorsSize == 0 && warningsSize == 0) {
            ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            if (indicator != null) {
                indicator.setTextValue(VcsLocalize.commitDialogCompletedSuccessfully());
            }
        }
        else if (myCustomResultHandler == null) {
            showErrorDialogAndMoveToAnotherList(processor, errorsSize, warningsSize, errors);
        }
        else {
            myCustomResultHandler.onFailure();
        }
    }

    private void showErrorDialogAndMoveToAnotherList(
        GeneralCommitProcessor processor,
        int errorsSize,
        int warningsSize,
        @Nonnull List<VcsException> errors
    ) {
        WaitForProgressToShow.runOrInvokeLaterAboveProgress(
            () -> {
                String message;
                if (errorsSize > 0 && warningsSize > 0) {
                    message = VcsLocalize.messageTextCommitFailedWithErrorsAndWarnings().get();
                }
                else if (errorsSize > 0) {
                    message = StringUtil.pluralize(VcsBundle.message("message.text.commit.failed.with.error"), errorsSize);
                }
                else {
                    message = StringUtil.pluralize(VcsLocalize.messageTextCommitFinishedWithWarnings().get(), warningsSize);
                }
                message += ":\n" + StringUtil.join(errors, Throwable::getMessage, "\n");
                //new VcsBalloonProblemNotifier(myProject, message, MessageType.ERROR).run();
                Messages.showErrorDialog(message, VcsLocalize.messageTitleCommit().get());

                if (errorsSize > 0) {
                    processor.afterFailedCheckIn();
                }
            },
            null,
            myProject
        );
    }

    @RequiredUIAccess
    public static void moveToFailedList(
        ChangeList changeList,
        String commitMessage,
        List<Change> failedChanges,
        String newChangelistName,
        Project project
    ) {
        // No need to move since we'll get exactly the same changelist.
        if (failedChanges.containsAll(changeList.getChanges())) {
            return;
        }

        final VcsConfiguration configuration = VcsConfiguration.getInstance(project);
        if (configuration.MOVE_TO_FAILED_COMMIT_CHANGELIST != VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY) {
            VcsShowConfirmationOption option = new VcsShowConfirmationOption() {
                @Override
                public Value getValue() {
                    return configuration.MOVE_TO_FAILED_COMMIT_CHANGELIST;
                }

                @Override
                public void setValue(Value value) {
                    configuration.MOVE_TO_FAILED_COMMIT_CHANGELIST = value;
                }

                @Override
                public boolean isPersistent() {
                    return true;
                }
            };
            boolean result = ConfirmationDialog.requestForConfirmation(
                option,
                project,
                VcsLocalize.commitFailedConfirmPrompt(),
                VcsLocalize.commitFailedConfirmTitle(),
                UIUtil.getQuestionIcon()
            );
            if (!result) {
                return;
            }
        }

        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        int index = 1;
        String failedListName = newChangelistName;
        while (changeListManager.findChangeList(failedListName) != null) {
            index++;
            failedListName = newChangelistName + " (" + index + ")";
        }

        LocalChangeList failedList = changeListManager.addChangeList(failedListName, commitMessage);
        changeListManager.moveChangesTo(failedList, failedChanges.toArray(new Change[failedChanges.size()]));
    }

    private static List<VcsException> collectErrors(List<VcsException> vcsExceptions) {
        List<VcsException> result = new ArrayList<>();
        for (VcsException vcsException : vcsExceptions) {
            if (!vcsException.isWarning()) {
                result.add(vcsException);
            }
        }
        return result;
    }
}
