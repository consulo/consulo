// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.autoimport;

import consulo.application.ReadAction;
import consulo.component.util.SimpleModificationTracker;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.externalSystem.autoimport.ExternalSystemModificationType;
import consulo.externalSystem.autoimport.ExternalSystemProjectAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectListener;
import consulo.externalSystem.autoimport.ExternalSystemRefreshStatus;
import consulo.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext;
import consulo.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext.Event;
import consulo.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext.ReloadStatus;
import consulo.externalSystem.autoimport.ExternalSystemSettingsFilesReloadContext;
import consulo.externalSystem.impl.internal.autoimport.AutoImportProjectStatus.ProjectEvent;
import consulo.externalSystem.impl.internal.autoimport.AutoImportProjectStatus.Stamp;
import consulo.externalSystem.impl.internal.autoimport.changes.AsyncFileChangesListener;
import consulo.externalSystem.impl.internal.autoimport.changes.FilesChangesListener;
import consulo.externalSystem.impl.internal.autoimport.changes.NewFilesListener;
import consulo.externalSystem.impl.internal.autoimport.settings.AsyncSupplier;
import consulo.externalSystem.impl.internal.autoimport.settings.BackgroundAsyncSupplier;
import consulo.externalSystem.impl.internal.observable.AtomicOperationTrace;
import consulo.externalSystem.impl.internal.util.CrcUtils;
import consulo.externalSystem.impl.internal.util.cache.AsyncLocalCache;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class AutoImportProjectSettingsFilesTracker {
    private static final Logger LOG = Logger.getInstance("#consulo.externalSystem.autoimport");

    private final Project myProject;
    private final AutoImportProjectTracker myProjectTracker;
    private final Executor myBackgroundExecutor;
    private final ExternalSystemProjectAware myProjectAware;
    private final Disposable myParentDisposable;

    private final AutoImportProjectStatus myProjectStatus;

    private final AtomicReference<SettingsFilesStatus> mySettingsFilesStatus = new AtomicReference<>(new SettingsFilesStatus());

    private final AtomicOperationTrace myApplyChangesOperation = new AtomicOperationTrace("Apply changes operation");

    private final SettingsFilesAsyncSupplier mySettingsAsyncSupplier;

    public AutoImportProjectSettingsFilesTracker(
        Project project,
        AutoImportProjectTracker projectTracker,
        Executor backgroundExecutor,
        ExternalSystemProjectAware projectAware,
        Disposable parentDisposable
    ) {
        myProject = project;
        myProjectTracker = projectTracker;
        myBackgroundExecutor = backgroundExecutor;
        myProjectAware = projectAware;
        myParentDisposable = parentDisposable;
        myProjectStatus = new AutoImportProjectStatus("[settings-tracker] " + projectAware.getProjectId());
        mySettingsAsyncSupplier = new SettingsFilesAsyncSupplier();

        projectAware.subscribe(new ProjectListener(), parentDisposable);
        NewFilesListener.whenNewFilesCreated(files -> mySettingsAsyncSupplier.invalidate(), parentDisposable);
        AsyncFileChangesListener.subscribeOnDocumentsAndVirtualFilesChanges(mySettingsAsyncSupplier, new ProjectSettingsListener(), parentDisposable);
    }

    private Map<String, Long> calculateSettingsFilesCRC(Set<String> settingsFiles) {
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        Map<String, Long> result = new LinkedHashMap<>();
        for (String settingsFile : settingsFiles) {
            VirtualFile file = localFileSystem.findFileByPath(settingsFile);
            if (file == null) {
                continue;
            }
            long crc = calculateCrc(file);
            if (crc != 0L) {
                result.put(file.getPath(), crc);
            }
        }
        return result;
    }

    private long calculateCrc(VirtualFile file) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = fileDocumentManager.getCachedDocument(file);
        if (document != null) {
            return CrcUtils.calculateCrc(document, myProject, myProjectAware.getProjectId().getSystemId(), file);
        }
        return CrcUtils.calculateCrc(file, myProject, myProjectAware.getProjectId().getSystemId());
    }

    public boolean isUpToDate() {
        return myProjectStatus.isUpToDate();
    }

    public ExternalSystemModificationType getModificationType() {
        return myProjectStatus.getModificationType();
    }

    public ExternalSystemSettingsFilesReloadContext getSettingsContext() {
        SettingsFilesStatus status = mySettingsFilesStatus.get();
        return new SettingsFilesReloadContext(status.getUpdated(), status.getCreated(), status.getDeleted());
    }

    /**
     * Updates settings files status using new CRCs.
     *
     * @param reloadStatus see {@link #adjustCrc} for details
     */
    private SettingsFilesStatus updateSettingsFilesStatus(String operationName, Map<String, Long> newCRC, ReloadStatus reloadStatus) {
        return mySettingsFilesStatus.updateAndGet(it -> adjustCrc(new SettingsFilesStatus(it.getOldCRC(), newCRC), operationName, reloadStatus));
    }

    /**
     * Adjusts settings files status.
     * <p>
     * It allows ignoring files modifications by rules from an external system.
     * For example, some build systems needed to ignore file updates during reload.
     *
     * @see ExternalSystemProjectAware#isIgnoredSettingsFileEvent
     */
    private SettingsFilesStatus adjustCrc(SettingsFilesStatus settingsFilesStatus, String operationName, ReloadStatus reloadStatus) {
        ExternalSystemModificationType modificationType = getModificationType();
        Map<String, Long> oldCRC = new HashMap<>(settingsFilesStatus.getOldCRC());
        for (String path : settingsFilesStatus.getUpdated()) {
            SettingsFilesModificationContext context = new SettingsFilesModificationContext(Event.UPDATE, modificationType, reloadStatus);
            if (myProjectAware.isIgnoredSettingsFileEvent(path, context)) {
                oldCRC.put(path, settingsFilesStatus.getNewCRC().get(path));
            }
        }
        for (String path : settingsFilesStatus.getCreated()) {
            SettingsFilesModificationContext context = new SettingsFilesModificationContext(Event.CREATE, modificationType, reloadStatus);
            if (myProjectAware.isIgnoredSettingsFileEvent(path, context)) {
                oldCRC.put(path, settingsFilesStatus.getNewCRC().get(path));
            }
        }
        for (String path : settingsFilesStatus.getDeleted()) {
            SettingsFilesModificationContext context = new SettingsFilesModificationContext(Event.DELETE, modificationType, reloadStatus);
            if (myProjectAware.isIgnoredSettingsFileEvent(path, context)) {
                oldCRC.remove(path);
            }
        }
        SettingsFilesStatus status = new SettingsFilesStatus(oldCRC, settingsFilesStatus.getNewCRC());
        LOG.debug(
            "[" + operationName + "] " +
                "ReloadStatus=" + reloadStatus + ", " +
                "ModificationType=" + modificationType + ", " +
                "Updated=" + status.getUpdated() + ", " +
                "Created=" + status.getCreated() + ", " +
                "Deleted=" + status.getDeleted()
        );
        return status;
    }

    public AutoImportProjectSettingsFilesTrackerState getState() {
        AutoImportProjectSettingsFilesTrackerState state = new AutoImportProjectSettingsFilesTrackerState();
        state.isDirty = myProjectStatus.isDirty();
        state.settingsFiles = new HashMap<>(mySettingsFilesStatus.get().getOldCRC());
        return state;
    }

    public void loadState(AutoImportProjectSettingsFilesTrackerState state) {
        Stamp operationStamp = Stamp.nextStamp();
        mySettingsFilesStatus.set(new SettingsFilesStatus(new HashMap<>(state.settingsFiles)));
        if (state.isDirty) {
            myProjectStatus.markDirty(operationStamp, ExternalSystemModificationType.EXTERNAL);
        }
        else {
            myProjectStatus.markSynchronized(operationStamp);
        }
    }

    public void refreshChanges() {
        submitSettingsFilesStatusUpdate("refreshChanges", context -> {
            context.myRefreshVfs = true;
            context.mySyncEvent = ProjectEvent.Revert::new;
            context.myChangeEvent = ProjectEvent::externalInvalidate;
            context.myCallback = myProjectTracker::scheduleChangeProcessing;
        });
    }

    private void submitSettingsFilesCollection(boolean isRefreshVfs, boolean isInvalidateCache, Consumer<Set<String>> callback) {
        if (isInvalidateCache || isRefreshVfs) {
            mySettingsAsyncSupplier.invalidate();
        }
        mySettingsAsyncSupplier.supply(settingsPaths -> {
            if (isRefreshVfs) {
                List<File> settingsFiles = settingsPaths.stream().map(File::new).toList();
                if (!settingsFiles.isEmpty()) {
                    LocalFileSystem.getInstance().refreshIoFiles(settingsFiles);
                }
            }
            callback.accept(settingsPaths);
        });
    }

    private void submitSettingsFilesStatusUpdate(String operationName, Consumer<SettingsFilesStatusUpdateContext> configureContext) {
        ReloadStatus reloadStatus = myApplyChangesOperation.isOperationInProgress() ? ReloadStatus.IN_PROGRESS : ReloadStatus.IDLE;
        SettingsFilesStatusUpdateContext context = new SettingsFilesStatusUpdateContext(reloadStatus);
        configureContext.accept(context);
        submitSettingsFilesStatusUpdate(operationName, context);
    }

    private void submitSettingsFilesStatusUpdate(String operationName, SettingsFilesStatusUpdateContext context) {
        submitSettingsFilesCollection(context.myRefreshVfs, context.myInvalidateCache, settingsPaths -> {
            // This operation is used for ordering between an update operation and file changes listener.
            // Therefore, the operation stamp cannot be taken earlier than VFS refresh for the settings files.
            // @see the AsyncFileChangesListener#apply function for details
            Stamp operationStamp = Stamp.nextStamp();
            Map<String, Long> newSettingsFilesCRC = ReadAction.nonBlocking(() -> calculateSettingsFilesCRC(settingsPaths))
                .expireWith(myParentDisposable)
                .executeSynchronously();
            SettingsFilesStatus settingsFilesStatus = updateSettingsFilesStatus(operationName, newSettingsFilesCRC, context.myReloadStatus);
            updateProjectStatus(operationStamp, context.mySyncEvent, context.myChangeEvent, settingsFilesStatus);
            Runnable callback = context.myCallback;
            if (callback != null) {
                callback.run();
            }
        });
    }

    private void updateProjectStatus(
        Stamp operationStamp,
        @Nullable Function<Stamp, ProjectEvent> syncEvent,
        @Nullable Function<Stamp, ProjectEvent> changeEvent,
        SettingsFilesStatus settingsFilesStatus
    ) {
        Function<Stamp, ProjectEvent> event = settingsFilesStatus.hasChanges() ? changeEvent : syncEvent;
        if (event != null) {
            myProjectStatus.update(event.apply(operationStamp));
        }
    }

    public void beforeApplyChanges(Disposable parentDisposable, Runnable listener) {
        myApplyChangesOperation.whenOperationStarted(parentDisposable, listener);
    }

    public void afterApplyChanges(Disposable parentDisposable, Runnable listener) {
        myApplyChangesOperation.whenOperationFinished(parentDisposable, listener);
    }

    private static class SettingsFilesStatusUpdateContext {
        private ReloadStatus myReloadStatus;
        private boolean myRefreshVfs = false;
        private boolean myInvalidateCache = false;
        private @Nullable Function<Stamp, ProjectEvent> mySyncEvent;
        private @Nullable Function<Stamp, ProjectEvent> myChangeEvent;
        private @Nullable Runnable myCallback;

        private SettingsFilesStatusUpdateContext(ReloadStatus reloadStatus) {
            myReloadStatus = reloadStatus;
        }
    }

    private static class SettingsFilesStatus {
        private final Map<String, Long> myOldCRC;
        private final Map<String, Long> myNewCRC;

        private @Nullable Set<String> myUpdated;
        private @Nullable Set<String> myCreated;
        private @Nullable Set<String> myDeleted;

        private SettingsFilesStatus() {
            this(Map.of());
        }

        private SettingsFilesStatus(Map<String, Long> crc) {
            this(crc, crc);
        }

        private SettingsFilesStatus(Map<String, Long> oldCRC, Map<String, Long> newCRC) {
            myOldCRC = oldCRC;
            myNewCRC = newCRC;
        }

        public Map<String, Long> getOldCRC() {
            return myOldCRC;
        }

        public Map<String, Long> getNewCRC() {
            return myNewCRC;
        }

        public synchronized Set<String> getUpdated() {
            Set<String> updated = myUpdated;
            if (updated == null) {
                updated = new HashSet<>();
                for (String path : myOldCRC.keySet()) {
                    if (myNewCRC.containsKey(path) && !myOldCRC.get(path).equals(myNewCRC.get(path))) {
                        updated.add(path);
                    }
                }
                myUpdated = updated;
            }
            return updated;
        }

        public synchronized Set<String> getCreated() {
            Set<String> created = myCreated;
            if (created == null) {
                created = new HashSet<>(myNewCRC.keySet());
                created.removeAll(myOldCRC.keySet());
                myCreated = created;
            }
            return created;
        }

        public synchronized Set<String> getDeleted() {
            Set<String> deleted = myDeleted;
            if (deleted == null) {
                deleted = new HashSet<>(myOldCRC.keySet());
                deleted.removeAll(myNewCRC.keySet());
                myDeleted = deleted;
            }
            return deleted;
        }

        public boolean hasChanges() {
            return !getUpdated().isEmpty() || !getCreated().isEmpty() || !getDeleted().isEmpty();
        }
    }

    private record SettingsFilesReloadContext(Set<String> updated, Set<String> created, Set<String> deleted)
        implements ExternalSystemSettingsFilesReloadContext {
        @Override
        public Set<String> getUpdated() {
            return updated;
        }

        @Override
        public Set<String> getCreated() {
            return created;
        }

        @Override
        public Set<String> getDeleted() {
            return deleted;
        }
    }

    private record SettingsFilesModificationContext(
        Event event,
        ExternalSystemModificationType modificationType,
        ReloadStatus reloadStatus
    ) implements ExternalSystemSettingsFilesModificationContext {
        @Override
        public Event getEvent() {
            return event;
        }

        @Override
        public ExternalSystemModificationType getModificationType() {
            return modificationType;
        }

        @Override
        public ReloadStatus getReloadStatus() {
            return reloadStatus;
        }
    }

    private class ProjectListener implements ExternalSystemProjectListener {
        @Override
        public void onProjectReloadStart() {
            myApplyChangesOperation.traceStart();
            submitSettingsFilesStatusUpdate("onProjectReloadStart", context -> {
                context.myRefreshVfs = true;
                context.myReloadStatus = ReloadStatus.JUST_STARTED;
                context.mySyncEvent = ProjectEvent.Synchronize::new;
                context.myChangeEvent = ProjectEvent::externalInvalidate;
            });
        }

        @Override
        public void onProjectReloadFinish(ExternalSystemRefreshStatus status) {
            submitSettingsFilesStatusUpdate("onProjectReloadFinish", context -> {
                context.myRefreshVfs = true;
                context.myReloadStatus = ReloadStatus.JUST_FINISHED;
                context.mySyncEvent = ProjectEvent.Synchronize::new;
                context.myChangeEvent = ProjectEvent::externalInvalidate;
                context.myCallback = myApplyChangesOperation::traceFinish;
            });
        }

        @Override
        public void onSettingsFilesListChange() {
            submitSettingsFilesStatusUpdate("onSettingsFilesListChange", context -> {
                context.myInvalidateCache = true;
                context.mySyncEvent = ProjectEvent.Revert::new;
                context.myChangeEvent = ProjectEvent::externalModify;
                context.myCallback = myProjectTracker::scheduleChangeProcessing;
            });
        }
    }

    private class ProjectSettingsListener implements FilesChangesListener {
        @Override
        public void onFileChange(Stamp stamp, String path, long modificationStamp, ExternalSystemModificationType modificationType) {
            ExternalSystemModificationType adjustedModificationType = myProjectAware.adjustModificationType(path, modificationType);
            logModificationAsDebug(path, modificationStamp, modificationType, adjustedModificationType);
            myProjectStatus.markModified(stamp, adjustedModificationType);
        }

        @Override
        public void apply() {
            submitSettingsFilesStatusUpdate("ProjectSettingsListener.apply", context -> {
                context.mySyncEvent = ProjectEvent.Revert::new;
                context.myCallback = myProjectTracker::scheduleChangeProcessing;
            });
        }

        private void logModificationAsDebug(
            String path,
            long modificationStamp,
            ExternalSystemModificationType type,
            ExternalSystemModificationType adjustedType
        ) {
            if (LOG.isDebugEnabled()) {
                String projectPath = myProjectAware.getProjectId().getExternalProjectPath();
                String relativePath = FileUtil.getRelativePath(projectPath, path, '/');
                LOG.debug("File " + (relativePath == null ? path : relativePath) + " is modified at " + modificationStamp +
                    " as " + type + " (adjusted to " + adjustedType + ")");
            }
        }
    }

    private class SettingsFilesAsyncSupplier implements AsyncSupplier<Set<String>> {
        private final SimpleModificationTracker myModificationTracker = new SimpleModificationTracker();

        private final AsyncLocalCache<Set<String>> mySettingsFilesCache = new AsyncLocalCache<>();

        private final BackgroundAsyncSupplier<Set<String>> mySupplier = new BackgroundAsyncSupplier<>(
            AsyncSupplier.blocking(this::getOrCollectSettingsFiles),
            AutoImportProjectTracker::isAsyncChangesProcessing,
            myBackgroundExecutor,
            myParentDisposable
        );

        private Set<String> getOrCollectSettingsFiles() {
            return mySettingsFilesCache.getOrCreateValueBlocking(myModificationTracker.getModificationCount(), () -> {
                Set<String> result = new HashSet<>();
                for (String settingsFile : myProjectAware.getSettingsFiles()) {
                    result.add(FileUtil.toSystemIndependentName(settingsFile));
                }
                return result;
            });
        }

        @Override
        public void supply(Consumer<Set<String>> consumer) {
            mySupplier.supply(settingsFiles -> {
                Set<String> result = new HashSet<>(settingsFiles);
                result.addAll(mySettingsFilesStatus.get().getOldCRC().keySet());
                consumer.accept(result);
            });
        }

        public void invalidate() {
            myModificationTracker.incModificationCount();
        }
    }
}
