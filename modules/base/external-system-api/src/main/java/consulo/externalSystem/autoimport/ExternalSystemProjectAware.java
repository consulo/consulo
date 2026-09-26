// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.autoimport;

import consulo.disposer.Disposable;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Set;

public interface ExternalSystemProjectAware {
    ExternalSystemProjectId getProjectId();

    /**
     * Collects settings files which will be watched.
     * These property is read each time after {@link #reloadProject} gets called, new file was added
     * or {@link ExternalSystemProjectListener#onSettingsFilesListChange} event happened.
     */
    Set<String> getSettingsFiles();

    void subscribe(ExternalSystemProjectListener listener, Disposable parentDisposable);

    /**
     * Schedules build tool's project reload.
     * <p>
     * Note: Build tool sync shouldn't block current thread,
     * because it is used to dispatch all other reloads in the IDE project.
     */
    void reloadProject(ExternalSystemProjectReloadContext context);

    /**
     * Internal. Please see implementation limitations.
     * <p>
     * This property defines a delay for the "smart" project sync request.
     * Usually, the "smart" sync is requested after changes in the {@link #getSettingsFiles()}.
     * <p>
     * Note: All external systems sync events are dispatched and merged by the same merging update queue.
     * Therefore, this value can be overridden by the greater {@link #getSmartProjectReloadDelay()}.
     * For example, between default (3 seconds) and zero delays will be chosen the default delay.
     * <p>
     * A null value means default {@link #getSmartProjectReloadDelay()} that is equals to 3 seconds.
     */
    default @Nullable Duration getSmartProjectReloadDelay() {
        return null;
    }

    /**
     * Experimental. Please see implementation limitations.
     * <p>
     * This function allows ignoring settings files events. For example Idea can ignore external
     * changes during reload.
     * <p>
     * Note: All ignored modifications cannot be reverted. So if we ignore only create events
     * then if settings file was created and removed then we mark project status as modified,
     * because we can restore only delete event by CRCs.
     * <p>
     * Note: Now create event and register settings file (file appear in settings files list) event
     * is same (also for delete and unregister), because we cannot find settings file if it doesn't
     * exist in file system. Usually settings files list forms during file system scanning.
     * <p>
     * Note: This function will be called on EDT. Please make only trivial checks like:
     * {@code context.getModificationType() == EXTERNAL && path.endsWith(".my-ext")}
     * <p>
     * Note: {@link ExternalSystemSettingsFilesModificationContext.ReloadStatus#JUST_FINISHED} is used to ignore create events
     * during reload. But we cannot replace it by {@link ExternalSystemSettingsFilesModificationContext.ReloadStatus#IN_PROGRESS},
     * because we should merge create and all next update events into one create event and ignore all of them.
     * So {@link ExternalSystemSettingsFilesModificationContext.ReloadStatus#JUST_FINISHED} true only at the end of reload.
     */
    default boolean isIgnoredSettingsFileEvent(String path, ExternalSystemSettingsFilesModificationContext context) {
        ExternalSystemSettingsFilesModificationContext.ReloadStatus reloadStatus = context.getReloadStatus();
        return reloadStatus == ExternalSystemSettingsFilesModificationContext.ReloadStatus.JUST_STARTED
            || reloadStatus == ExternalSystemSettingsFilesModificationContext.ReloadStatus.JUST_FINISHED
            && context.getEvent() == ExternalSystemSettingsFilesModificationContext.Event.CREATE;
    }

    /**
     * Experimental. Please see implementation limitations.
     * <p>
     * This function allows adjusting modification type of the modified file. For example, Idea can change
     * {@link ExternalSystemModificationType#INTERNAL} to {@link ExternalSystemModificationType#HIDDEN} to skip auto reloading.
     * <p>
     * Note: This function will be called on EDT. Please make only trivial checks like:
     * {@code modificationType == INTERNAL && path.endsWith(".hidden")}
     */
    default ExternalSystemModificationType adjustModificationType(String path, ExternalSystemModificationType modificationType) {
        return modificationType;
    }

    /**
     * Experimental. Please see implementation limitations.
     * <p>
     * This function allows to disable all project syncs for a build tool project.
     * <p>
     * Note: It disables auto-syncs and manual syncs that started by a build tool integration and by explicit user actions.
     * For example, if project caches invalidated, if settings files are changed or from the editor floating toolbar.
     * <p>
     * Note: The auto-sync will be automatically re-scheduled in several cases like auto-sync settings changed,
     * project settings files changed, etc. However, it can be re-scheduled after any custom event using
     * the {@link ExternalSystemProjectTracker#scheduleChangeProcessing} function.
     */
    default boolean isDisabledReload(ExternalSystemProjectReloadContext context) {
        return false;
    }

    /**
     * Experimental. Please see implementation limitations.
     * <p>
     * This function allows to disable only project auto syncs for a build tool project.
     * <p>
     * Note: It disables auto-syncs that started by a build tool integration.
     * For example, if project caches invalidated or if settings files are changed.
     * <p>
     * Note: The auto-sync will be automatically re-scheduled in several cases like auto-sync settings changed,
     * project settings files changed, etc. However, it can be re-scheduled after any custom event using
     * the {@link ExternalSystemProjectTracker#scheduleChangeProcessing} function.
     */
    default boolean isDisabledAutoReload(ExternalSystemProjectReloadContext context) {
        return false;
    }
}
