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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.Semaphore;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.EditorNotifications;
import consulo.ide.impl.idea.openapi.vcs.VcsConnectionProblem;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.ChangeListRemoveConfirmation;
import consulo.ide.impl.idea.openapi.vcs.changes.conflicts.ChangelistConflictTracker;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.CommitHelper;
import consulo.ide.impl.idea.openapi.vcs.impl.AbstractVcsHelperImpl;
import consulo.ide.impl.idea.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.readOnlyHandler.ReadonlyStatusHandlerImpl;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.FunctionUtil;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.DirectoryIndexExcludePolicy;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.project.ui.notification.NotificationType;
import consulo.proxy.EventDispatcher;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.*;
import consulo.util.lang.ref.SimpleReference;
import consulo.util.xml.serializer.JDOMExternalizerUtil;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.impl.internal.change.*;
import consulo.versionControlSystem.internal.ChangeListManagerEx;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.versionControlSystem.util.VcsRootIterator;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

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

    private ChangeListWorker myWorker;
    private VcsException myUpdateException;
    private Supplier<JComponent> myAdditionalInfo;

    private final EventDispatcher<ChangeListListener> myListeners = EventDispatcher.create(ChangeListListener.class);

    private final Object myDataLock = new Object();

    private final Disposable myUpdateDisposable = Disposable.newDisposable();

    private final List<CommitExecutor> myExecutors = new ArrayList<>();

    private final IgnoredFilesComponent myIgnoredFilesComponent;
    private boolean myExcludedConvertedToIgnored;
    @Nonnull
    private volatile ProgressIndicator myUpdateChangesProgressIndicator = createProgressIndicator();

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

    private final ChangeListScheduler myScheduler = new ChangeListScheduler(); // update thread

    private boolean myModalNotificationsBlocked;
    @Nonnull
    private final Collection<LocalChangeList> myListsToBeDeleted = new HashSet<>();

    public static ChangeListManagerImpl getInstanceImpl(Project project) {
        return (ChangeListManagerImpl) project.getInstance(ChangeListManager.class);
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
        myAdditionalInfo = null;
        myChangesViewManager = myProject.isDefault() ? new DummyChangesView(myProject) : ChangesViewManager.getInstance(myProject);
        myFileStatusManager = FileStatusManager.getInstance(myProject);
        myComposite = new FileHolderComposite(project);
        myIgnoredFilesComponent = new IgnoredFilesComponent(myProject, true);
        myUpdater = new UpdateRequestsQueue(myProject, myScheduler, this::updateImmediately);

        myWorker = new ChangeListWorker(myProject, new MyChangesDeltaForwarder(myProject, myScheduler));
        myDelayedNotificator = new DelayedNotificator(myListeners, myScheduler);
        myModifier = new Modifier(myWorker, myDelayedNotificator);

        myConflictTracker = new ChangelistConflictTracker(project, this, myFileStatusManager, editorNotifications, applicationConcurrency);

        myListeners.addListener(new ChangeListListener() {
            @Override
            public void defaultListChanged(ChangeList oldDefaultList, ChangeList newDefaultList) {
                LocalChangeList oldList = (LocalChangeList) oldDefaultList;
                if (oldDefaultList == null || oldList.hasDefaultName() || oldDefaultList.equals(newDefaultList)) {
                    return;
                }

                scheduleAutomaticChangeListDeletionIfEmpty(oldList, config);
            }
        });

        myListeners.addListener(new ChangeListListener() {
            @Nonnull
            private ChangeListListener topic() {
                return myProject.getMessageBus().syncPublisher(ChangeListListener.class);
            }

            @Override
            public void changeListAdded(ChangeList list) {
                topic().changeListAdded(list);
            }

            @Override
            public void changesRemoved(Collection<Change> changes, ChangeList fromList) {
                topic().changesRemoved(changes, fromList);
            }

            @Override
            public void changesAdded(Collection<Change> changes, ChangeList toList) {
                topic().changesAdded(changes, toList);
            }

            @Override
            public void changeListRemoved(ChangeList list) {
                topic().changeListRemoved(list);
            }

            @Override
            public void changeListChanged(ChangeList list) {
                topic().changeListChanged(list);
            }

            @Override
            public void changeListRenamed(ChangeList list, String oldName) {
                topic().changeListRenamed(list, oldName);
            }

            @Override
            public void changeListCommentChanged(ChangeList list, String oldComment) {
                topic().changeListCommentChanged(list, oldComment);
            }

            @Override
            public void changesMoved(Collection<Change> changes, ChangeList fromList, ChangeList toList) {
                topic().changesMoved(changes, fromList, toList);
            }

            @Override
            public void defaultListChanged(ChangeList oldDefaultList, ChangeList newDefaultList) {
                topic().defaultListChanged(oldDefaultList, newDefaultList);
            }

            @Override
            public void unchangedFileStatusChanged() {
                topic().unchangedFileStatusChanged();
            }

            @Override
            public void changeListUpdateDone() {
                topic().changeListUpdateDone();
            }
        });

        Disposer.register(this, myUpdateDisposable); // register defensively, in case "projectClosing" won't be called

        MessageBusConnection connection = project.getApplication().getMessageBus().connect(this);
        connection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
            @Override
            public void projectClosing(@Nonnull Project project) {
                if (project == ChangeListManagerImpl.this.myProject) {
                    // Can't use Project disposable - it will be called after pending tasks are finished
                    Disposer.dispose(myUpdateDisposable);
                }
            }
        });
    }

    private void scheduleAutomaticChangeListDeletionIfEmpty(LocalChangeList oldList, VcsConfiguration config) {
        if (oldList.isReadOnly() || !oldList.getChanges().isEmpty()) {
            return;
        }

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
            @RequiredUIAccess
            @Override
            public boolean askIfShouldRemoveChangeLists(@Nonnull List<? extends LocalChangeList> toAsk) {
                return myConfig.REMOVE_EMPTY_INACTIVE_CHANGELISTS != VcsShowConfirmationOption.Value.SHOW_CONFIRMATION
                    || showRemoveEmptyChangeListsProposal(myConfig, toAsk);
            }
        });
    }

    /**
     * Shows the proposal to delete one or more changelists that were default and became empty.
     *
     * @return true if the changelists have to be deleted, false if not.
     */
    @RequiredUIAccess
    private boolean showRemoveEmptyChangeListsProposal(
        @Nonnull VcsConfiguration config,
        @Nonnull Collection<? extends LocalChangeList> lists
    ) {
        if (lists.isEmpty()) {
            return false;
        }

        String question;
        if (lists.size() == 1) {
            question = String.format(
                "<html>The empty changelist '%s' is no longer active.<br>Do you want to remove it?</html>",
                StringUtil.first(lists.iterator().next().getName(), 30, true)
            );
        }
        else {
            question = String.format(
                "<html>Empty changelists<br/>%s are no longer active.<br>Do you want to remove them?</html>",
                StringUtil.join(
                    lists,
                    list -> StringUtil.first(list.getName(), 30, true),
                    "<br/>"
                )
            );
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

    @Override
    @RequiredUIAccess
    public void blockModalNotifications() {
        myModalNotificationsBlocked = true;
    }

    @Override
    @RequiredUIAccess
    public void unblockModalNotifications() {
        myModalNotificationsBlocked = false;
        deleteEmptyChangeLists(myListsToBeDeleted);
        myListsToBeDeleted.clear();
    }

    public void projectOpened() {
        initializeForNewProject();

        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
        ((ProjectLevelVcsManagerImpl) vcsManager).addInitializationRequest(VcsInitObject.CHANGE_LIST_MANAGER, () -> {
            myUpdater.initialized();
            broadcastStateAfterLoad();
            myProject.getMessageBus().connect().subscribe(VcsMappingListener.class, myVcsListener);
        });

        myConflictTracker.startTracking();
    }

    private void broadcastStateAfterLoad() {
        List<LocalChangeList> listCopy;
        synchronized (myDataLock) {
            listCopy = getChangeListsCopy();
        }
        if (!myProject.isDisposed()) {
            myProject.getMessageBus().syncPublisher(LocalChangeListsLoadedListener.class).processLoadedLists(listCopy);
        }
    }

    private void initializeForNewProject() {
        myProject.getApplication().runReadAction(() -> {
            synchronized (myDataLock) {
                if (myWorker.isEmpty()) {
                    setDefaultChangeList(myWorker.addChangeList(LocalChangeList.DEFAULT_NAME, null, null));
                }
                if (!Registry.is("ide.hide.excluded.files") && !myExcludedConvertedToIgnored) {
                    convertExcludedToIgnored();
                    myExcludedConvertedToIgnored = true;
                }
            }
        });
    }

    @RequiredReadAction
    void convertExcludedToIgnored() {
        for (DirectoryIndexExcludePolicy policy : DirectoryIndexExcludePolicy.EP_NAME.getExtensions(myProject)) {
            for (VirtualFile file : policy.getExcludeRootsForProject()) {
                addDirectoryToIgnoreImplicitly(file.getPath());
            }
        }

        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(myProject);
        VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
        for (Module module : ModuleManager.getInstance(myProject).getModules()) {
            for (String url : ModuleRootManager.getInstance(module).getExcludeRootUrls()) {
                VirtualFile file = virtualFileManager.findFileByUrl(url);
                if (file != null && !fileIndex.isExcluded(file)) {
                    //root is included into some inner module so it shouldn't be ignored
                    continue;
                }
                addDirectoryToIgnoreImplicitly(VfsUtilCore.urlToPath(url));
            }
        }
    }

    @Override
    public void dispose() {
        synchronized (myDataLock) {
            myUpdateChangesProgressIndicator.cancel();
        }

        myUpdater.stop();
        myConflictTracker.stopTracking();
    }

    /**
     * update itself might produce actions done on AWT thread (invoked-after),
     * so waiting for its completion on AWT thread is not good runnable is invoked on AWT thread
     */
    @Override
    public void invokeAfterUpdate(
        Runnable afterUpdate,
        InvokeAfterUpdateMode mode,
        @Nullable String title,
        @Nullable ModalityState state
    ) {
        myUpdater.invokeAfterUpdate(afterUpdate, mode, title, null, state);
    }

    @Override
    public void invokeAfterUpdate(
        Runnable afterUpdate,
        InvokeAfterUpdateMode mode,
        String title,
        Consumer<VcsDirtyScopeManager> dirtyScopeManagerFiller,
        ModalityState state
    ) {
        myUpdater.invokeAfterUpdate(afterUpdate, mode, title, dirtyScopeManagerFiller, state);
    }

    @Override
    public void freeze(@Nonnull String reason) {
        myUpdater.setIgnoreBackgroundOperation(true);
        Semaphore sem = new Semaphore();
        sem.down();

        invokeAfterUpdate(
            () -> {
                myUpdater.setIgnoreBackgroundOperation(false);
                myUpdater.pause();
                myFreezeName.set(reason);
                sem.up();
            },
            InvokeAfterUpdateMode.SILENT_CALLBACK_POOLED,
            "",
            Application.get().getDefaultModalityState()
        );

        boolean free = false;
        while (!free) {
            ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
            if (pi != null) {
                pi.checkCanceled();
            }
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
    public void scheduleUpdateImpl() {
        myUpdater.schedule();
    }

    @Override
    public void scheduleUpdate() {
        myUpdater.schedule();
    }

    @Override
    public void scheduleUpdate(boolean updateUnversionedFiles) {
        myUpdater.schedule();
    }

    private void filterOutIgnoredFiles(List<VcsDirtyScope> scopes) {
        Set<VirtualFile> refreshFiles = new HashSet<>();
        try {
            synchronized (myDataLock) {
                IgnoredFilesCompositeHolder fileHolder = myComposite.getIgnoredFileHolder();

                for (Iterator<VcsDirtyScope> iterator = scopes.iterator(); iterator.hasNext(); ) {
                    VcsModifiableDirtyScope scope = (VcsModifiableDirtyScope) iterator.next();
                    VcsDirtyScopeModifier modifier = scope.getModifier();
                    if (modifier != null) {
                        fileHolder.notifyVcsStarted(scope.getVcs());
                        Iterator<FilePath> filesIterator = modifier.getDirtyFilesIterator();
                        while (filesIterator.hasNext()) {
                            FilePath dirtyFile = filesIterator.next();
                            if (dirtyFile.getVirtualFile() != null && isIgnoredFile(dirtyFile)) {
                                filesIterator.remove();
                                fileHolder.addFile(dirtyFile);
                                refreshFiles.add(dirtyFile.getVirtualFile());
                            }
                        }
                        Collection<VirtualFile> roots = modifier.getAffectedVcsRoots();
                        for (VirtualFile root : roots) {
                            Iterator<FilePath> dirIterator = modifier.getDirtyDirectoriesIterator(root);
                            while (dirIterator.hasNext()) {
                                FilePath dir = dirIterator.next();
                                if (dir.getVirtualFile() != null && isIgnoredFile(dir)) {
                                    dirIterator.remove();
                                    fileHolder.addFile(dir);
                                    refreshFiles.add(dir.getVirtualFile());
                                }
                            }
                        }
                        modifier.recheckDirtyKeys();
                        if (scope.isEmpty()) {
                            iterator.remove();
                        }
                    }
                }
            }
        }
        catch (Exception | AssertionError ex) {
            LOG.error(ex);
        }
        for (VirtualFile file : refreshFiles) {
            myFileStatusManager.fileStatusChanged(file);
        }
    }

    private boolean updateImmediately() {
        return BackgroundTaskUtil.runUnderDisposeAwareIndicator(myUpdateDisposable, this::updateImmediatelyImpl);
    }

    /**
     * @return false if update was re-scheduled due to new 'markEverythingDirty' event, true otherwise.
     */
    private boolean updateImmediatelyImpl() {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
        if (!vcsManager.hasActiveVcss()) {
            return true;
        }

        VcsDirtyScopeManagerImpl dirtyScopeManager = (VcsDirtyScopeManagerImpl) VcsDirtyScopeManager.getInstance(myProject);

        VcsInvalidated invalidated = dirtyScopeManager.retrieveScopes();
        if (checkScopeIsEmpty(invalidated)) {
            LOG.debug("[update] - dirty scope is empty");
            dirtyScopeManager.changesProcessed();
            return true;
        }

        boolean wasEverythingDirty = invalidated.isEverythingDirty();
        List<VcsModifiableDirtyScope> scopes = invalidated.getScopes();

        try {
            checkIfDisposed();

            // copy existing data to objects that would be updated.
            // mark for "modifier" that update started (it would create duplicates of modification commands done by user during update;
            // after update of copies of objects is complete, it would apply the same modifications to copies.)
            DataHolder dataHolder;
            ProgressIndicator indicator = createProgressIndicator();
            synchronized (myDataLock) {
                dataHolder = new DataHolder((FileHolderComposite) myComposite.copy(), myWorker.copy(), wasEverythingDirty);
                myModifier.enterUpdate();
                if (wasEverythingDirty) {
                    myUpdateException = null;
                    myAdditionalInfo = null;
                }
                myUpdateChangesProgressIndicator = indicator;
            }
            if (LOG.isDebugEnabled()) {
                String scopeInString = StringUtil.join(scopes, Object::toString, "->\n");
                LOG.debug("refresh procedure started, everything: " + wasEverythingDirty + " dirty scope: " + scopeInString + "\ncurrent changes: " + myWorker);
            }
            dataHolder.notifyStart();
            myChangesViewManager.scheduleRefresh();

            ProgressManager.getInstance().runProcess(() -> iterateScopes(dataHolder, scopes, wasEverythingDirty), indicator);

            boolean takeChanges = myUpdateException == null;
            if (takeChanges) {
                // update IDEA-level ignored files
                updateIgnoredFiles(dataHolder.getComposite());
            }

            // for the case of project being closed we need a read action here -> to be more consistent
            myProject.getApplication().runReadAction(() -> {
                if (myProject.isDisposed()) {
                    return;
                }
                synchronized (myDataLock) {
                    // do same modifications to change lists as was done during update + do delayed notifications
                    dataHolder.notifyEnd();
                    // should be applied for notifications to be delivered (they were delayed) - anyway whether we take changes or not
                    myModifier.finishUpdate(dataHolder.getChangeListWorker());
                    // update member from copy
                    if (takeChanges) {
                        ChangeListWorker oldWorker = myWorker;
                        myWorker = dataHolder.getChangeListWorker();
                        myWorker.onAfterWorkerSwitch(oldWorker);
                        myModifier.setWorker(myWorker);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("refresh procedure finished, unversioned size: " + dataHolder.getComposite()
                                .getVFHolder(FileHolder.HolderType.UNVERSIONED)
                                .getSize() + "\nchanges: " + myWorker);
                        }
                        boolean statusChanged = !myComposite.equals(dataHolder.getComposite());
                        myComposite = dataHolder.getComposite();
                        if (statusChanged) {
                            myDelayedNotificator.unchangedFileStatusChanged();
                        }
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

            myChangesViewManager.scheduleRefresh();
        }
        catch (ProcessCanceledException e) {
            // OK, we're finishing all the stuff now.
        }
        catch (Exception | AssertionError ex) {
            LOG.error(ex);
        }
        finally {
            dirtyScopeManager.changesProcessed();

            synchronized (myDataLock) {
                myDelayedNotificator.changeListUpdateDone();
                myChangesViewManager.scheduleRefresh();
            }
        }

        return true;
    }

    private static boolean checkScopeIsEmpty(VcsInvalidated invalidated) {
        return invalidated == null || !invalidated.isEverythingDirty() && invalidated.isEmpty();
    }

    private void iterateScopes(DataHolder dataHolder, List<? extends VcsModifiableDirtyScope> scopes, boolean wasEverythingDirty) {
        ChangeListManagerGate gate = dataHolder.getChangeListWorker().createSelfGate();
        // do actual requests about file statuses
        Supplier<Boolean> disposedGetter = () -> myProject.isDisposed() || myUpdater.isStopped();
        UpdatingChangeListBuilder builder =
            new UpdatingChangeListBuilder(dataHolder.getChangeListWorker(), dataHolder.getComposite(), disposedGetter, this, gate);

        for (VcsModifiableDirtyScope scope : scopes) {
            myUpdateChangesProgressIndicator.checkCanceled();

            AbstractVcs vcs = scope.getVcs();
            if (vcs == null) {
                continue;
            }
            scope.setWasEverythingDirty(wasEverythingDirty);

            myChangesViewManager.setBusy(true);
            dataHolder.notifyStartProcessingChanges(scope);

            actualUpdate(builder, scope, vcs, dataHolder, gate);

            if (myUpdateException != null) {
                break;
            }
        }
        synchronized (myDataLock) {
            if (myAdditionalInfo == null) {
                myAdditionalInfo = builder.getAdditionalInfo();
            }
        }
    }

    @Nonnull
    private static ProgressIndicator createProgressIndicator() {
        return new EmptyProgressIndicator();
    }

    private class DataHolder {
        private final boolean myWasEverythingDirty;
        private final FileHolderComposite myComposite;
        private final ChangeListWorker myChangeListWorker;

        private DataHolder(FileHolderComposite composite, ChangeListWorker changeListWorker, boolean wasEverythingDirty) {
            myComposite = composite;
            myChangeListWorker = changeListWorker;
            myWasEverythingDirty = wasEverythingDirty;
        }

        private void notifyStart() {
            if (myWasEverythingDirty) {
                myComposite.cleanAll();
                myChangeListWorker.notifyStartProcessingChanges(null);
            }
        }

        private void notifyStartProcessingChanges(@Nonnull VcsModifiableDirtyScope scope) {
            if (!myWasEverythingDirty) {
                myComposite.cleanAndAdjustScope(scope);
                myChangeListWorker.notifyStartProcessingChanges(scope);
            }

            myComposite.notifyVcsStarted(scope.getVcs());
            myChangeListWorker.notifyVcsStarted(scope.getVcs());
        }

        private void notifyDoneProcessingChanges() {
            if (!myWasEverythingDirty) {
                myChangeListWorker.notifyDoneProcessingChanges(myDelayedNotificator);
            }
        }

        void notifyEnd() {
            if (myWasEverythingDirty) {
                myChangeListWorker.notifyDoneProcessingChanges(myDelayedNotificator);
            }
        }

        public FileHolderComposite getComposite() {
            return myComposite;
        }

        ChangeListWorker getChangeListWorker() {
            return myChangeListWorker;
        }
    }

    private void actualUpdate(
        @Nonnull UpdatingChangeListBuilder builder,
        @Nonnull VcsDirtyScope scope,
        @Nonnull AbstractVcs vcs,
        @Nonnull DataHolder dataHolder,
        @Nonnull ChangeListManagerGate gate
    ) {
        try {
            ChangeProvider changeProvider = vcs.getChangeProvider();
            if (changeProvider != null) {
                FoldersCutDownWorker foldersCutDownWorker = new FoldersCutDownWorker();
                try {
                    builder.setCurrent(scope, foldersCutDownWorker);
                    changeProvider.getChanges(scope, builder, myUpdateChangesProgressIndicator, gate);
                }
                catch (VcsException e) {
                    handleUpdateException(e);
                }
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable t) {
            LOG.debug(t);
            ExceptionUtil.rethrowAllAsUnchecked(t);
        }
        finally {
            if (!myUpdater.isStopped()) {
                dataHolder.notifyDoneProcessingChanges();
            }
        }
    }

    private void handleUpdateException(VcsException e) {
        LOG.info(e);

        if (e instanceof VcsConnectionProblem vcsConnectionProblem) {
            myProject.getApplication().invokeLater(() -> vcsConnectionProblem.attemptQuickFix(false));
        }

        if (myUpdateException == null) {
            if (myProject.getApplication().isUnitTestMode()) {
                AbstractVcsHelper helper = AbstractVcsHelper.getInstance(myProject);
                if (helper instanceof AbstractVcsHelperImpl helperImpl && helperImpl.handleCustom(e)) {
                    return;
                }
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
            myUpdateException = e;
        }
    }

    private void checkIfDisposed() {
        if (myProject.getDisposeState().get() == ThreeState.UNSURE) {
            throw new ProcessCanceledException();
        }

        if (myUpdater.isStopped()) {
            throw new ProcessCanceledException();
        }
    }

    public static boolean isUnder(Change change, VcsDirtyScope scope) {
        ContentRevision before = change.getBeforeRevision();
        ContentRevision after = change.getAfterRevision();
        return before != null && scope.belongsTo(before.getFile()) || after != null && scope.belongsTo(after.getFile());
    }

    @Override
    public List<LocalChangeList> getChangeListsCopy() {
        synchronized (myDataLock) {
            return myWorker.getListsCopy();
        }
    }

    /**
     * @deprecated this method made equivalent to {@link #getChangeListsCopy()} so to don't be confused by method name,
     * better use {@link #getChangeListsCopy()}
     */
    @Override
    @Nonnull
    public List<LocalChangeList> getChangeLists() {
        synchronized (myDataLock) {
            return getChangeListsCopy();
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
            if (change == null) {
                return Collections.emptyList();
            }
            return getChangeLists(change);
        }
    }

    @Override
    public List<File> getAffectedPaths() {
        synchronized (myDataLock) {
            return myWorker.getAffectedPaths();
        }
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
        synchronized (myDataLock) {
            return myWorker.getAffectedFiles();
        }
    }

    @Override
    @Nonnull
    public Collection<Change> getAllChanges() {
        synchronized (myDataLock) {
            return myWorker.getAllChanges();
        }
    }

    @Nonnull
    public List<VirtualFile> getUnversionedFiles() {
        synchronized (myDataLock) {
            return myComposite.getVFHolder(FileHolder.HolderType.UNVERSIONED).getFiles();
        }
    }

    @Nonnull
    public Couple<Integer> getUnversionedFilesSize() {
        synchronized (myDataLock) {
            VirtualFileHolder holder = myComposite.getVFHolder(FileHolder.HolderType.UNVERSIONED);
            return Couple.of(holder.getSize(), holder.getNumDirs());
        }
    }

    @Override
    public List<VirtualFile> getModifiedWithoutEditing() {
        synchronized (myDataLock) {
            return myComposite.getVFHolder(FileHolder.HolderType.MODIFIED_WITHOUT_EDITING).getFiles();
        }
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
        synchronized (myDataLock) {
            return myComposite.getVFHolder(FileHolder.HolderType.LOCKED).getFiles();
        }
    }

    Map<VirtualFile, LogicalLock> getLogicallyLockedFolders() {
        synchronized (myDataLock) {
            return new HashMap<>(((LogicallyLockedHolder) myComposite.get(FileHolder.HolderType.LOGICALLY_LOCKED)).getMap());
        }
    }

    public boolean isLogicallyLocked(VirtualFile file) {
        synchronized (myDataLock) {
            return ((LogicallyLockedHolder) myComposite.get(FileHolder.HolderType.LOGICALLY_LOCKED)).containsKey(file);
        }
    }

    @Override
    public boolean isContainedInLocallyDeleted(FilePath filePath) {
        synchronized (myDataLock) {
            return myWorker.isContainedInLocallyDeleted(filePath);
        }
    }

    public List<LocallyDeletedChange> getDeletedFiles() {
        synchronized (myDataLock) {
            return myWorker.getLocallyDeleted().getFiles();
        }
    }

    MultiMap<String, VirtualFile> getSwitchedFilesMap() {
        synchronized (myDataLock) {
            return myWorker.getSwitchedHolder().getBranchToFileMap();
        }
    }

    @Nullable
    Map<VirtualFile, String> getSwitchedRoots() {
        synchronized (myDataLock) {
            return ((SwitchedFileHolder) myComposite.get(FileHolder.HolderType.ROOT_SWITCH)).getFilesMapCopy();
        }
    }

    public VcsException getUpdateException() {
        synchronized (myDataLock) {
            return myUpdateException;
        }
    }

    Supplier<JComponent> getAdditionalUpdateInfo() {
        synchronized (myDataLock) {
            return myAdditionalInfo;
        }
    }

    @Override
    public boolean isFileAffected(VirtualFile file) {
        synchronized (myDataLock) {
            return myWorker.getStatus(file) != null;
        }
    }

    @Override
    @Nullable
    public LocalChangeList findChangeList(String name) {
        synchronized (myDataLock) {
            return myWorker.getCopyByName(name);
        }
    }

    @Override
    public LocalChangeList getChangeList(String id) {
        synchronized (myDataLock) {
            return myWorker.getChangeList(id);
        }
    }

    @Override
    public LocalChangeList addChangeList(@Nonnull String name, @Nullable String comment) {
        return addChangeList(name, comment, null);
    }

    @Nonnull
    @Override
    public LocalChangeList addChangeList(@Nonnull String name, @Nullable String comment, @Nullable Object data) {
        return myProject.getApplication().runReadAction((Supplier<LocalChangeList>) () -> {
            synchronized (myDataLock) {
                LocalChangeList changeList = myModifier.addChangeList(name, comment, data);
                myChangesViewManager.scheduleRefresh();
                return changeList;
            }
        });
    }


    @Override
    public void removeChangeList(String name) {
        myProject.getApplication().runReadAction(() -> {
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
    public Runnable prepareForChangeDeletion(Collection<Change> changes) {
        Map<String, LocalChangeList> lists = new HashMap<>();
        Map<String, List<Change>> map;
        synchronized (myDataLock) {
            map = myWorker.listsForChanges(changes, lists);
        }
        return () -> myProject.getApplication().runReadAction(() -> {
            synchronized (myDataLock) {
                for (Map.Entry<String, List<Change>> entry : map.entrySet()) {
                    List<Change> changes1 = entry.getValue();
                    for (Iterator<Change> iterator = changes1.iterator(); iterator.hasNext(); ) {
                        Change change = iterator.next();
                        if (getChangeList(change) != null) {
                            // was not actually rolled back
                            iterator.remove();
                        }
                    }
                    myDelayedNotificator.changesRemoved(changes1, lists.get(entry.getKey()));
                }
                for (String listName : map.keySet()) {
                    LocalChangeList byName = myWorker.getCopyByName(listName);
                    if (byName != null && !byName.isDefault()) {
                        scheduleAutomaticChangeListDeletionIfEmpty(byName, myConfig);
                    }
                }
            }
        });
    }

    @Override
    public void setDefaultChangeList(@Nonnull LocalChangeList list) {
        myProject.getApplication().runReadAction(() -> {
            synchronized (myDataLock) {
                myModifier.setDefault(list.getName());
            }
        });
        myChangesViewManager.scheduleRefresh();
    }

    @Override
    @Nullable
    public LocalChangeList getDefaultChangeList() {
        synchronized (myDataLock) {
            return myWorker.getDefaultListCopy();
        }
    }

    @Override
    public boolean isDefaultChangeList(ChangeList list) {
        return list instanceof LocalChangeList localChangeList && myWorker.isDefaultList(localChangeList);
    }

    @Override
    @Nonnull
    public Collection<LocalChangeList> getInvolvedListsFilterChanges(
        @Nonnull Collection<Change> changes,
        @Nonnull List<Change> validChanges
    ) {
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
    public String getChangeListNameIfOnlyOne(Change[] changes) {
        synchronized (myDataLock) {
            return myWorker.listNameIfOnlyOne(changes);
        }
    }

    /**
     * @deprecated better use normal comparison, with equals
     */
    @Override
    @Nullable
    public LocalChangeList getIdentityChangeList(@Nonnull Change change) {
        synchronized (myDataLock) {
            List<LocalChangeList> lists = myWorker.getListsCopy();
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
    public Change getChange(FilePath file) {
        synchronized (myDataLock) {
            return myWorker.getChangeForPath(file);
        }
    }

    @Override
    public boolean isUnversioned(VirtualFile file) {
        synchronized (myDataLock) {
            return myComposite.getVFHolder(FileHolder.HolderType.UNVERSIONED).containsFile(file);
        }
    }

    @Override
    @Nonnull
    public FileStatus getStatus(VirtualFile file) {
        synchronized (myDataLock) {
            if (myComposite.getVFHolder(FileHolder.HolderType.UNVERSIONED).containsFile(file)) {
                return FileStatus.UNKNOWN;
            }
            if (myComposite.getVFHolder(FileHolder.HolderType.MODIFIED_WITHOUT_EDITING).containsFile(file)) {
                return FileStatus.HIJACKED;
            }
            if (myComposite.getIgnoredFileHolder().containsFile(file)) {
                return FileStatus.IGNORED;
            }

            boolean switched = myWorker.isSwitched(file);
            FileStatus status = myWorker.getStatus(file);
            if (status != null) {
                return FileStatus.NOT_CHANGED.equals(status) && switched ? FileStatus.SWITCHED : status;
            }
            if (switched) {
                return FileStatus.SWITCHED;
            }
            return FileStatus.NOT_CHANGED;
        }
    }

    @Override
    @Nonnull
    public Collection<Change> getChangesIn(VirtualFile dir) {
        return getChangesIn(VcsUtil.getFilePath(dir));
    }

    @Nonnull
    @Override
    public ThreeState haveChangesUnder(@Nonnull VirtualFile vf) {
        if (!vf.isValid() || !vf.isDirectory()) {
            return ThreeState.NO;
        }
        synchronized (myDataLock) {
            return myWorker.haveChangesUnder(vf);
        }
    }

    @Override
    @Nonnull
    public Collection<Change> getChangesIn(FilePath dirPath) {
        synchronized (myDataLock) {
            return myWorker.getChangesIn(dirPath);
        }
    }

    @Override
    @Nullable
    public AbstractVcs getVcsFor(@Nonnull Change change) {
        VcsKey key;
        synchronized (myDataLock) {
            key = myWorker.getVcsFor(change);
        }
        return key != null ? ProjectLevelVcsManager.getInstance(myProject).findVcsByName(key.getName()) : null;
    }

    @Override
    public void moveChangesTo(LocalChangeList list, Change... changes) {
        myProject.getApplication().runReadAction(() -> {
            synchronized (myDataLock) {
                myModifier.moveChangesTo(list.getName(), changes);
            }
        });
        myChangesViewManager.scheduleRefresh();
    }

    @Override
    @RequiredUIAccess
    public void addUnversionedFiles(LocalChangeList list, @Nonnull List<VirtualFile> files) {
        addUnversionedFiles(list, files, getDefaultUnversionedFileCondition(), null);
    }

    // TODO this is for quick-fix for GitAdd problem. To be removed after proper fix
    // (which should introduce something like VcsAddRemoveEnvironment)
    @Deprecated
    @Nonnull
    @RequiredUIAccess
    public List<VcsException> addUnversionedFiles(
        LocalChangeList list,
        @Nonnull List<VirtualFile> files,
        @Nonnull Predicate<FileStatus> statusChecker,
        @Nullable Consumer<List<Change>> changesConsumer
    ) {
        List<VcsException> exceptions = new ArrayList<>();
        Set<VirtualFile> allProcessedFiles = new HashSet<>();
        ChangesUtil.processVirtualFilesByVcs(
            myProject,
            files,
            (vcs, items) -> {
                CheckinEnvironment environment = vcs.getCheckinEnvironment();
                if (environment != null) {
                    Set<VirtualFile> descendants = getUnversionedDescendantsRecursively(items, statusChecker);
                    Set<VirtualFile> parents = vcs.areDirectoriesVersionedItems()
                        ? getUnversionedParents(items, statusChecker)
                        : Collections.<VirtualFile>emptySet();

                    // it is assumed that not-added parents of files passed to scheduleUnversionedFilesForAddition() will also be added to vcs
                    // (inside the method) - so common add logic just needs to refresh statuses of parents
                    List<VcsException> result = new ArrayList<>();
                    ProgressManager.getInstance().run(new Task.Modal(myProject, "Adding files to VCS...", true) {
                        @Override
                        public void run(@Nonnull ProgressIndicator indicator) {
                            indicator.setIndeterminate(true);
                            List<VcsException> exs = environment.scheduleUnversionedFilesForAddition(new ArrayList<>(descendants));
                            if (exs != null) {
                                ContainerUtil.addAll(result, exs);
                            }
                        }
                    });

                    allProcessedFiles.addAll(descendants);
                    allProcessedFiles.addAll(parents);
                    exceptions.addAll(result);
                }
            }
        );

        if (!exceptions.isEmpty()) {
            StringBuilder message = new StringBuilder(VcsLocalize.errorAddingFilesPrompt().get());
            for (VcsException ex : exceptions) {
                message.append("\n").append(ex.getMessage());
            }
            Messages.showErrorDialog(myProject, message.toString(), VcsLocalize.errorAddingFilesTitle().get());
        }

        for (VirtualFile file : allProcessedFiles) {
            myFileStatusManager.fileStatusChanged(file);
        }
        VcsDirtyScopeManager.getInstance(myProject).filesDirty(allProcessedFiles, null);

        SimpleReference<List<Change>> foundChanges = SimpleReference.create();
        boolean moveRequired = !list.isDefault();
        boolean syncUpdateRequired = changesConsumer != null;

        if (moveRequired || syncUpdateRequired) {
            // find the changes for the added files and move them to the necessary changelist
            InvokeAfterUpdateMode updateMode = syncUpdateRequired
                ? InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE : InvokeAfterUpdateMode.BACKGROUND_NOT_CANCELLABLE;

            invokeAfterUpdate(
                () -> {
                    myProject.getApplication().runReadAction(() -> {
                        synchronized (myDataLock) {
                            List<Change> newChanges = findChanges(allProcessedFiles);
                            foundChanges.set(newChanges);

                            if (moveRequired && !newChanges.isEmpty()) {
                                moveChangesTo(list, newChanges.toArray(new Change[newChanges.size()]));
                            }
                        }
                    });

                    myChangesViewManager.scheduleRefresh();
                },
                updateMode,
                VcsLocalize.changeListsManagerAddUnversioned().get(),
                null
            );

            if (changesConsumer != null) {
                changesConsumer.accept(foundChanges.get());
            }
        }
        else {
            myChangesViewManager.scheduleRefresh();
        }

        return exceptions;
    }

    @Nonnull
    private List<Change> findChanges(@Nonnull Collection<VirtualFile> files) {
        List<Change> result = new ArrayList<>();

        for (Change change : getDefaultChangeList().getChanges()) {
            ContentRevision afterRevision = change.getAfterRevision();
            if (afterRevision != null) {
                VirtualFile file = afterRevision.getFile().getVirtualFile();
                if (files.contains(file)) {
                    result.add(change);
                }
            }
        }

        return result;
    }

    @Nonnull
    public static Predicate<FileStatus> getDefaultUnversionedFileCondition() {
        return status -> status == FileStatus.UNKNOWN;
    }

    @Nonnull
    private Set<VirtualFile> getUnversionedDescendantsRecursively(
        @Nonnull List<VirtualFile> items,
        @Nonnull Predicate<FileStatus> condition
    ) {
        Set<VirtualFile> result = new HashSet<>();
        Predicate<VirtualFile> addToResultProcessor = file -> {
            if (condition.test(getStatus(file))) {
                result.add(file);
            }
            return true;
        };

        for (VirtualFile item : items) {
            VcsRootIterator.iterateVfUnderVcsRoot(myProject, item, addToResultProcessor);
        }

        return result;
    }

    @Nonnull
    private Set<VirtualFile> getUnversionedParents(@Nonnull Collection<VirtualFile> items, @Nonnull Predicate<FileStatus> condition) {
        Set<VirtualFile> result = new HashSet<>();

        for (VirtualFile item : items) {
            VirtualFile parent = item.getParent();

            while (parent != null && condition.test(getStatus(parent))) {
                result.add(parent);
                parent = parent.getParent();
            }
        }

        return result;
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

    private boolean doCommit(LocalChangeList changeList, List<Change> changes, boolean synchronously) {
        FileDocumentManager.getInstance().saveAllDocuments();
        return new CommitHelper(
            myProject,
            changeList,
            changes,
            changeList.getName(),
            StringUtil.isEmpty(changeList.getComment()) ? changeList.getName() : changeList.getComment(),
            new ArrayList<>(),
            false,
            synchronously,
            FunctionUtil.nullConstant(),
            null
        ).doCommit();
    }

    @Override
    public void commitChangesSynchronously(LocalChangeList changeList, List<Change> changes) {
        doCommit(changeList, changes, true);
    }

    @Override
    public boolean commitChangesSynchronouslyWithResult(LocalChangeList changeList, List<Change> changes) {
        return doCommit(changeList, changes, true);
    }

    @Override
    public void loadState(Element element) {
        if (myProject.isDefault()) {
            return;
        }

        synchronized (myDataLock) {
            myIgnoredFilesComponent.clear();
            new ChangeListManagerSerialization(myIgnoredFilesComponent, myWorker).readExternal(element);
            if (!myWorker.isEmpty() && getDefaultChangeList() == null) {
                setDefaultChangeList(myWorker.getListsCopy().get(0));
            }
        }
        myExcludedConvertedToIgnored = Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, EXCLUDED_CONVERTED_TO_IGNORED_OPTION));
        myConflictTracker.loadState(element);
    }

    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("state");
        if (myProject.isDefault()) {
            return element;
        }

        IgnoredFilesComponent ignoredFilesComponent;
        ChangeListWorker worker;
        synchronized (myDataLock) {
            ignoredFilesComponent = new IgnoredFilesComponent(myIgnoredFilesComponent);
            worker = myWorker.copy();
        }
        ChangeListManagerSerialization.writeExternal(element, ignoredFilesComponent, worker);
        if (myExcludedConvertedToIgnored) {
            JDOMExternalizerUtil.writeField(element, EXCLUDED_CONVERTED_TO_IGNORED_OPTION, String.valueOf(true));
        }
        myConflictTracker.saveState(element);
        return element;
    }

    // used in TeamCity
    @Override
    public void reopenFiles(List<FilePath> paths) {
        ReadonlyStatusHandlerImpl readonlyStatusHandler = (ReadonlyStatusHandlerImpl) ReadonlyStatusHandler.getInstance(myProject);
        boolean savedOption = readonlyStatusHandler.getState().SHOW_DIALOG;
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

        private MyDirtyFilesScheduler(Project project) {
            myProject = project;
            myCnt = 0;
            myEveryThing = false;
        }

        public void accept(Collection<VirtualFile> coll) {
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
            VcsDirtyScopeManager vcsDirtyScopeManager = VcsDirtyScopeManager.getInstance(myProject);
            if (myEveryThing) {
                vcsDirtyScopeManager.markEverythingDirty();
            }
            else {
                vcsDirtyScopeManager.filesDirty(myFiles, myDirs);
            }
        }
    }

    @Override
    public void addFilesToIgnore(IgnoredFileBean... filesToIgnore) {
        myIgnoredFilesComponent.add(filesToIgnore);
        scheduleUnversionedUpdate();
    }

    @Override
    public void addDirectoryToIgnoreImplicitly(@Nonnull String path) {
        myIgnoredFilesComponent.addIgnoredDirectoryImplicitly(path, myProject);
    }

    public IgnoredFilesComponent getIgnoredFilesComponent() {
        return myIgnoredFilesComponent;
    }

    private void scheduleUnversionedUpdate() {
        MyDirtyFilesScheduler scheduler = new MyDirtyFilesScheduler(myProject);

        synchronized (myDataLock) {
            VirtualFileHolder unversionedHolder = myComposite.getVFHolder(FileHolder.HolderType.UNVERSIONED);
            IgnoredFilesHolder ignoredHolder = (IgnoredFilesHolder) myComposite.get(FileHolder.HolderType.IGNORED);

            scheduler.accept(unversionedHolder.getFiles());
            scheduler.accept(ignoredHolder.values());
        }

        scheduler.arise();
    }

    @Override
    public void setFilesToIgnore(IgnoredFileBean... filesToIgnore) {
        myIgnoredFilesComponent.set(filesToIgnore);
        scheduleUnversionedUpdate();
    }

    private void updateIgnoredFiles(FileHolderComposite composite) {
        VirtualFileHolder vfHolder = composite.getVFHolder(FileHolder.HolderType.UNVERSIONED);
        List<VirtualFile> unversionedFiles = vfHolder.getFiles();
        exchangeWithIgnored(composite, vfHolder, unversionedFiles);

        VirtualFileHolder vfModifiedHolder = composite.getVFHolder(FileHolder.HolderType.MODIFIED_WITHOUT_EDITING);
        List<VirtualFile> modifiedFiles = vfModifiedHolder.getFiles();
        exchangeWithIgnored(composite, vfModifiedHolder, modifiedFiles);
    }

    private void exchangeWithIgnored(FileHolderComposite composite, VirtualFileHolder vfHolder, List<VirtualFile> unversionedFiles) {
        for (VirtualFile file : unversionedFiles) {
            if (isIgnoredFile(file)) {
                vfHolder.removeFile(file);
                composite.getIgnoredFileHolder().addFile(file);
            }
        }
    }

    @Override
    public IgnoredFileBean[] getFilesToIgnore() {
        return myIgnoredFilesComponent.getFilesToIgnore();
    }

    @Override
    public boolean isIgnoredFile(@Nonnull FilePath filePath) {
        VcsRoot vcsRoot = ProjectLevelVcsManager.getInstance(myProject).getVcsRootObjectFor(filePath);
        if (vcsRoot == null) {
            return false;
        }

        synchronized (myDataLock) {
            return myComposite.getIgnoredFileHolder().containsFile(filePath, vcsRoot);
        }
    }

    @Override
    @Nullable
    public String getSwitchedBranch(VirtualFile file) {
        synchronized (myDataLock) {
            return myWorker.getBranchForFile(file);
        }
    }

    @Override
    public String getDefaultListName() {
        synchronized (myDataLock) {
            return myWorker.getDefaultListName();
        }
    }

    private static VirtualFile[] collectFiles(List<FilePath> paths) {
        ArrayList<VirtualFile> result = new ArrayList<>();
        for (FilePath path : paths) {
            if (path.getVirtualFile() != null) {
                result.add(path.getVirtualFile());
            }
        }

        return VfsUtilCore.toVirtualFileArray(result);
    }

    @Override
    public boolean setReadOnly(String name, boolean value) {
        return myProject.getApplication().runReadAction((Supplier<Boolean>) () -> {
            synchronized (myDataLock) {
                boolean result = myModifier.setReadOnly(name, value);
                myChangesViewManager.scheduleRefresh();
                return result;
            }
        });
    }

    @Override
    public boolean editName(@Nonnull String fromName, @Nonnull String toName) {
        return myProject.getApplication().runReadAction((Supplier<Boolean>) () -> {
            synchronized (myDataLock) {
                boolean result = myModifier.editName(fromName, toName);
                myChangesViewManager.scheduleRefresh();
                return result;
            }
        });
    }

    @Override
    public String editComment(@Nonnull String fromName, String newComment) {
        return myProject.getApplication().runReadAction((Supplier<String>) () -> {
            synchronized (myDataLock) {
                String oldComment = myModifier.editComment(fromName, newComment);
                myChangesViewManager.scheduleRefresh();
                return oldComment;
            }
        });
    }

    @TestOnly
    public void waitUntilRefreshed() {
        VcsDirtyScopeVfsListener.getInstance(myProject).waitForAsyncTaskCompletion();
        myUpdater.waitUntilRefreshed();
        waitUpdateAlarm();
    }

    // this is for perforce tests to ensure that LastSuccessfulUpdateTracker receives the event it needs
    private void waitUpdateAlarm() {
        Semaphore semaphore = new Semaphore();
        semaphore.down();
        myScheduler.submit(semaphore::up);
        semaphore.waitFor();
    }

    public void executeOnUpdaterThread(Runnable runnable) {
        myScheduler.submit(runnable);
    }

    @Override
    @TestOnly
    public boolean ensureUpToDate(boolean canBeCanceled) {
        if (myProject.getApplication().isDispatchThread()) {
            updateImmediately();
            return true;
        }
        VcsDirtyScopeVfsListener.getInstance(myProject).waitForAsyncTaskCompletion();
        myUpdater.waitUntilRefreshed();
        waitUpdateAlarm();
        return true;
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

    private static class MyChangesDeltaForwarder implements PlusMinusModify<BaseRevision> {
        private final RemoteRevisionsCache myRevisionsCache;
        private final ProjectLevelVcsManager myVcsManager;
        private final Project myProject;
        private final ChangeListScheduler myScheduler;

        public MyChangesDeltaForwarder(Project project, ChangeListScheduler scheduler) {
            myProject = project;
            myScheduler = scheduler;
            myRevisionsCache = RemoteRevisionsCache.getInstance(project);
            myVcsManager = ProjectLevelVcsManager.getInstance(project);
        }

        @Override
        public void modify(BaseRevision was, BaseRevision become) {
            myScheduler.submit(() -> {
                AbstractVcs vcs = getVcs(was);
                if (vcs != null) {
                    myRevisionsCache.plus(Pair.create(was.getPath().getPath(), vcs));
                }
                // maybe define modify method?
                myProject.getMessageBus().syncPublisher(VcsAnnotationRefresher.class).dirty(become);
            });
        }

        @Override
        public void plus(BaseRevision baseRevision) {
            myScheduler.submit(() -> {
                AbstractVcs vcs = getVcs(baseRevision);
                if (vcs != null) {
                    myRevisionsCache.plus(Pair.create(baseRevision.getPath().getPath(), vcs));
                }
                myProject.getMessageBus().syncPublisher(VcsAnnotationRefresher.class).dirty(baseRevision);
            });
        }

        @Override
        public void minus(BaseRevision baseRevision) {
            myScheduler.submit(() -> {
                AbstractVcs vcs = getVcs(baseRevision);
                if (vcs != null) {
                    myRevisionsCache.minus(Pair.create(baseRevision.getPath().getPath(), vcs));
                }
                myProject.getMessageBus().syncPublisher(VcsAnnotationRefresher.class).dirty(baseRevision.getPath().getPath());
            });
        }

        @Nullable
        private AbstractVcs getVcs(BaseRevision baseRevision) {
            VcsKey vcsKey = baseRevision.getVcs();
            if (vcsKey == null) {
                FilePath path = baseRevision.getPath();
                vcsKey = findVcs(path);
                if (vcsKey == null) {
                    return null;
                }
            }
            return myVcsManager.findVcsByName(vcsKey.getName());
        }

        @Nullable
        private VcsKey findVcs(FilePath path) {
            // does not matter directory or not
            VirtualFile vf = path.getVirtualFile();
            if (vf == null) {
                vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.getIOFile());
            }
            if (vf == null) {
                return null;
            }
            AbstractVcs vcs = myVcsManager.getVcsFor(vf);
            return vcs == null ? null : vcs.getKeyInstanceMethod();
        }
    }

    @Override
    @RequiredUIAccess
    public boolean isFreezedWithNotification(String modalTitle) {
        String freezeReason = isFreezed();
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
