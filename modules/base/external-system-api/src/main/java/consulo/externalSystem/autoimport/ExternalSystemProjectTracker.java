// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.autoimport;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;

@ServiceAPI(ComponentScope.PROJECT)
public interface ExternalSystemProjectTracker {
    static ExternalSystemProjectTracker getInstance(Project project) {
        return project.getInstance(ExternalSystemProjectTracker.class);
    }

    /**
     * Starts tracking of project settings that will be defined by {@code projectAware}
     * <p>
     * Auto reloads will be activated after first project refresh
     * (i.e. after first {@link ExternalSystemProjectListener#onProjectReloadStart} / {@link ExternalSystemProjectListener#onProjectReloadFinish})
     *
     * @see #activate for details
     */
    void register(ExternalSystemProjectAware projectAware);

    /**
     * @param parentDisposable allows to remove {@code projectAware} when it will be disposed
     * @see #register(ExternalSystemProjectAware)
     */
    default void register(ExternalSystemProjectAware projectAware, Disposable parentDisposable) {
        register(projectAware);
        Disposer.register(parentDisposable, () -> remove(projectAware.getProjectId()));
    }

    /**
     * Activates auto reload for project with {@code id}
     * <p>
     * Allows to detect project that loaded from local cashes but previously didn't register here
     */
    void activate(ExternalSystemProjectId id);

    /**
     * Stops tracking of project settings that were defined by {@link ExternalSystemProjectAware} with {@code id}
     */
    void remove(ExternalSystemProjectId id);

    /**
     * Marks project settings as dirty.
     */
    void markDirty(ExternalSystemProjectId id);

    /**
     * Marks all external project settings as dirty
     *
     * @see #markDirty(ExternalSystemProjectId)
     */
    void markDirtyAllProjects();

    /**
     * Schedules project reload, may be skipped if project is up-to-date, project is being reloaded or VCS is being updated.
     * Use {@link #markDirtyAllProjects} for force project reload.
     */
    void scheduleProjectRefresh();

    /**
     * Schedules project reload or notification update.
     * I.e. marks this place as safe to start auto-reload.
     *
     * @see #scheduleProjectRefresh
     */
    void scheduleChangeProcessing();
}
