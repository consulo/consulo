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

import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.ide.impl.idea.ide.util.DelegatingProgressIndicator;
import consulo.project.ui.notification.NotificationType;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.document.Document;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.application.util.function.Computable;
import consulo.util.dataholder.Key;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.*;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.MoveChangesToAnotherListAction;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.CommittedChangesCache;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.ide.impl.idea.openapi.vcs.ui.VcsBalloonProblemNotifier;
import consulo.ide.impl.idea.openapi.vcs.update.RefreshVFsSynchronously;
import consulo.ide.impl.idea.util.Consumer;
import consulo.ide.impl.idea.util.Function;
import consulo.ide.impl.idea.util.NullableFunction;
import consulo.ide.impl.idea.util.WaitForProgressToShow;
import consulo.application.util.Semaphore;
import consulo.ide.impl.idea.util.ui.ConfirmationDialog;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class CommitHelper {
  public static final Key<Object> DOCUMENT_BEING_COMMITTED_KEY = new Key<Object>("DOCUMENT_BEING_COMMITTED");

  private final static Logger LOG = Logger.getInstance(CommitHelper.class);
  private final Project myProject;

  private final ChangeList myChangeList;
  private final List<Change> myIncludedChanges;

  private final String myActionName;
  private final String myCommitMessage;

  private final List<CheckinHandler> myHandlers;
  private final boolean myAllOfDefaultChangeListChangesIncluded;
  private final boolean myForceSyncCommit;
  private final NullableFunction<Object, Object> myAdditionalData;
  @Nullable private final CommitResultHandler myCustomResultHandler;
  private final List<Document> myCommittingDocuments = new ArrayList<Document>();
  private final VcsConfiguration myConfiguration;
  private final VcsDirtyScopeManager myDirtyScopeManager;
  private final HashSet<String> myFeedback;

  public CommitHelper(final Project project,
                      final ChangeList changeList,
                      final List<Change> includedChanges,
                      final String actionName,
                      final String commitMessage,
                      final List<CheckinHandler> handlers,
                      final boolean allOfDefaultChangeListChangesIncluded,
                      final boolean synchronously,
                      final NullableFunction<Object, Object> additionalDataHolder,
                      @Nullable CommitResultHandler customResultHandler) {
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
    myFeedback = new HashSet<String>();
  }

  public boolean doCommit() {
    return doCommit((AbstractVcs)null);
  }

  public boolean doCommit(@Nullable AbstractVcs vcs) {
    return doCommit(new CommitProcessor(vcs));
  }

  public boolean doAlienCommit(final AbstractVcs vcs) {
    return doCommit(new AlienCommitProcessor(vcs));
  }

  private boolean doCommit(final GeneralCommitProcessor processor) {

    final Runnable action = new Runnable() {
      @Override
      public void run() {
        delegateCommitToVcsThread(processor);
      }
    };

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
        public void run(@Nonnull final ProgressIndicator indicator) {
          final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance((Project)myProject);
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

  private void delegateCommitToVcsThread(final GeneralCommitProcessor processor) {
    final ProgressIndicator indicator = new DelegatingProgressIndicator();

    final Semaphore endSemaphore = new Semaphore();
    endSemaphore.down();

    ChangeListManagerImpl.getInstanceImpl(myProject).executeOnUpdaterThread(new Runnable() {
      @Override
      public void run() {
        indicator.setText("Performing VCS commit...");
        try {
          ProgressManager.getInstance().runProcess(new Runnable() {
            @Override
            public void run() {
              indicator.checkCanceled();
              generalCommit(processor);
            }
          }, indicator);
        }
        finally {
          endSemaphore.up();
        }
      }
    });

    indicator.setText("Waiting for VCS background tasks to finish...");
    while (!endSemaphore.waitFor(20)) {
      indicator.checkCanceled();
    }
  }

  private String reportResult(GeneralCommitProcessor processor) {
    final List<Change> changesFailedToCommit = processor.getChangesFailedToCommit();

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
    VcsBalloonProblemNotifier.NOTIFICATION_GROUP.createNotification(content.toString(), notificationType).notify(myProject);
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
    final String[] FROM = {"<", ">"};
    final String[] TO = {"&lt;", "&gt;"};
    return StringUtil.replace(s, FROM, TO);
  }

  private static boolean doesntContainErrors(final List<VcsException> vcsExceptions) {
    for (VcsException vcsException : vcsExceptions) {
      if (!vcsException.isWarning()) return false;
    }
    return true;
  }

  private void generalCommit(final GeneralCommitProcessor processor) {
    try {
      final Application appManager = ApplicationManager.getApplication();
      appManager.runReadAction(new Runnable() {
        @Override
        public void run() {
          markCommittingDocuments();
        }
      });

      try {
        processor.callSelf();
      }
      finally {
        appManager.runReadAction(new Runnable() {
          @Override
          public void run() {
            unmarkCommittingDocuments();
          }
        });
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
      WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
        @Override
        public void run() {
          final Runnable runnable = processor.postRefresh();
          if (runnable != null) {
            runnable.run();
          }
        }
      }, null, myProject);
    }
  }

  private class AlienCommitProcessor extends GeneralCommitProcessor {
    private final AbstractVcs myVcs;

    private AlienCommitProcessor(final AbstractVcs vcs) {
      myVcs = vcs;
    }

    @Override
    public void callSelf() {
      ChangesUtil.processItemsByVcs(myIncludedChanges, new ChangesUtil.VcsSeparator<Change>() {
        @Override
        public AbstractVcs getVcsFor(final Change item) {
          return myVcs;
        }
      }, this);
    }

    @Override
    public void process(final AbstractVcs vcs, final List<Change> items) {
      if (myVcs.getName().equals(vcs.getName())) {
        final CheckinEnvironment environment = vcs.getCheckinEnvironment();
        if (environment != null) {
          Collection<FilePath> paths = ChangesUtil.getPaths(items);
          myPathsToRefresh.addAll(paths);

          final List<VcsException> exceptions = environment.commit(items, myCommitMessage, myAdditionalData, myFeedback);
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
      myPathsToRefresh = new ArrayList<FilePath>();
      myVcsExceptions = new ArrayList<VcsException>();
      myChangesFailedToCommit = new ArrayList<Change>();
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
    @Nullable private final AbstractVcs myVcs;

    private CommitProcessor(@Nullable AbstractVcs vcs) {
      myVcs = vcs;
      myAfterVcsRefreshModification = ChangeListsModificationAfterCommit.NOTHING;
      if (myChangeList instanceof LocalChangeList) {
        final LocalChangeList localList = (LocalChangeList)myChangeList;
        final boolean containsAll = new HashSet<Change>(myIncludedChanges).containsAll(new HashSet<Change>(myChangeList.getChanges()));
        if (containsAll && !localList.isDefault() && !localList.isReadOnly()) {
          myAfterVcsRefreshModification = ChangeListsModificationAfterCommit.DELETE_LIST;
        }
        else if (myConfiguration.OFFER_MOVE_TO_ANOTHER_CHANGELIST_ON_PARTIAL_COMMIT && (!containsAll) &&
                 localList.isDefault() && myAllOfDefaultChangeListChangesIncluded) {
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
    public void process(final AbstractVcs vcs, final List<Change> items) {
      final CheckinEnvironment environment = vcs.getCheckinEnvironment();
      if (environment != null) {
        Collection<FilePath> paths = ChangesUtil.getPaths(items);
        myPathsToRefresh.addAll(paths);
        if (environment.keepChangeListAfterCommit(myChangeList)) {
          myKeepChangeListAfterCommit = true;
        }
        final List<VcsException> exceptions = environment.commit(items, myCommitMessage, myAdditionalData, myFeedback);
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
    public void afterFailedCheckIn() {
      moveToFailedList(myChangeList, myCommitMessage, getChangesFailedToCommit(),
                       VcsBundle.message("commit.dialog.failed.commit.template", myChangeList.getName()), myProject);
    }

    @Override
    public void doBeforeRefresh() {
      final ChangeListManagerImpl clManager = (ChangeListManagerImpl)ChangeListManager.getInstance(myProject);
      clManager.showLocalChangesInvalidated();

      myAction = ApplicationManager.getApplication().runReadAction(new Computable<LocalHistoryAction>() {
        @Override
        public LocalHistoryAction compute() {
          return LocalHistory.getInstance().startAction(myActionName);
        }
      });
    }

    @Override
    public void customRefresh() {
      final List<Change> toRefresh = new ArrayList<Change>();
      ChangesUtil.processChangesByVcs(myProject, myIncludedChanges, new ChangesUtil.PerVcsProcessor<Change>() {
        @Override
        public void process(AbstractVcs vcs, List<Change> items) {
          CheckinEnvironment ce = vcs.getCheckinEnvironment();
          if (ce != null && ce.isRefreshAfterCommitNeeded()) {
            toRefresh.addAll(items);
          }
        }
      });

      if (toRefresh.isEmpty()) {
        return;
      }

      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.setText(VcsBundle.message("commit.dialog.refresh.files"));
      }
      RefreshVFsSynchronously.updateChanges(toRefresh);
    }

    @Override
    public Runnable postRefresh() {
      return new Runnable() {
        @Override
        public void run() {
          // to be completely sure
          if (myAction != null) {
            myAction.finish();
          }
          if (!myProject.isDisposed()) {
            // after vcs refresh is completed, outdated notifiers should be removed if some exists...
            final ChangeListManager clManager = ChangeListManager.getInstance(myProject);
            clManager.invokeAfterUpdate(new Runnable() {
                                          @Override
                                          public void run() {
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
                                                  final Collection<Change> changes = clManager.getDefaultChangeList().getChanges();
                                                  MoveChangesToAnotherListAction.askAndMove(myProject, changes, null);
                                                }
                                              }
                                            }
                                            final CommittedChangesCache cache = CommittedChangesCache.getInstance(myProject);
                                            // in background since commit must have authorized
                                            cache.refreshAllCachesAsync(false, true);
                                            cache.refreshIncomingChangesAsync();
                                          }
                                        }, InvokeAfterUpdateMode.SILENT, null, new Consumer<VcsDirtyScopeManager>() {
                                          @Override
                                          public void consume(final VcsDirtyScopeManager vcsDirtyScopeManager) {
                                            for (FilePath path : myPathsToRefresh) {
                                              vcsDirtyScopeManager.fileDirty(path);
                                            }
                                          }
                                        }, null
            );

            LocalHistory.getInstance().putSystemLabel(myProject, myActionName + ": " + myCommitMessage);
          }
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
      ApplicationManager.getApplication().runReadAction(new Runnable() {
        @Override
        public void run() {
          vcsRefresh();
        }
      });
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
   * @see #unmarkCommittingDocuments(java.util.Collection)
   * @see VetoSavingCommittingDocumentsAdapter
   */
  @Nonnull
  public static Collection<Document> markCommittingDocuments(@Nonnull Project project, @Nonnull List<Change> changes) {
    Collection<Document> committingDocs = new ArrayList<Document>();
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
   * @see #markCommittingDocuments(Project, java.util.List)
   * @see VetoSavingCommittingDocumentsAdapter
   */
  public static void unmarkCommittingDocuments(@Nonnull Collection<Document> committingDocs) {
    for (Document doc : committingDocs) {
      doc.putUserData(DOCUMENT_BEING_COMMITTED_KEY, null);
    }
  }

  private void commitCompleted(final List<VcsException> allExceptions, final GeneralCommitProcessor processor) {
    final List<VcsException> errors = collectErrors(allExceptions);
    final int errorsSize = errors.size();
    final int warningsSize = allExceptions.size() - errorsSize;

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

    if ((errorsSize == 0) && (warningsSize == 0)) {
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.setText(VcsBundle.message("commit.dialog.completed.successfully"));
      }
    }
    else {
      if (myCustomResultHandler == null) {
        showErrorDialogAndMoveToAnotherList(processor, errorsSize, warningsSize, errors);
      }
      else {
        myCustomResultHandler.onFailure();
      }
    }
  }

  private void showErrorDialogAndMoveToAnotherList(final GeneralCommitProcessor processor,
                                                   final int errorsSize,
                                                   final int warningsSize,
                                                   @Nonnull final List<VcsException> errors) {
    WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
      @Override
      public void run() {
        String message;
        if (errorsSize > 0 && warningsSize > 0) {
          message = VcsBundle.message("message.text.commit.failed.with.errors.and.warnings");
        }
        else if (errorsSize > 0) {
          message = StringUtil.pluralize(VcsBundle.message("message.text.commit.failed.with.error"), errorsSize);
        }
        else {
          message = StringUtil.pluralize(VcsBundle.message("message.text.commit.finished.with.warning"), warningsSize);
        }
        message += ":\n" + StringUtil.join(errors, new Function<VcsException, String>() {
          @Override
          public String fun(VcsException e) {
            return e.getMessage();
          }
        }, "\n");
        //new VcsBalloonProblemNotifier(myProject, message, MessageType.ERROR).run();
        Messages.showErrorDialog(message, VcsBundle.message("message.title.commit"));

        if (errorsSize > 0) {
          processor.afterFailedCheckIn();
        }
      }
    }, null, myProject);
  }

  public static void moveToFailedList(final ChangeList changeList,
                                      final String commitMessage,
                                      final List<Change> failedChanges,
                                      final String newChangelistName,
                                      final Project project) {
    // No need to move since we'll get exactly the same changelist.
    if (failedChanges.containsAll(changeList.getChanges())) return;

    final VcsConfiguration configuration = VcsConfiguration.getInstance(project);
    if (configuration.MOVE_TO_FAILED_COMMIT_CHANGELIST != VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY) {
      final VcsShowConfirmationOption option = new VcsShowConfirmationOption() {
        @Override
        public Value getValue() {
          return configuration.MOVE_TO_FAILED_COMMIT_CHANGELIST;
        }

        @Override
        public void setValue(final Value value) {
          configuration.MOVE_TO_FAILED_COMMIT_CHANGELIST = value;
        }

        @Override
        public boolean isPersistent() {
          return true;
        }
      };
      boolean result = ConfirmationDialog
              .requestForConfirmation(option, project, VcsBundle.message("commit.failed.confirm.prompt"), VcsBundle.message("commit.failed.confirm.title"),
                                      Messages.getQuestionIcon());
      if (!result) return;
    }

    final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
    int index = 1;
    String failedListName = newChangelistName;
    while (changeListManager.findChangeList(failedListName) != null) {
      index++;
      failedListName = newChangelistName + " (" + index + ")";
    }

    final LocalChangeList failedList = changeListManager.addChangeList(failedListName, commitMessage);
    changeListManager.moveChangesTo(failedList, failedChanges.toArray(new Change[failedChanges.size()]));
  }

  private static List<VcsException> collectErrors(final List<VcsException> vcsExceptions) {
    final ArrayList<VcsException> result = new ArrayList<VcsException>();
    for (VcsException vcsException : vcsExceptions) {
      if (!vcsException.isWarning()) {
        result.add(vcsException);
      }
    }
    return result;
  }
}
