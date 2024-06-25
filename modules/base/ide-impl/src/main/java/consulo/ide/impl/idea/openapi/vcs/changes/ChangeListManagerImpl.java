/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessToken;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.progress.SensitiveProgressWrapper;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.Semaphore;
import consulo.application.util.function.Processor;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.EditorNotifications;
import consulo.ide.impl.idea.openapi.util.Disposer;
import consulo.ide.impl.idea.openapi.vcs.CalledInAwt;
import consulo.ide.impl.idea.openapi.vcs.VcsConnectionProblem;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.ChangeListRemoveConfirmation;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.ScheduleForAdditionAction;
import consulo.ide.impl.idea.openapi.vcs.changes.conflicts.ChangelistConflictTracker;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.CommitHelper;
import consulo.ide.impl.idea.openapi.vcs.impl.AbstractVcsHelperImpl;
import consulo.ide.impl.idea.openapi.vcs.readOnlyHandler.ReadonlyStatusHandlerImpl;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.ide.impl.idea.util.ExceptionUtil;
import consulo.ide.impl.idea.util.FunctionUtil;
import consulo.ide.impl.idea.util.SlowOperations;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import consulo.util.lang.function.Condition;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.impl.internal.VcsRootIterator;
import consulo.versionControlSystem.impl.internal.change.*;
import consulo.versionControlSystem.impl.internal.change.ui.ChangeListDeltaListener;
import consulo.versionControlSystem.internal.ChangeListAvailabilityListener;
import consulo.versionControlSystem.internal.ChangeListManagerEx;
import consulo.versionControlSystem.internal.ChangesViewEx;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@State(name = "ChangeListManager", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
@Singleton
@ServiceImpl
public class ChangeListManagerImpl extends ChangeListManagerEx implements ChangeListOwner, Disposable, PersistentStateComponent<Element> {
  public static final Logger LOG = Logger.getInstance(ChangeListManagerImpl.class);
  private static final String EXCLUDED_CONVERTED_TO_IGNORED_OPTION = "EXCLUDED_CONVERTED_TO_IGNORED";

  private final Project myProject;
  private final VcsConfiguration myConfig;
  private final ChangesViewI myChangesViewManager;
  private final FileStatusManager myFileStatusManager;
  private final UpdateRequestsQueue myUpdater;

  private final Modifier myModifier;

  private FileHolderComposite myComposite;
  private final Disposable myUpdateDisposable = Disposable.newDisposable();
  private boolean myInitialUpdate = true;
  @Nullable
  private List<LocalChangeListImpl> myDisabledWorkerState;

  private ChangeListWorker myWorker;
  private VcsException myUpdateException;
  @Nonnull
  private List<Supplier<JComponent>> myAdditionalInfo = Collections.emptyList();

  private final EventDispatcher<ChangeListListener> myListeners = EventDispatcher.create(ChangeListListener.class);

  private final Object myDataLock = new Object();

  private final List<CommitExecutor> myExecutors = new ArrayList<>();

  private boolean myShowLocalChangesInvalidated;
  private final AtomicReference<String> myFreezeName;

  // notifies myListeners on the same thread that local changes update is done
  private final DelayedNotificator myDelayedNotificator;

  private final VcsListener myVcsListener = new VcsListener() {
    @Override
    public void directoryMappingChanged() {
      VcsDirtyScopeManager.getInstance(myProject).markEverythingDirty();
    }
  };
  private final ChangelistConflictTracker myConflictTracker;

  private final ChangeListScheduler myScheduler = new ChangeListScheduler();

  private boolean myModalNotificationsBlocked;
  @Nonnull
  private final Collection<LocalChangeList> myListsToBeDeleted = new HashSet<>();

  public static ChangeListManagerImpl getInstanceImpl(final Project project) {
    return (ChangeListManagerImpl)project.getInstance(ChangeListManager.class);
  }

  @Inject
  public ChangeListManagerImpl(
    Project project,
    VcsConfiguration config,
    ApplicationConcurrency applicationConcurrency,
    EditorNotifications editorNotifications
  ) {
    myProject = project;
    myConfig = config;
    myFreezeName = new AtomicReference<>(null);
    myConflictTracker = new ChangelistConflictTracker(project, this);
    myChangesViewManager = myProject.isDefault() ? new DummyChangesView(myProject) : ChangesViewManager.getInstance(myProject);
    myFileStatusManager = FileStatusManager.getInstance(myProject);
    myComposite = new FileHolderComposite(project);
    myIgnoredIdeaLevel = new IgnoredFilesComponent(myProject, true);
    myUpdater = new UpdateRequestsQueue(myProject, myScheduler, this::updateImmediately, this::hasNothingToUpdate);

    myDelayedNotificator = new DelayedNotificator(myProject, this, myScheduler);
    myWorker = new ChangeListWorker(myProject, myDelayedNotificator);
    myModifier = new Modifier(myWorker, myDelayedNotificator);

    myListeners.addListener(new ChangeListListener() {
      @Override
      public void defaultListChanged(final ChangeList oldDefaultList, ChangeList newDefaultList) {
        final LocalChangeList oldList = (LocalChangeList)oldDefaultList;
        if (oldDefaultList == null || oldList.hasDefaultName() || oldDefaultList.equals(newDefaultList)) return;

        scheduleAutomaticChangeListDeletionIfEmpty(oldList, config);
      }
    });

    MessageBusConnection busConnection = myProject.getMessageBus().connect(this);
    busConnection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
      @Override
      public void projectClosing(@Nonnull Project project) {
        if (project == myProject) {
          // Can't use Project disposable - it will be called after pending tasks are finished
          Disposer.dispose(myUpdateDisposable);
        }
      }
    });
  }

  @RequiredUIAccess
  private void updateChangeListAvailability() {
    if (myProject.isDisposed()) return;

    boolean enabled = shouldEnableChangeLists();
    synchronized (myDataLock) {
      if (enabled == myWorker.areChangeListsEnabled()) return;
    }

    myProject.getMessageBus().syncPublisher(ChangeListAvailabilityListener.TOPIC).onBefore();

    synchronized (myDataLock) {
      assert enabled != myWorker.areChangeListsEnabled();

      if (!enabled) {
        myDisabledWorkerState = myWorker.getChangeListsImpl();
      }

      myWorker.setChangeListsEnabled(enabled);

      if (enabled) {
        if (myDisabledWorkerState != null) {
          myWorker.setChangeLists(myDisabledWorkerState);
        }

        // Schedule refresh to replace FakeRevisions with actual changes
        VcsDirtyScopeManager.getInstance(myProject).markEverythingDirty();
        ChangesViewManager.getInstance(myProject).scheduleRefresh();
      }
    }

    myProject.getMessageBus().syncPublisher(ChangeListAvailabilityListener.TOPIC).onAfter();
  }

  private boolean shouldEnableChangeLists() {
    boolean forceDisable = CommitModeManager.getInstance(myProject).getCurrentCommitMode().hideLocalChangesTab() ||
      Registry.is("vcs.disable.changelists", false);
    return !forceDisable;
  }

  private void scheduleAutomaticChangeListDeletionIfEmpty(final LocalChangeList oldList, final VcsConfiguration config) {
    if (oldList.isReadOnly() || !oldList.getChanges().isEmpty()) return;

    invokeAfterUpdate(() -> {
      LocalChangeList actualList = getChangeList(oldList.getId());
      if (actualList == null) {
        return; // removed already
      }

      if (myModalNotificationsBlocked && config.REMOVE_EMPTY_INACTIVE_CHANGELISTS != VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY) {
        myListsToBeDeleted.add(oldList);
      }
      else {
        deleteEmptyChangeLists(Collections.singletonList(actualList));
      }
    }, InvokeAfterUpdateMode.SILENT, null, null);
  }

  private void deleteEmptyChangeLists(@Nonnull Collection<LocalChangeList> lists) {
    if (lists.isEmpty() || myConfig.REMOVE_EMPTY_INACTIVE_CHANGELISTS == VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY) {
      return;
    }

    ChangeListRemoveConfirmation.processLists(myProject, false, lists, new ChangeListRemoveConfirmation() {
      @Override
      public boolean askIfShouldRemoveChangeLists(@Nonnull List<? extends LocalChangeList> toAsk) {
        return myConfig.REMOVE_EMPTY_INACTIVE_CHANGELISTS != VcsShowConfirmationOption.Value.SHOW_CONFIRMATION || showRemoveEmptyChangeListsProposal(
          myConfig,
          toAsk);
      }
    });
  }

  /**
   * Shows the proposal to delete one or more changelists that were default and became empty.
   *
   * @return true if the changelists have to be deleted, false if not.
   */
  @NonNls
  private boolean showRemoveEmptyChangeListsProposal(@Nonnull final VcsConfiguration config,
                                                     @Nonnull Collection<? extends LocalChangeList> lists) {
    if (lists.isEmpty()) {
      return false;
    }

    final String question;
    if (lists.size() == 1) {
      question = String.format("<html>The empty changelist '%s' is no longer active.<br>Do you want to remove it?</html>",
                               StringUtil.first(lists.iterator().next().getName(), 30, true));
    }
    else {
      question = String.format("<html>Empty changelists<br/>%s are no longer active.<br>Do you want to remove them?</html>",
                               StringUtil.join(lists,
                                               (Function<LocalChangeList, String>)list -> StringUtil.first(list.getName(), 30, true),
                                               "<br/>"));
    }

    VcsConfirmationDialog dialog =
      new VcsConfirmationDialog(myProject, "Remove Empty Changelist", "Remove", "Cancel", new VcsShowConfirmationOption() {
        @Override
        public Value getValue() {
          return config.REMOVE_EMPTY_INACTIVE_CHANGELISTS;
        }

        @Override
        public void setValue(Value value) {
          config.REMOVE_EMPTY_INACTIVE_CHANGELISTS = value;
        }

        @Override
        public boolean isPersistent() {
          return true;
        }
      }, question, "&Remember my choice");
    return dialog.showAndGet();
  }

  @RequiredUIAccess
  @Override
  @CalledInAwt
  public void blockModalNotifications() {
    myModalNotificationsBlocked = true;
  }

  @RequiredUIAccess
  @Override
  @CalledInAwt
  public void unblockModalNotifications() {
    myModalNotificationsBlocked = false;
    deleteEmptyChangeLists(myListsToBeDeleted);
    myListsToBeDeleted.clear();
  }

  public void startUpdater() {
    myUpdater.initialized();
    BackgroundTaskUtil.syncPublisher(myProject, LocalChangeListsLoadedListener.class).processLoadedLists(getChangeLists());

    MessageBusConnection connection = myProject.getMessageBus().connect(this);
    connection.subscribe(VcsMappingListener.class, () -> VcsDirtyScopeManager.getInstance(myProject).markEverythingDirty());

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      myConflictTracker.startTracking();
    }
  }

  @Override
  public void dispose() {
    myUpdater.stop();
    myScheduler.shutdown();
  }

  /**
   * update itself might produce actions done on AWT thread (invoked-after),
   * so waiting for its completion on AWT thread is not good runnable is invoked on AWT thread
   */
  @Override
  public void invokeAfterUpdate(final Runnable afterUpdate,
                                final InvokeAfterUpdateMode mode,
                                @Nullable final String title,
                                @Nullable final ModalityState state) {
    myUpdater.invokeAfterUpdate(afterUpdate, mode, title);
  }

  @Override
  public void invokeAfterUpdate(
    final Runnable afterUpdate,
    final InvokeAfterUpdateMode mode,
    final String title,
    final Consumer<VcsDirtyScopeManager> dirtyScopeManagerFiller,
    final ModalityState state
  ) {
    myUpdater.invokeAfterUpdate(afterUpdate, mode, title, dirtyScopeManagerFiller, state);
  }

  public void freeze(@Nonnull String reason) {
    myUpdater.setIgnoreBackgroundOperation(true);
    Semaphore sem = new Semaphore();
    sem.down();

    invokeAfterUpdate(() -> {
      myUpdater.setIgnoreBackgroundOperation(false);
      myUpdater.pause();
      myFreezeName.set(reason);
      sem.up();
    }, InvokeAfterUpdateMode.SILENT_CALLBACK_POOLED, "", IdeaModalityState.defaultModalityState());

    boolean free = false;
    while (!free) {
      ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
      if (pi != null) pi.checkCanceled();
      free = sem.waitFor(500);
    }
  }

  @Override
  public void letGo() {
    unfreeze();
  }

  @Override
  public void unfreeze() {
    myUpdater.go();
    myFreezeName.set(null);
  }

  @Override
  public String isFreezed() {
    return myFreezeName.get();
  }

  @Override
  public void scheduleUpdate() {
    myUpdater.schedule();
  }

  @Override
  public void scheduleUpdate(boolean updateUnversionedFiles) {
    myUpdater.schedule();
  }

  /**
   * @return false if update was re-scheduled due to new 'markEverythingDirty' event, true otherwise.
   */
  private boolean updateImmediately() {
    return BackgroundTaskUtil.runUnderDisposeAwareIndicator(myUpdateDisposable, () -> {
      final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
      if (!vcsManager.hasActiveVcss()) return true;

      VcsDirtyScopeManagerImpl dirtyScopeManager = VcsDirtyScopeManagerImpl.getInstanceImpl(myProject);
      final VcsInvalidated invalidated = dirtyScopeManager.retrieveScopes();
      if (checkScopeIsEmpty(invalidated)) {
        LOG.debug("[update] - dirty scope is empty");
        dirtyScopeManager.changesProcessed();
        return true;
      }

      final boolean wasEverythingDirty = invalidated.isEverythingDirty();
      final List<VcsModifiableDirtyScope> scopes = invalidated.getScopes();

      boolean isInitialUpdate;
      ChangesViewEx changesView = ChangesViewManager.getInstanceEx(myProject);
      try {
        if (myUpdater.isStopped()) return true;

        // copy existing data to objects that would be updated.
        // mark for "modifier" that update started (it would create duplicates of modification commands done by user during update;
        // after update of copies of objects is complete, it would apply the same modifications to copies.)
        final DataHolder dataHolder;
        synchronized (myDataLock) {
          dataHolder = new DataHolder(myComposite.copy(), new ChangeListWorker.ChangeListUpdater(myWorker), wasEverythingDirty);
          myModifier.enterUpdate();
          if (wasEverythingDirty) {
            myUpdateException = null;
            myAdditionalInfo = Collections.emptyList();
          }

          if (LOG.isDebugEnabled()) {
            String scopeInString = StringUtil.join(scopes, Object::toString, "->\n");
            LOG.debug("refresh procedure started, everything: " + wasEverythingDirty + " dirty scope: " + scopeInString +
                        "\nignored: " + myComposite.getIgnoredFileHolder().getFiles().size() +
                        "\nunversioned: " + myComposite.getUnversionedFileHolder().getFiles().size() +
                        "\ncurrent changes: " + myWorker);
          }

          isInitialUpdate = myInitialUpdate;
          myInitialUpdate = false;
        }
        changesView.setBusy(true);
        changesView.scheduleRefresh();

        SensitiveProgressWrapper vcsIndicator = new SensitiveProgressWrapper(ProgressManager.getInstance().getProgressIndicator());
        if (!isInitialUpdate) invalidated.doWhenCanceled(() -> vcsIndicator.cancel());

        try {
          ProgressManager.getInstance().executeProcessUnderProgress(() -> {
            iterateScopes(dataHolder, scopes, vcsIndicator);
          }, vcsIndicator);
        }
        catch (ProcessCanceledException ignore) {
        }
        boolean wasCancelled = vcsIndicator.isCanceled();

        // for the case of project being closed we need a read action here -> to be more consistent
        ApplicationManager.getApplication().runReadAction(() -> {
          if (myProject.isDisposed()) return;

          synchronized (myDataLock) {
            ChangeListWorker updatedWorker = dataHolder.getUpdatedWorker();
            boolean takeChanges = myUpdateException == null && !wasCancelled &&
              updatedWorker.areChangeListsEnabled() == myWorker.areChangeListsEnabled();

            // update member from copy
            if (takeChanges) {
              dataHolder.finish();
              // do same modifications to change lists as was done during update + do delayed notifications
              myModifier.finishUpdate(updatedWorker);

              myWorker.applyChangesFromUpdate(updatedWorker, new MyChangesDeltaForwarder(myProject, myScheduler));

              if (LOG.isDebugEnabled()) {
                LOG.debug("refresh procedure finished, unversioned size: " +
                            dataHolder.getComposite().getUnversionedFileHolder().getFiles().size() +
                            "\nchanges: " + myWorker);
              }
              final boolean statusChanged = !myComposite.equals(dataHolder.getComposite());
              myComposite = dataHolder.getComposite();
              if (statusChanged) {
                boolean isUnchangedUpdating = isInUpdate() || isUnversionedInUpdateMode() || isIgnoredInUpdateMode();
                myDelayedNotificator.unchangedFileStatusChanged(!isUnchangedUpdating);
              }
              LOG.debug("[update] - success");
            }
            else {
              myModifier.finishUpdate(null);
              LOG.debug(String.format("[update] - aborted, wasCancelled: %s", wasCancelled));
            }
            myShowLocalChangesInvalidated = false;
          }
        });

        for (VcsDirtyScope scope : scopes) {
          if (scope.getVcs().isTrackingUnchangedContent()) {
            VcsRootIterator.iterateExistingInsideScope(scope, file -> {
              LastUnchangedContentTracker.markUntouched(file); //todo what if it has become dirty again during update?
              return true;
            });
          }
        }

        return !wasCancelled;
      }
      catch (ProcessCanceledException e) {
        // OK, we're finishing all the stuff now.
      }
      catch (Exception | AssertionError ex) {
        LOG.error(ex);
      }
      finally {
        dirtyScopeManager.changesProcessed();

        myDelayedNotificator.changedFileStatusChanged(!isInUpdate());
        myDelayedNotificator.changeListUpdateDone();
        changesView.scheduleRefresh();
      }
      return true;
    });
  }


  private static boolean checkScopeIsEmpty(VcsInvalidated invalidated) {
    if (invalidated == null) return true;
    if (invalidated.isEverythingDirty()) return false;
    return invalidated.isEmpty();
  }

  private void iterateScopes(@Nonnull DataHolder dataHolder,
                             @Nonnull List<? extends VcsModifiableDirtyScope> scopes,
                             @Nonnull ProgressIndicator indicator) {
    ChangeListWorker.ChangeListUpdater updater = dataHolder.getChangeListUpdater();
    FileHolderComposite composite = dataHolder.getComposite();
    Supplier<Boolean> disposedGetter = () -> myProject.isDisposed() || myUpdater.isStopped();

    List<Supplier<JComponent>> additionalInfos = new ArrayList<>();

    dataHolder.notifyStart();
    try {
      for (VcsModifiableDirtyScope scope : scopes) {
        indicator.checkCanceled();

        // do actual requests about file statuses
        UpdatingChangeListBuilder builder = new UpdatingChangeListBuilder(scope, updater, composite, disposedGetter);
        actualUpdate(builder, scope, dataHolder, updater, indicator);
        additionalInfos.addAll(builder.getAdditionalInfo());

        synchronized (myDataLock) {
          if (myUpdateException != null) break;
        }
      }
    }
    finally {
      dataHolder.notifyEnd();
    }

    synchronized (myDataLock) {
      myAdditionalInfo = additionalInfos;
    }
  }

  public boolean isUnversionedInUpdateMode() {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getUnversionedFileHolder().isInUpdatingMode();
      }
    });
  }

  private final class DataHolder {
    private final boolean myWasEverythingDirty;
    private final FileHolderComposite myComposite;
    private final ChangeListWorker.ChangeListUpdater myChangeListUpdater;

    private DataHolder(FileHolderComposite composite, ChangeListWorker.ChangeListUpdater changeListUpdater, boolean wasEverythingDirty) {
      myComposite = composite;
      myChangeListUpdater = changeListUpdater;
      myWasEverythingDirty = wasEverythingDirty;
    }

    private void notifyStart() {
      if (myWasEverythingDirty) {
        myComposite.cleanAll();
        myChangeListUpdater.notifyStartProcessingChanges(null);
      }
    }

    private void notifyStartProcessingChanges(@Nonnull VcsModifiableDirtyScope scope) {
      if (!myWasEverythingDirty) {
        myComposite.cleanUnderScope(scope);
        myChangeListUpdater.notifyStartProcessingChanges(scope);
      }

      myComposite.notifyVcsStarted(scope.getVcs());
    }

    private void notifyDoneProcessingChanges(@Nonnull VcsDirtyScope scope) {
      if (!myWasEverythingDirty) {
        myChangeListUpdater.notifyDoneProcessingChanges(myDelayedNotificator, scope);
      }
    }

    void notifyEnd() {
      if (myWasEverythingDirty) {
        myChangeListUpdater.notifyDoneProcessingChanges(myDelayedNotificator, null);
      }
    }

    public void finish() {
      myChangeListUpdater.finish();
    }

    @Nonnull
    public FileHolderComposite getComposite() {
      return myComposite;
    }

    @Nonnull
    public ChangeListWorker.ChangeListUpdater getChangeListUpdater() {
      return myChangeListUpdater;
    }

    @Nonnull
    public ChangeListWorker getUpdatedWorker() {
      return myChangeListUpdater.getUpdatedWorker();
    }
  }

  private void actualUpdate(@Nonnull UpdatingChangeListBuilder builder,
                            @Nonnull VcsModifiableDirtyScope scope,
                            @Nonnull DataHolder dataHolder,
                            @Nonnull ChangeListManagerGate gate,
                            @Nonnull ProgressIndicator indicator) {
    dataHolder.notifyStartProcessingChanges(scope);
    try {
      AbstractVcs vcs = scope.getVcs();
      ChangeProvider changeProvider = vcs.getChangeProvider();
      if (changeProvider != null) {
        //StructuredIdeActivity activity = VcsStatisticsCollector.logClmRefresh(myProject, vcs, scope.wasEveryThingDirty());
        changeProvider.getChanges(scope, builder, indicator, gate);
        //activity.finished();
      }
    }
    catch (VcsException e) {
      handleUpdateException(e);
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable t) {
      LOG.debug(t);
      ExceptionUtil.rethrow(t);
    }
    finally {
      if (!myUpdater.isStopped()) {
        dataHolder.notifyDoneProcessingChanges(scope);
      }
    }
  }

  private void handleUpdateException(final VcsException e) {
    LOG.info(e);

    if (e instanceof VcsConnectionProblem) {
      ApplicationManager.getApplication().invokeLater(() -> ((VcsConnectionProblem)e).attemptQuickFix(false));
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      AbstractVcsHelper helper = AbstractVcsHelper.getInstance(myProject);
      if (helper instanceof AbstractVcsHelperImpl && ((AbstractVcsHelperImpl)helper).handleCustom(e)) {
        return;
      }
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
    }

    synchronized (myDataLock) {
      myUpdateException = e;
    }
  }

  private void checkIfDisposed() {
    if (myUpdater.isStopped()) throw new ProcessCanceledException();
  }

  public static boolean isUnder(final Change change, final VcsDirtyScope scope) {
    final ContentRevision before = change.getBeforeRevision();
    final ContentRevision after = change.getAfterRevision();
    return before != null && scope.belongsTo(before.getFile()) || after != null && scope.belongsTo(after.getFile());
  }

  @Override
  @Nonnull
  public List<LocalChangeList> getChangeLists() {
    synchronized (myDataLock) {
      return myWorker.getChangeLists();
    }
  }

  @Nonnull
  @Override
  public List<LocalChangeList> getChangeLists(@Nonnull Change change) {
    return getAffectedLists(Collections.singletonList(change));
  }

  @Nonnull
  @Override
  public List<LocalChangeList> getChangeLists(@Nonnull VirtualFile file) {
    synchronized (myDataLock) {
      Change change = myWorker.getChangeForPath(VcsUtil.getFilePath(file));
      if (change == null) return Collections.emptyList();
      return getChangeLists(change);
    }
  }

  @Nonnull
  @Override
  public List<File> getAffectedPaths() {
    List<FilePath> filePaths;
    synchronized (myDataLock) {
      filePaths = myWorker.getAffectedPaths();
    }
    return consulo.util.collection.ContainerUtil.mapNotNull(filePaths, FilePath::getIOFile);
  }

  @Override
  @Nonnull
  public List<LocalChangeList> getAffectedLists(@Nonnull Collection<? extends Change> changes) {
    synchronized (myDataLock) {
      return myWorker.getAffectedLists(changes);
    }
  }

  @Override
  @Nonnull
  public List<VirtualFile> getAffectedFiles() {
    List<FilePath> filePaths;
    synchronized (myDataLock) {
      filePaths = myWorker.getAffectedPaths();
    }
    return consulo.util.collection.ContainerUtil.mapNotNull(filePaths, FilePath::getVirtualFile);
  }

  @Override
  @Nonnull
  public Collection<Change> getAllChanges() {
    synchronized (myDataLock) {
      return myWorker.getAllChanges();
    }
  }

  @Nonnull
  @Override
  public List<FilePath> getUnversionedFilesPaths() {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return new ArrayList<>(myComposite.getUnversionedFileHolder().getFiles());
      }
    });
  }

  @Override
  public List<VirtualFile> getModifiedWithoutEditing() {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getModifiedWithoutEditingFileHolder().getFiles();
      }
    });
  }

  /**
   * @return only roots for ignored folders, and ignored files
   */
  @Nonnull
  public List<VirtualFile> getIgnoredFiles() {
    synchronized (myDataLock) {
      return new ArrayList<>(myComposite.getIgnoredFileHolder().values());
    }
  }

  boolean isIgnoredInUpdateMode() {
    return myComposite.getIgnoredFileHolder().isInUpdatingMode();
  }

  public List<VirtualFile> getLockedFolders() {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getLockedFileHolder().getFiles();
      }
    });
  }

  Map<VirtualFile, LogicalLock> getLogicallyLockedFolders() {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return new HashMap<>(myComposite.getLogicallyLockedFileHolder().getMap());
      }
    });
  }

  public boolean isLogicallyLocked(final VirtualFile file) {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getLogicallyLockedFileHolder().containsKey(file);
      }
    });
  }

  public boolean isContainedInLocallyDeleted(final FilePath filePath) {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getDeletedFileHolder().isContainedInLocallyDeleted(filePath);
      }
    });
  }

  public List<LocallyDeletedChange> getDeletedFiles() {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getDeletedFileHolder().getFiles();
      }
    });
  }

  public MultiMap<String, VirtualFile> getSwitchedFilesMap() {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getSwitchedFileHolder().getBranchToFileMap();
      }
    });
  }

  @Nullable
  Map<VirtualFile, String> getSwitchedRoots() {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getRootSwitchFileHolder().getFilesMapCopy();
      }
    });
  }

  public VcsException getUpdateException() {
    synchronized (myDataLock) {
      return myUpdateException;
    }
  }

  public @Nonnull List<Supplier<JComponent>> getAdditionalUpdateInfo() {
    synchronized (myDataLock) {
      List<Supplier<JComponent>> updateInfo = new ArrayList<>();
      if (myUpdateException != null) {
        String errorMessage = VcsBundle.message("error.updating.changes", myUpdateException.getMessage());
        updateInfo.add(ChangesViewManager.createTextStatusFactory(errorMessage, true));
      }
      updateInfo.addAll(myAdditionalInfo);
      return updateInfo;
    }
  }

  @Override
  public boolean isFileAffected(final VirtualFile file) {
    synchronized (myDataLock) {
      return myWorker.getStatus(file) != null;
    }
  }

  @Override
  @Nullable
  public LocalChangeList findChangeList(final String name) {
    synchronized (myDataLock) {
      return myWorker.getChangeListByName(name);
    }
  }

  @Override
  @Nullable
  public LocalChangeList getChangeList(@Nullable String id) {
    synchronized (myDataLock) {
      return myWorker.getChangeListById(id);
    }
  }

  private void scheduleChangesViewRefresh() {
    if (!myProject.isDisposed()) {
      ChangesViewManager.getInstance(myProject).scheduleRefresh();
    }
  }

  @Nonnull
  @Override
  public LocalChangeList addChangeList(@Nonnull final String name, @Nullable final String comment, @Nullable final ChangeListData data) {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        final LocalChangeList changeList = myModifier.addChangeList(name, comment, data);
        scheduleChangesViewRefresh();
        return changeList;
      }
    });
  }

  @Override
  public void removeChangeList(final String name) {
    ApplicationManager.getApplication().runReadAction(() -> {
      synchronized (myDataLock) {
        myModifier.removeChangeList(name);
        myChangesViewManager.scheduleRefresh();
      }
    });
  }

  @Override
  public void removeChangeList(LocalChangeList list) {
    removeChangeList(list.getName());
  }

  /**
   * does no modification to change lists, only notification is sent
   */
  @Override
  @Nonnull
  public Runnable prepareForChangeDeletion(final Collection<Change> changes) {
    final Map<String, LocalChangeList> lists = new HashMap<>();
    final Map<String, List<Change>> map;
    synchronized (myDataLock) {
      map = myWorker.listsForChanges(changes, lists);
    }
    return () -> {
      ApplicationManager.getApplication().runReadAction(() -> {
        synchronized (myDataLock) {
          for (Map.Entry<String, List<Change>> entry : map.entrySet()) {
            final List<Change> changes1 = entry.getValue();
            for (Iterator<Change> iterator = changes1.iterator(); iterator.hasNext(); ) {
              final Change change = iterator.next();
              if (getChangeList(change) != null) {
                // was not actually rolled back
                iterator.remove();
              }
            }
            myDelayedNotificator.changesRemoved(changes1, lists.get(entry.getKey()));
          }
          for (String listName : map.keySet()) {
            final LocalChangeList byName = myWorker.getCopyByName(listName);
            if (byName != null && !byName.isDefault()) {
              scheduleAutomaticChangeListDeletionIfEmpty(byName, myConfig);
            }
          }
        }
      });
    };
  }

  public void setDefaultChangeList(@Nonnull String name, boolean automatic) {
    ApplicationManager.getApplication().runReadAction(() -> {
      synchronized (myDataLock) {
        myModifier.setDefault(name, automatic);
        scheduleChangesViewRefresh();
      }
    });
  }

  @Override
  public void setDefaultChangeList(@Nonnull String name) {
    setDefaultChangeList(name, false);
  }

  @Override
  public void setDefaultChangeList(@Nonnull final LocalChangeList list) {
    setDefaultChangeList(list, false);
  }

  @Override
  public void setDefaultChangeList(@Nonnull final LocalChangeList list, boolean automatic) {
    setDefaultChangeList(list.getName(), automatic);
  }

  @Nonnull
  @Override
  public LocalChangeList getDefaultChangeList() {
    synchronized (myDataLock) {
      return myWorker.getDefaultList();
    }
  }

  @Override
  @Nonnull
  public Collection<LocalChangeList> getInvolvedListsFilterChanges(final Collection<Change> changes, final List<Change> validChanges) {
    synchronized (myDataLock) {
      return myWorker.getInvolvedListsFilterChanges(changes, validChanges);
    }
  }

  @Override
  @Nullable
  public LocalChangeList getChangeList(@Nonnull Change change) {
    synchronized (myDataLock) {
      return myWorker.listForChange(change);
    }
  }

  @Override
  public String getChangeListNameIfOnlyOne(final Change[] changes) {
    synchronized (myDataLock) {
      List<LocalChangeList> lists = myWorker.getAffectedLists(Arrays.asList(changes));
      return lists.size() == 1 ? lists.get(0).getName() : null;
    }
  }

  /**
   * @deprecated better use normal comparison, with equals
   */
  @Override
  @Nullable
  public LocalChangeList getIdentityChangeList(Change change) {
    synchronized (myDataLock) {
      final List<LocalChangeList> lists = myWorker.getListsCopy();
      for (LocalChangeList list : lists) {
        for (Change oldChange : list.getChanges()) {
          if (oldChange == change) {
            return list;
          }
        }
      }
      return null;
    }
  }

  @Override
  public boolean isInUpdate() {
    synchronized (myDataLock) {
      return myModifier.isInsideUpdate() || myShowLocalChangesInvalidated;
    }
  }

  @Override
  @Nullable
  public Change getChange(@Nonnull VirtualFile file) {
    return getChange(VcsUtil.getFilePath(file));
  }

  @Override
  public LocalChangeList getChangeList(@Nonnull VirtualFile file) {
    synchronized (myDataLock) {
      return myWorker.getListCopy(file);
    }
  }

  @Override
  @Nullable
  public Change getChange(final FilePath file) {
    synchronized (myDataLock) {
      return myWorker.getChangeForPath(file);
    }
  }

  @Override
  public boolean isUnversioned(VirtualFile file) {
    VcsRoot vcsRoot;
    try (AccessToken ignore = SlowOperations.knownIssue("IDEA-322445, EA-857508")) {
      vcsRoot = ProjectLevelVcsManager.getInstance(myProject).getVcsRootObjectFor(file);
      if (vcsRoot == null) return false;
    }

    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getUnversionedFileHolder().containsFile(VcsUtil.getFilePath(file), vcsRoot);
      }
    });
  }

  @Nonnull
  @Override
  public FileStatus getStatus(@Nonnull FilePath path) {
    return getStatus(path, path.getVirtualFile());
  }

  @Override
  @Nonnull
  public FileStatus getStatus(@Nonnull VirtualFile file) {
    return getStatus(VcsUtil.getFilePath(file), file);
  }

  @Nonnull
  private FileStatus getStatus(@Nonnull FilePath path, @Nullable VirtualFile file) {
    VcsRoot vcsRoot = file != null ? ProjectLevelVcsManager.getInstance(myProject).getVcsRootObjectFor(file)
      : ProjectLevelVcsManager.getInstance(myProject).getVcsRootObjectFor(path);
    if (vcsRoot == null) return FileStatus.NOT_CHANGED;

    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        if (myComposite.getUnversionedFileHolder().containsFile(path, vcsRoot)) return FileStatus.UNKNOWN;
        if (file != null && myComposite.getModifiedWithoutEditingFileHolder().containsFile(file)) return FileStatus.HIJACKED;
        if (myComposite.getIgnoredFileHolder().containsFile(path, vcsRoot)) return FileStatus.IGNORED;

        FileStatus status = ObjectUtil.notNull(myWorker.getStatus(path), FileStatus.NOT_CHANGED);

        if (file != null && FileStatus.NOT_CHANGED.equals(status)) {
          boolean switched = myComposite.getSwitchedFileHolder().containsFile(file);
          if (switched) return FileStatus.SWITCHED;
        }

        return status;
      }
    });
  }


  @Override
  @Nonnull
  public Collection<Change> getChangesIn(VirtualFile dir) {
    return getChangesIn(VcsUtil.getFilePath(dir));
  }

  @Nonnull
  @Override
  public ThreeState haveChangesUnder(@Nonnull final VirtualFile vf) {
    if (!vf.isValid() || !vf.isDirectory()) return ThreeState.NO;
    synchronized (myDataLock) {
      return myWorker.haveChangesUnder(vf);
    }
  }

  @Override
  @Nonnull
  public Collection<Change> getChangesIn(final FilePath dirPath) {
    return getAllChanges().stream().filter(change -> isChangeUnder(dirPath, change)).collect(Collectors.toSet());
  }

  private static boolean isChangeUnder(@Nonnull FilePath parent, @Nonnull Change change) {
    FilePath after = ChangesUtil.getAfterPath(change);
    FilePath before = ChangesUtil.getBeforePath(change);
    return after != null && after.isUnder(parent, false) ||
      !Comparing.equal(before, after) && before != null && before.isUnder(parent, false);
  }

  @Override
  public void moveChangesTo(@Nonnull LocalChangeList list, @Nonnull Change... changes) {
    moveChangesTo(list, ContainerUtil.skipNulls(Arrays.asList(changes)));
  }

  @Override
  public void moveChangesTo(@Nonnull LocalChangeList list, @Nonnull List<Change> changes) {
    ApplicationManager.getApplication().runReadAction(() -> {
      synchronized (myDataLock) {
        myModifier.moveChangesTo(list.getName(), changes);
        scheduleChangesViewRefresh();
      }
    });
  }

  @Override
  public void addUnversionedFiles(final LocalChangeList list, @Nonnull final List<VirtualFile> files) {
    ScheduleForAdditionAction.Manager.addUnversionedFilesToVcs(myProject, list, files);
  }

  @Nonnull
  public static Condition<FileStatus> getDefaultUnversionedFileCondition() {
    return status -> status == FileStatus.UNKNOWN;
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public void addChangeListListener(ChangeListListener listener) {
    myListeners.addListener(listener);
  }


  @Override
  public void removeChangeListListener(ChangeListListener listener) {
    myListeners.removeListener(listener);
  }

  @Override
  public void registerCommitExecutor(CommitExecutor executor) {
    myExecutors.add(executor);
  }

  @Override
  public void commitChanges(LocalChangeList changeList, List<Change> changes) {
    doCommit(changeList, changes, false);
  }

  private boolean doCommit(final LocalChangeList changeList, final List<Change> changes, final boolean synchronously) {
    FileDocumentManager.getInstance().saveAllDocuments();
    return new CommitHelper(myProject,
                            changeList,
                            changes,
                            changeList.getName(),
                            StringUtil.isEmpty(changeList.getComment()) ? changeList.getName() : changeList.getComment(),
                            new ArrayList<>(),
                            false,
                            synchronously,
                            FunctionUtil.nullConstant(),
                            null).doCommit();
  }

  @Override
  public void commitChangesSynchronously(LocalChangeList changeList, List<Change> changes) {
    doCommit(changeList, changes, true);
  }

  @Override
  public boolean commitChangesSynchronouslyWithResult(final LocalChangeList changeList, final List<Change> changes) {
    return doCommit(changeList, changes, true);
  }

  @Override
  public void loadState(Element element) {
    synchronized (myDataLock) {
      if (!myInitialUpdate) {
        LOG.warn("Local changes overwritten");
        VcsDirtyScopeManager.getInstance(myProject).markEverythingDirty();
      }

      boolean areChangeListsEnabled = shouldEnableChangeLists();
      myWorker.setChangeListsEnabled(areChangeListsEnabled);

      List<LocalChangeListImpl> changeLists = ChangeListManagerSerialization.readExternal(element, myProject);
      if (areChangeListsEnabled) {
        myWorker.setChangeLists(changeLists);
      }
      else {
        myDisabledWorkerState = changeLists;
      }
    }
    myConflictTracker.loadState(element);
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("state");
    synchronized (myDataLock) {
      boolean areChangeListsEnabled = myWorker.areChangeListsEnabled();
      List<? extends LocalChangeList> changesToSave = areChangeListsEnabled ? myWorker.getChangeLists() : myDisabledWorkerState;
      ChangeListManagerSerialization.writeExternal(element, changesToSave, areChangeListsEnabled);
    }
    myConflictTracker.saveState(element);
    return element;
  }

  // used in TeamCity
  @Override
  public void reopenFiles(List<FilePath> paths) {
    final ReadonlyStatusHandlerImpl readonlyStatusHandler = (ReadonlyStatusHandlerImpl)ReadonlyStatusHandler.getInstance(myProject);
    final boolean savedOption = readonlyStatusHandler.getState().SHOW_DIALOG;
    readonlyStatusHandler.getState().SHOW_DIALOG = false;
    try {
      readonlyStatusHandler.ensureFilesWritable(collectFiles(paths));
    }
    finally {
      readonlyStatusHandler.getState().SHOW_DIALOG = savedOption;
    }
  }

  @Override
  public List<CommitExecutor> getRegisteredExecutors() {
    return Collections.unmodifiableList(myExecutors);
  }

  private static class MyDirtyFilesScheduler {
    private static final int ourPiecesLimit = 100;
    private final List<VirtualFile> myFiles = new ArrayList<>();
    private final List<VirtualFile> myDirs = new ArrayList<>();
    private boolean myEveryThing;
    private int myCnt;
    private final Project myProject;

    private MyDirtyFilesScheduler(final Project project) {
      myProject = project;
      myCnt = 0;
      myEveryThing = false;
    }

    public void accept(final Collection<VirtualFile> coll) {
      for (VirtualFile vf : coll) {
        if (myCnt > ourPiecesLimit) {
          myEveryThing = true;
          break;
        }
        if (vf.isDirectory()) {
          myDirs.add(vf);
        }
        else {
          myFiles.add(vf);
        }
        ++myCnt;
      }
    }

    public void arise() {
      final VcsDirtyScopeManager vcsDirtyScopeManager = VcsDirtyScopeManager.getInstance(myProject);
      if (myEveryThing) {
        vcsDirtyScopeManager.markEverythingDirty();
      }
      else {
        vcsDirtyScopeManager.filesDirty(myFiles, myDirs);
      }
    }
  }

  /**
   * @return true if {@link #updateImmediately()} can be skipped.
   */
  private boolean hasNothingToUpdate() {
    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
    if (!vcsManager.hasActiveVcss()) return true;

    VcsDirtyScopeManagerImpl dirtyScopeManager = VcsDirtyScopeManagerImpl.getInstanceImpl(myProject);
    return !dirtyScopeManager.hasDirtyScopes();
  }

  @Override
  public boolean isIgnoredFile(@Nonnull VirtualFile file) {
    return isIgnoredFile(VcsUtil.getFilePath(file));
  }

  @Override
  public boolean isIgnoredFile(@Nonnull FilePath file) {
    VcsRoot vcsRoot = ProjectLevelVcsManager.getInstance(myProject).getVcsRootObjectFor(file);
    if (vcsRoot == null) return false;

    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getIgnoredFileHolder().containsFile(file, vcsRoot);
      }
    });
  }

  @Override
  @Nullable
  public String getSwitchedBranch(final VirtualFile file) {
    return ReadAction.compute(() -> {
      synchronized (myDataLock) {
        return myComposite.getSwitchedFileHolder().getBranchForFile(file);
      }
    });
  }

  @Override
  public String getDefaultListName() {
    synchronized (myDataLock) {
      return myWorker.getDefaultList().getName();
    }
  }

  private static VirtualFile[] collectFiles(final List<FilePath> paths) {
    final ArrayList<VirtualFile> result = new ArrayList<>();
    for (FilePath path : paths) {
      if (path.getVirtualFile() != null) {
        result.add(path.getVirtualFile());
      }
    }

    return VfsUtilCore.toVirtualFileArray(result);
  }

  @Override
  public boolean setReadOnly(final String name, final boolean value) {
    return ApplicationManager.getApplication().runReadAction((Supplier<Boolean>)() -> {
      synchronized (myDataLock) {
        final boolean result = myModifier.setReadOnly(name, value);
        myChangesViewManager.scheduleRefresh();
        return result;
      }
    });
  }

  @Override
  public boolean editName(@Nonnull final String fromName, @Nonnull final String toName) {
    return ApplicationManager.getApplication().runReadAction((Supplier<Boolean>)() -> {
      synchronized (myDataLock) {
        final boolean result = myModifier.editName(fromName, toName);
        myChangesViewManager.scheduleRefresh();
        return result;
      }
    });
  }

  @Override
  public String editComment(@Nonnull final String fromName, final String newComment) {
    return ApplicationManager.getApplication().runReadAction((Supplier<String>)() -> {
      synchronized (myDataLock) {
        final String oldComment = myModifier.editComment(fromName, newComment);
        myChangesViewManager.scheduleRefresh();
        return oldComment;
      }
    });
  }

  public void executeOnUpdaterThread(Runnable r) {
    myScheduler.submit(r);
  }

  @Override
  public int getChangeListsNumber() {
    synchronized (myDataLock) {
      return myWorker.getChangeListsNumber();
    }
  }

  // only a light attempt to show that some dirty scope request is asynchronously coming
  // for users to see changes are not valid
  // (commit -> asynch synch VFS -> asynch vcs dirty scope)
  public void showLocalChangesInvalidated() {
    synchronized (myDataLock) {
      myShowLocalChangesInvalidated = true;
    }
  }

  public ChangelistConflictTracker getConflictTracker() {
    return myConflictTracker;
  }

  private static final class MyChangesDeltaForwarder implements ChangeListDeltaListener {
    private final RemoteRevisionsCache myRevisionsCache;
    private final ProjectLevelVcsManager myVcsManager;
    private final Project myProject;
    private final ChangeListScheduler myScheduler;

    MyChangesDeltaForwarder(final Project project, @Nonnull ChangeListScheduler scheduler) {
      myProject = project;
      myScheduler = scheduler;
      myRevisionsCache = RemoteRevisionsCache.getInstance(project);
      myVcsManager = ProjectLevelVcsManager.getInstance(project);
    }

    @Override
    public void modified(@Nonnull BaseRevision was, @Nonnull BaseRevision become) {
      doModify(was, become);
    }

    @Override
    public void added(@Nonnull BaseRevision baseRevision) {
      doModify(baseRevision, baseRevision);
    }

    @Override
    public void removed(@Nonnull BaseRevision baseRevision) {
      myScheduler.submit(() -> {
        AbstractVcs vcs = getVcs(baseRevision);
        if (vcs != null) {
          myRevisionsCache.changeRemoved(baseRevision.getPath(), vcs);
        }
        BackgroundTaskUtil.syncPublisher(myProject, VcsAnnotationRefresher.class).dirty(baseRevision.getPath());
      });
    }

    private void doModify(BaseRevision was, BaseRevision become) {
      myScheduler.submit(() -> {
        final AbstractVcs vcs = getVcs(was);
        if (vcs != null) {
          myRevisionsCache.changeUpdated(was.getPath(), vcs);
        }
        BackgroundTaskUtil.syncPublisher(myProject, VcsAnnotationRefresher.class).dirty(become);
      });
    }

    @Nullable
    private AbstractVcs getVcs(@Nonnull BaseRevision baseRevision) {
      AbstractVcs vcs = baseRevision.getVcs();
      if (vcs != null) return vcs;
      return myVcsManager.getVcsFor(baseRevision.getFilePath());
    }
  }

  @Override
  public boolean isFreezedWithNotification(String modalTitle) {
    final String freezeReason = isFreezed();
    if (freezeReason != null) {
      if (modalTitle != null) {
        Messages.showErrorDialog(myProject, freezeReason, modalTitle);
      }
      else {
        VcsBalloonProblemNotifier.showOverChangesView(myProject, freezeReason, NotificationType.WARNING);
      }
    }
    return freezeReason != null;
  }
}
