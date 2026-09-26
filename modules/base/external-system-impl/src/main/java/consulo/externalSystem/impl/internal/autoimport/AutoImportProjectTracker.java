// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.autoimport;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalSystem.autoimport.ExternalSystemAutoImportAwareListener;
import consulo.externalSystem.autoimport.ExternalSystemModificationType;
import consulo.externalSystem.autoimport.ExternalSystemProjectAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectId;
import consulo.externalSystem.autoimport.ExternalSystemProjectListener;
import consulo.externalSystem.autoimport.ExternalSystemProjectNotificationAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectReloadContext;
import consulo.externalSystem.autoimport.ExternalSystemProjectTracker;
import consulo.externalSystem.autoimport.ExternalSystemProjectTrackerSettings;
import consulo.externalSystem.autoimport.ExternalSystemProjectTrackerSettings.AutoReloadType;
import consulo.externalSystem.autoimport.ExternalSystemRefreshStatus;
import consulo.externalSystem.autoimport.ExternalSystemSettingsFilesReloadContext;
import consulo.externalSystem.impl.internal.autoimport.AutoImportProjectStatus.Stamp;
import consulo.externalSystem.impl.internal.autoimport.update.PriorityEatUpdate;
import consulo.externalSystem.impl.internal.observable.AtomicOperationTrace;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.event.BatchFileChangeListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kava.beans.PropertyChangeEvent;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;


@Singleton
@ServiceImpl
@State(name = "ExternalSystemProjectTracker", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class AutoImportProjectTracker implements ExternalSystemProjectTracker, Disposable, PersistentStateComponent<AutoImportProjectTrackerState> {
    private static final Logger LOG = Logger.getInstance("#consulo.externalSystem.autoimport");

    private static final AtomicBoolean ourOnceIgnoreDisableReloadRegistry = new AtomicBoolean(false);

    private static volatile boolean ourEnableReload = !Application.get().isUnitTestMode();

    private static volatile boolean ourEnableAutoReload = !Application.get().isUnitTestMode();

    private static volatile boolean ourAsyncChangesProcessing = !Application.get().isHeadlessEnvironment()
        || Boolean.getBoolean("intellij.progress.task.ignoreHeadless")
        || Boolean.getBoolean("external.system.auto.import.headless.async");

    private static volatile Duration ourMergingTimeSpan = Duration.ofMillis(300);

    private static volatile Duration ourAutoReloadDelay = Duration.ofSeconds(3);

    private final Project myProject;

    private final Disposable myServiceDisposable = this;

    private final AutoImportProjectTrackerSettings mySettings;
    private final ExternalSystemProjectNotificationAware myNotificationAware;

    private final Map<StateKey, AutoImportProjectDataState> myProjectDataStates = new ConcurrentHashMap<>();
    private final Map<ExternalSystemProjectId, ProjectData> myProjectDataMap = new ConcurrentHashMap<>();
    private final AtomicOperationTrace myProjectChangeOperation = new AtomicOperationTrace("Project change operation");
    private final AtomicBoolean myIsProjectLookupActive = new AtomicBoolean(false);

    private final ExecutorService myBackgroundExecutor =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("AutoImportProjectTracker.backgroundExecutor", 1);

    private final MergingUpdateQueue myDispatcher;

    private volatile Duration myDispatcherMergingTimeSpan;

    @Inject
    public AutoImportProjectTracker(
        Project project,
        ExternalSystemProjectTrackerSettings settings,
        ExternalSystemProjectNotificationAware notificationAware
    ) {
        myProject = project;
        mySettings = (AutoImportProjectTrackerSettings) settings;
        myNotificationAware = notificationAware;
        myDispatcherMergingTimeSpan = getMergingTimeSpan();
        myDispatcher = new MergingUpdateQueue(
            "AutoImportProjectTracker.dispatcher",
            (int) myDispatcherMergingTimeSpan.toMillis(),
            true,
            null,
            myServiceDisposable,
            null,
            Alarm.ThreadToUse.POOLED_THREAD
        );

        LOG.debug("Project tracker initialization");

        myProjectChangeOperation.whenOperationStarted(myServiceDisposable, () -> {
            LOG.debug("Detected project change start event");
            scheduleChangeProcessing();
        });
        myProjectChangeOperation.whenOperationFinished(myServiceDisposable, () -> {
            LOG.debug("Detected project change finish event");
            scheduleChangeProcessing();
        });
        mySettings.addAutoReloadTypeListener(myServiceDisposable, () -> {
            LOG.debug("Detected project reload type change event");
            scheduleChangeProcessing();
        });

        myDispatcher.setPassThrough(!ourAsyncChangesProcessing);

        myDispatcher.setRestartTimerOnAdd(true);

        project.getApplication().getMessageBus().connect(myServiceDisposable)
            .subscribe(BatchFileChangeListener.class, createProjectChangesListener());
        LookupManager.getInstance(project).addPropertyChangeListener(this::onLookupPropertyChange, myServiceDisposable);
        project.getMessageBus().connect(myServiceDisposable)
            .subscribe(ExternalSystemAutoImportAwareListener.class, createExternalSystemAutoImportAwareListener());
    }

    public static AutoImportProjectTracker getInstance(Project project) {
        return (AutoImportProjectTracker) ExternalSystemProjectTracker.getInstance(project);
    }

    private ProjectBatchFileChangeListener createProjectChangesListener() {
        return new ProjectBatchFileChangeListener(myProject) {
            @Override
            public void batchChangeStarted(@Nullable String activityName) {
                myProjectChangeOperation.traceStart();
            }

            @Override
            public void batchChangeCompleted() {
                myProjectChangeOperation.traceFinish();
            }
        };
    }

    private ExternalSystemAutoImportAwareListener createExternalSystemAutoImportAwareListener() {
        return new ExternalSystemAutoImportAwareListener() {
            @Override
            public void autoImportAwareOperationStarted() {
                myProjectChangeOperation.traceStart();
            }

            @Override
            public void autoImportAwareOperationCompleted() {
                myProjectChangeOperation.traceFinish();
            }
        };
    }

    private ExternalSystemProjectListener createProjectReloadListener(ProjectData projectData) {
        return new ExternalSystemProjectListener() {
            @Override
            public void onProjectReloadStart() {
                projectData.reloadOperation().traceStart();
                projectData.status().markSynchronized(Stamp.nextStamp());
                activate(projectData);
            }

            @Override
            public void onProjectReloadFinish(ExternalSystemRefreshStatus status) {
                if (status != ExternalSystemRefreshStatus.SUCCESS) {
                    projectData.status().markBroken(Stamp.nextStamp());
                }
                projectData.reloadOperation().traceFinish();
            }
        };
    }

    private void onLookupPropertyChange(PropertyChangeEvent event) {
        if (LookupManager.PROP_ACTIVE_LOOKUP.equals(event.getPropertyName())) {
            boolean isActive = event.getNewValue() != null;
            if (myIsProjectLookupActive.getAndSet(isActive) != isActive) {
                LOG.debug(isActive ? "Detected project lookup start event" : "Detected project lookup finish event");
                scheduleChangeProcessing();
            }
        }
    }

    @Override
    public void scheduleProjectRefresh() {
        LOG.debug("Schedule change processing (isExplicitReload=true)");

        schedule(0, 1, () -> processChanges(true));
    }

    @Override
    public void scheduleChangeProcessing() {
        LOG.debug("Schedule change processing (isExplicitReload=false)");

        schedule(1, 1, () -> processChanges(false));
    }

    /**
     * @see AutoImportProjectTracker#scheduleProjectRefresh
     * @see AutoImportProjectTracker#scheduleChangeProcessing
     */
    private void scheduleDelayedProjectReload(List<Pair<ExternalSystemProjectAware, ExternalSystemProjectReloadContext>> projectsToReload) {
        LOG.debug("Schedule delayed project reload");

        Duration smartProjectReloadDelay = myProjectDataMap.values().stream()
            .map(it -> it.projectAware().getSmartProjectReloadDelay())
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(getAutoReloadDelay());
        long dispatcherIterations = smartProjectReloadDelay.toMillis() / getMergingTimeSpan().toMillis();
        // We already dispatched `processChanges` with the `mergingTimeSpan` delay
        // Therefore we need to reduce effective
        int effectiveDispatchIterations = Math.max((int) dispatcherIterations - 1, 1);

        schedule(2, effectiveDispatchIterations, () -> reloadProject(projectsToReload));
    }

    private void schedule(int priority, int dispatchIterations, Runnable action) {
        syncDispatcherOptions();
        myDispatcher.queue(PriorityEatUpdate.create(priority, () -> {
            if (dispatchIterations - 1 > 0) {
                schedule(priority, dispatchIterations - 1, action);
            }
            else {
                action.run();
            }
        }));
    }

    private void syncDispatcherOptions() {
        boolean isPassThrough = !isAsyncChangesProcessing();
        if (myDispatcher.isPassThrough() != isPassThrough) {
            myDispatcher.setPassThrough(isPassThrough);
        }
        Duration mergingTimeSpan = getMergingTimeSpan();
        if (!mergingTimeSpan.equals(myDispatcherMergingTimeSpan)) {
            myDispatcherMergingTimeSpan = mergingTimeSpan;
            myDispatcher.setMergingTimeSpan((int) mergingTimeSpan.toMillis());
        }
    }

    private void processChanges(boolean isExplicitReload) {
        LOG.debug("Process changes (isExplicitReload=" + isExplicitReload + ")");

        List<Pair<ExternalSystemProjectAware, ExternalSystemProjectReloadContext>> projectsToReload = new ArrayList<>();
        for (ProjectData projectData : myProjectDataMap.values()) {
            ProjectReloadContext context = new ProjectReloadContext(
                isExplicitReload,
                !projectData.status().isUpToDate(),
                projectData.settingsTracker().getSettingsContext()
            );
            ExternalSystemProjectId projectId = projectData.projectAware().getProjectId();
            if (projectData.isUpToDate()) {
                LOG.debug(projectId + ": Skip project reload (UpToDate)");
                myNotificationAware.notificationExpire(projectId);
            }
            else if (isDisabledReload(projectData, context)) {
                LOG.debug(projectId + ": Skip project reload (disabled)");
                myNotificationAware.notificationExpire(projectId);
            }
            else if (!isExplicitReload && isDisabledAutoReload(projectData, context)) {
                LOG.debug(projectId + ": Skip project auto-reload (disabled)");
                myNotificationAware.notificationNotify(projectData.projectAware());
            }
            else {
                LOG.debug(projectId + ": Schedule project reload");
                myNotificationAware.notificationExpire(projectId);
                projectsToReload.add(Pair.create(projectData.projectAware(), context));
            }
        }

        if (projectsToReload.isEmpty()) {
            LOG.debug("Skip all project reloads");
        }
        else if (isExplicitReload) {
            reloadProject(projectsToReload);
        }
        else {
            scheduleDelayedProjectReload(projectsToReload);
        }
    }

    private void reloadProject(List<Pair<ExternalSystemProjectAware, ExternalSystemProjectReloadContext>> projectsToReload) {
        LOG.debug("Reload projects");

        myProject.getUIAccess().giveAndWaitIfNeed(() -> { // needed for backward compatibility
            for (Pair<ExternalSystemProjectAware, ExternalSystemProjectReloadContext> pair : projectsToReload) {
                ExternalSystemProjectAware projectAware = pair.getFirst();
                LOG.debug(projectAware.getProjectId() + ": reload project");
                projectAware.reloadProject(pair.getSecond());
            }
        });
    }

    private boolean isDisabledReload(ProjectData projectData, ExternalSystemProjectReloadContext context) {
        ExternalSystemProjectId projectId = projectData.projectAware().getProjectId();

        if (!isEnabledReload()) {
            LOG.debug(projectId + ": Disabled reload (global property)");
            return true;
        }

        if (!projectData.isActivated()) {
            LOG.debug(projectId + ": Disabled reload (activation)");
            return true;
        }

        if (myProjectChangeOperation.isOperationInProgress()) {
            LOG.debug(projectId + ": Disabled reload (project change)");
            return true;
        }

        if (projectData.reloadOperation().isOperationInProgress()) {
            LOG.debug(projectId + ": Disabled reload (project reload)");
            return true;
        }

        if (projectData.projectAware().isDisabledReload(context)) {
            LOG.debug(projectId + ": Disabled reload (custom)");
            return true;
        }

        return false;
    }

    private boolean isDisabledAutoReload(ProjectData projectData, ExternalSystemProjectReloadContext context) {
        ExternalSystemProjectId projectId = projectData.projectAware().getProjectId();

        if (!isEnabledAutoReload()) {
            LOG.debug(projectId + ": Disabled auto-reload (global property)");
            return true;
        }

        if (myIsProjectLookupActive.get()) {
            LOG.debug(projectId + ": Disabled auto-reload (project lookup)");
            return true;
        }

        AutoReloadType autoReloadType = mySettings.getAutoReloadType();
        ExternalSystemModificationType modificationType = projectData.getModificationType();
        boolean isDisabledAutoReload = switch (autoReloadType) {
            case ALL -> switch (modificationType) {
                case EXTERNAL -> false;
                case INTERNAL -> false;
                case HIDDEN -> true;
                case UNKNOWN -> true;
            };
            case SELECTIVE -> switch (modificationType) {
                case EXTERNAL -> false;
                case INTERNAL -> true;
                case HIDDEN -> true;
                case UNKNOWN -> true;
            };
            case NONE -> true;
        };
        if (isDisabledAutoReload) {
            LOG.debug(projectId + ": Disabled auto-reload (" + autoReloadType + ", " + modificationType + ")");
            return true;
        }

        if (projectData.projectAware().isDisabledAutoReload(context)) {
            LOG.debug(projectId + ": Disabled auto-reload (custom)");
            return true;
        }

        return false;
    }

    @Override
    public void register(ExternalSystemProjectAware projectAware) {
        ExternalSystemProjectId projectId = projectAware.getProjectId();
        AtomicOperationTrace reloadOperation = new AtomicOperationTrace("Reload " + projectId);
        AutoImportProjectStatus projectStatus = new AutoImportProjectStatus("[project-tracker] " + projectId);
        Disposable parentDisposable = Disposable.newDisposable(projectId.toString());
        Disposer.register(myServiceDisposable, parentDisposable);
        AutoImportProjectSettingsFilesTracker settingsTracker =
            new AutoImportProjectSettingsFilesTracker(myProject, this, myBackgroundExecutor, projectAware, parentDisposable);
        ProjectData projectData =
            new ProjectData(projectStatus, new AtomicBoolean(false), reloadOperation, projectAware, settingsTracker, parentDisposable);

        myProjectDataMap.put(projectId, projectData);

        settingsTracker.beforeApplyChanges(parentDisposable, reloadOperation::traceStart);
        settingsTracker.afterApplyChanges(parentDisposable, reloadOperation::traceFinish);

        reloadOperation.whenOperationStarted(myServiceDisposable, () -> {
            LOG.debug(projectId + ": Detected project reload start event");
            scheduleChangeProcessing();
        });
        reloadOperation.whenOperationFinished(myServiceDisposable, () -> {
            LOG.debug(projectId + ": Detected project reload finish event");
            scheduleChangeProcessing();
        });

        projectAware.subscribe(createProjectReloadListener(projectData), parentDisposable);
        Disposer.register(parentDisposable, () -> myNotificationAware.notificationExpire(projectId));

        loadState(projectId, projectData);
    }

    @Override
    public void activate(ExternalSystemProjectId id) {
        ProjectData projectData = projectDataMap(id, Map::get);
        if (projectData == null) {
            return;
        }
        activate(projectData);
    }

    private void activate(ProjectData projectData) {
        if (projectData.activated().compareAndSet(false, true)) {
            LOG.debug(projectData.projectAware().getProjectId() + ": Tracker is activated");
            scheduleChangeProcessing();
        }
    }

    @Override
    public void remove(ExternalSystemProjectId id) {
        ProjectData projectData = myProjectDataMap.remove(id);
        if (projectData == null) {
            return;
        }
        Disposer.dispose(projectData.parentDisposable());
    }

    @Override
    public void markDirty(ExternalSystemProjectId id) {
        ProjectData projectData = projectDataMap(id, Map::get);
        if (projectData == null) {
            return;
        }
        projectData.status().markDirty(Stamp.nextStamp());
    }

    @Override
    public void markDirtyAllProjects() {
        Stamp modificationTimeStamp = Stamp.nextStamp();
        for (ProjectData projectData : myProjectDataMap.values()) {
            projectData.status().markDirty(modificationTimeStamp);
        }
    }

    private @Nullable ProjectData projectDataMap(
        ExternalSystemProjectId id,
        BiFunction<Map<ExternalSystemProjectId, ProjectData>, ExternalSystemProjectId, @Nullable ProjectData> action
    ) {
        ProjectData projectData = action.apply(myProjectDataMap, id);
        if (projectData == null) {
            LOG.warn(String.format("Project isn't registered by id=%s", id), new Throwable());
        }
        return projectData;
    }

    @Override
    public AutoImportProjectTrackerState getState() {
        AutoImportProjectTrackerState state = new AutoImportProjectTrackerState();
        for (Map.Entry<ExternalSystemProjectId, ProjectData> entry : myProjectDataMap.entrySet()) {
            ExternalSystemProjectId projectId = entry.getKey();
            ProjectData projectData = entry.getValue();
            AutoImportProjectSystemState systemState =
                state.projectData.computeIfAbsent(projectId.getSystemId().getId(), it -> new AutoImportProjectSystemState());
            AutoImportProjectDataState projectState = new AutoImportProjectDataState();
            projectState.isDirty = projectData.status().isDirty();
            projectState.settingsTracker = projectData.settingsTracker().getState();
            LOG.debug(projectId + ": Store State (" + projectState + ")");
            systemState.projects.put(projectId.getExternalProjectPath(), projectState);
        }
        return state;
    }

    @Override
    public void loadState(AutoImportProjectTrackerState state) {
        for (Map.Entry<String, AutoImportProjectSystemState> systemEntry : state.projectData.entrySet()) {
            for (Map.Entry<String, AutoImportProjectDataState> projectEntry : systemEntry.getValue().projects.entrySet()) {
                myProjectDataStates.put(new StateKey(systemEntry.getKey(), projectEntry.getKey()), projectEntry.getValue());
            }
        }
        myProjectDataMap.forEach(this::loadState);
    }

    private void loadState(ExternalSystemProjectId projectId, ProjectData projectData) {
        AutoImportProjectDataState projectState =
            myProjectDataStates.remove(new StateKey(projectId.getSystemId().getId(), projectId.getExternalProjectPath()));
        LOG.debug(projectId + ": Load State (" + projectState + ")");
        AutoImportProjectSettingsFilesTrackerState settingsTrackerState = projectState == null ? null : projectState.settingsTracker;
        if (settingsTrackerState == null || projectState.isDirty) {
            projectData.status().markDirty(Stamp.nextStamp(), ExternalSystemModificationType.EXTERNAL);
            scheduleChangeProcessing();
            return;
        }
        projectData.settingsTracker().loadState(settingsTrackerState);
        projectData.settingsTracker().refreshChanges();
    }

    public Set<ExternalSystemProjectId> getActivatedProjects() {
        Set<ExternalSystemProjectId> result = new HashSet<>();
        for (ProjectData projectData : myProjectDataMap.values()) {
            if (projectData.isActivated()) {
                result.add(projectData.projectAware().getProjectId());
            }
        }
        return result;
    }

    @Override
    public void dispose() {
    }

    private record ProjectData(
        AutoImportProjectStatus status,
        AtomicBoolean activated,
        AtomicOperationTrace reloadOperation,
        ExternalSystemProjectAware projectAware,
        AutoImportProjectSettingsFilesTracker settingsTracker,
        Disposable parentDisposable
    ) {
        boolean isActivated() {
            return activated.get();
        }

        boolean isUpToDate() {
            return status.isUpToDate() && settingsTracker.isUpToDate();
        }

        ExternalSystemModificationType getModificationType() {
            return AutoImportProjectStatus.merge(status.getModificationType(), settingsTracker.getModificationType());
        }
    }

    private record StateKey(String systemId, String externalProjectPath) {
    }

    private record ProjectReloadContext(
        boolean isExplicitReload,
        boolean hasUndefinedModifications,
        ExternalSystemSettingsFilesReloadContext settingsFilesContext
    ) implements ExternalSystemProjectReloadContext {
        @Override
        public ExternalSystemSettingsFilesReloadContext getSettingsFilesContext() {
            return settingsFilesContext;
        }
    }

    private static boolean isEnabledReload() {
        return ourEnableReload &&
            (ourOnceIgnoreDisableReloadRegistry.getAndSet(false) ||
                !Boolean.getBoolean("external.system.auto.import.disabled"));
    }

    private static boolean isEnabledAutoReload() {
        return isEnabledReload() && ourEnableAutoReload;
    }

    public static boolean isAsyncChangesProcessing() {
        return ourAsyncChangesProcessing;
    }

    private static Duration getMergingTimeSpan() {
        return ourMergingTimeSpan;
    }

    private static Duration getAutoReloadDelay() {
        return ourAutoReloadDelay;
    }

    /**
     * In tests, enables only manual syncs executed using the {@link ExternalSystemProjectTracker} API.
     * <p>
     * Note: auto-sync still will be disabled.
     */
    public static void enableReloadInTests(Disposable parentDisposable) {
        boolean oldEnableReload = ourEnableReload;
        ourEnableReload = true;
        Disposer.register(parentDisposable, () -> ourEnableReload = oldEnableReload);
    }

    /**
     * In tests, enables all syncs executed using the {@link ExternalSystemProjectTracker} API.
     * <p>
     * Note: manual syncs will be also enabled.
     */
    public static void enableAutoReloadInTests(Disposable parentDisposable) {
        enableReloadInTests(parentDisposable);
        boolean oldEnableAutoReload = ourEnableAutoReload;
        ourEnableAutoReload = true;
        Disposer.register(parentDisposable, () -> ourEnableAutoReload = oldEnableAutoReload);
    }

    /**
     * In tests, enables async project status processing.
     */
    public static void enableAsyncAutoReloadInTests(Disposable parentDisposable) {
        boolean oldAsyncChangesProcessing = ourAsyncChangesProcessing;
        ourAsyncChangesProcessing = true;
        Disposer.register(parentDisposable, () -> ourAsyncChangesProcessing = oldAsyncChangesProcessing);
    }

    /**
     * Ignores once disable auto-reload registry.
     * Make sense only in a pair with system property {@code external.system.auto.import.disabled}.
     */
    public static void onceIgnoreDisableAutoReloadRegistry() {
        ourOnceIgnoreDisableReloadRegistry.set(true);
    }

    /**
     * Defines the delay minimum project tracker delay.
     * <p>
     * All project changes and sync requests that were made during this delay rescheduled it.
     * So, it allows us to merge all project changes and sync requests into one bulk sync request.
     * So, it allows us to start syncing automatically only once.
     */
    public static void setMergingTimeSpan(Duration delay, Disposable parentDisposable) {
        Duration oldMergingTimeSpan = ourMergingTimeSpan;
        ourMergingTimeSpan = delay;
        Disposer.register(parentDisposable, () -> ourMergingTimeSpan = oldMergingTimeSpan);
    }

    /**
     * Defines the delay from the detected project change to the start of auto-sync.
     *
     * @see #setMergingTimeSpan
     */
    public static void setAutoReloadDelay(Duration delay, Disposable parentDisposable) {
        Duration oldAutoReloadDelay = ourAutoReloadDelay;
        ourAutoReloadDelay = delay;
        Disposer.register(parentDisposable, () -> ourAutoReloadDelay = oldAutoReloadDelay);
    }
}
