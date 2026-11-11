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
package consulo.ide.impl.idea.ide;

import consulo.application.Application;
import consulo.application.SaveAndSyncHandler;
import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.document.FileDocumentManager;
import consulo.document.internal.FileDocumentManagerEx;
import consulo.fileEditor.FileEditorManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * What saving and syncing means with nothing of the toolkit in it. What a frontend adds is when the handler is
 * asked to do it - the toolkit bound one saves after the frame goes quiet, which needs a queue of input events.
 *
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public abstract class BaseSaveAndSyncHandler implements SaveAndSyncHandler, Disposable {
    protected static final Logger LOG = Logger.getInstance(SaveAndSyncHandler.class);

    protected final Application myApplication;
    protected final GeneralSettings mySettings;
    protected final FileDocumentManager myFileDocumentManager;

    private final ProgressManager myProgressManager;

    private final AtomicInteger myBlockSaveOnFrameDeactivationCount = new AtomicInteger();
    private final AtomicInteger myBlockSyncOnFrameActivationCount = new AtomicInteger();

    private volatile long myRefreshSessionId;
    private Future<?> myRefreshDelayAlarm = CompletableFuture.completedFuture(null);

    private final Object myBackgroundRefreshLock = new Object();
    private Future<?> myBackgroundRefresh = CompletableFuture.completedFuture(null);
    private final AtomicBoolean myBackgroundRefreshRunning = new AtomicBoolean();

    private UIAccess myStableUIAccess;

    protected BaseSaveAndSyncHandler(
        Application application,
        GeneralSettings generalSettings,
        ProgressManager progressManager,
        FileDocumentManager fileDocumentManager
    ) {
        myApplication = application;
        mySettings = generalSettings;
        myProgressManager = progressManager;
        myFileDocumentManager = fileDocumentManager;
        myStableUIAccess = UIAccess.supportsMultipleUI() ? null : UIAccess.listAll().getFirst();
    }

    @Override
    public void dispose() {
        cancelBackgroundRefresh();
        RefreshQueue.getInstance().cancelSession(myRefreshSessionId);
    }

    public void onFrameActivated() {
        if (!isIdleBackgroundRefresh()) {
            cancelBackgroundRefresh();
        }

        if (!myApplication.isDisposed() && mySettings.isSyncOnFrameActivation()) {
            scheduleRefresh();
        }
    }

    @RequiredUIAccess
    public void onFrameDeactivated() {
        UIAccess uiAccess = UIAccess.current();

        LOG.debug("save(): enter");
        if (canSyncOrSave(uiAccess)) {
            saveProjectsAndDocuments(uiAccess);
        }
        LOG.debug("save(): exit");

        if (!isIdleBackgroundRefresh()) {
            startBackgroundRefresh(true);
        }
    }

    /**
     * {@code true} - the VFS is refreshed in the background while there is no user activity in the IDE,
     * {@code false} - while the IDE frame is not focused
     */
    protected static boolean isIdleBackgroundRefresh() {
        return Registry.is("vfs.background.refresh.on.idle", true);
    }

    /**
     * Called by the frontend when the user has been inactive for {@link #getBackgroundRefreshIntervalSeconds()}
     */
    protected void onUserInactive() {
        if (isIdleBackgroundRefresh()) {
            startBackgroundRefresh(false);
        }
    }

    /**
     * Called by the frontend on any user activity
     */
    protected void onUserActivity() {
        if (isIdleBackgroundRefresh()) {
            cancelBackgroundRefresh();
        }
    }

    protected static int getBackgroundRefreshIntervalSeconds() {
        return Math.max(1, Registry.intValue("vfs.background.refresh.interval", 15));
    }

    private void startBackgroundRefresh(boolean delayFirst) {
        synchronized (myBackgroundRefreshLock) {
            myBackgroundRefresh.cancel(false);
            myBackgroundRefreshRunning.set(false);
            if (!mySettings.isBackgroundSync()) {
                return;
            }
            long interval = getBackgroundRefreshIntervalSeconds();
            myBackgroundRefresh = AppExecutorUtil.getAppScheduledExecutorService()
                .scheduleWithFixedDelay(this::refreshAllLocalRootsInBackground, delayFirst ? interval : 0, interval, TimeUnit.SECONDS);
        }
    }

    private void cancelBackgroundRefresh() {
        synchronized (myBackgroundRefreshLock) {
            if (myBackgroundRefresh.isDone()) {
                return;
            }
            myBackgroundRefresh.cancel(false);
            myBackgroundRefresh = CompletableFuture.completedFuture(null);
        }
    }

    private void refreshAllLocalRootsInBackground() {
        if (myApplication.isDisposed() || !mySettings.isBackgroundSync() || ProjectManager.getInstance().getOpenProjects().length == 0) {
            return;
        }
        VirtualFile[] roots = ManagingFS.getInstance().getLocalRoots();
        if (roots.length == 0 || myBlockSyncOnFrameActivationCount.get() != 0) {
            return;
        }
        if (!ContainerUtil.exists(roots, root -> root instanceof NewVirtualFile file && file.isDirty())) {
            return;
        }
        if (!myBackgroundRefreshRunning.compareAndSet(false, true)) {
            return;
        }
        LOG.debug("VFS refresh started (background sync)");
        RefreshQueue.getInstance().refresh(true, true, () -> myBackgroundRefreshRunning.set(false), roots);
    }

    @RequiredUIAccess
    protected void saveAllDocumentsIfInactive() {
        if (mySettings.isAutoSaveIfInactive() && canSyncOrSave(UIAccess.current())) {
            ((FileDocumentManagerEx) myFileDocumentManager).saveAllDocuments(false);
        }
    }

    @RequiredUIAccess
    protected boolean canSyncOrSave(UIAccess uiAccess) {
        return !uiAccess.isInModalContext() && !myProgressManager.hasModalProgressIndicator();
    }

    @Override
    public void saveProjectsAndDocuments(UIAccess uiAccess) {
        if (!myApplication.isDisposed() && mySettings.isSaveOnFrameDeactivation() && myBlockSaveOnFrameDeactivationCount.get() == 0) {
            myApplication.saveAllWithProgress(uiAccess);
        }
    }

    @Override
    public void scheduleRefresh() {
        myRefreshDelayAlarm.cancel(false);
        if (myStableUIAccess == null) {
            myRefreshDelayAlarm = AppExecutorUtil.getAppScheduledExecutorService()
                .schedule(this::doScheduledRefreshBackground, 300, TimeUnit.MILLISECONDS);
        }
        else {
            myRefreshDelayAlarm = myStableUIAccess.getScheduler().schedule(this::doScheduledRefreshFromUI, 300, TimeUnit.MILLISECONDS);
        }

    }

    @RequiredUIAccess
    private void doScheduledRefreshFromUI() {
        if (canSyncOrSave(UIAccess.current())) {
            refreshOpenFiles();
        }
        maybeRefresh(myApplication.getNoneModalityState());
    }

    private void doScheduledRefreshBackground() {
        refreshOpenFiles();

        maybeRefresh(myApplication.getNoneModalityState());
    }

    public void maybeRefresh(ModalityState modalityState) {
        if (myBlockSyncOnFrameActivationCount.get() == 0 && mySettings.isSyncOnFrameActivation()) {
            RefreshQueue queue = RefreshQueue.getInstance();
            queue.cancelSession(myRefreshSessionId);

            RefreshSession session = queue.createSession(true, true, null, modalityState);
            session.addAllFiles(ManagingFS.getInstance().getLocalRoots());
            myRefreshSessionId = session.getId();
            session.launch();
            LOG.debug("vfs refreshed");
        }
        else if (LOG.isDebugEnabled()) {
            LOG.debug("vfs refresh rejected, blocked: " + (myBlockSyncOnFrameActivationCount.get() != 0) + ", isSyncOnFrameActivation: " + mySettings
                .isSyncOnFrameActivation());
        }
    }

    @Override
    public void refreshOpenFiles() {
        List<VirtualFile> files = new ArrayList<>();

        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            for (VirtualFile file : FileEditorManager.getInstance(project).getSelectedFiles()) {
                if (file instanceof VirtualFileWithId) {
                    files.add(file);
                }
            }
        }

        if (!files.isEmpty()) {
            // refresh open files synchronously so it doesn't wait for potentially longish refresh request in the queue to finish
            RefreshQueue.getInstance().refresh(false, false, null, files);
        }
    }

    @Override
    public void blockSaveOnFrameDeactivation() {
        myBlockSaveOnFrameDeactivationCount.incrementAndGet();
    }

    @Override
    public void unblockSaveOnFrameDeactivation() {
        myBlockSaveOnFrameDeactivationCount.decrementAndGet();
    }

    @Override
    public void blockSyncOnFrameActivation() {
        myBlockSyncOnFrameActivationCount.incrementAndGet();
    }

    @Override
    public void unblockSyncOnFrameActivation() {
        myBlockSyncOnFrameActivationCount.decrementAndGet();
    }

    @Override
    public boolean isSaveOnFrameDeactivation() {
        return mySettings.isSaveOnFrameDeactivation();
    }

    @Override
    public boolean isSyncOnFrameActivation() {
        return mySettings.isSyncOnFrameActivation();
    }
}
