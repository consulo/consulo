/*
 * Copyright 2013-2025 consulo.io
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
package consulo.externalSystem.service.project;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.externalSystem.importing.ImportSpecBuilder;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-04-10
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ExternalSystemProjectRefresher {
    static ExternalSystemProjectRefresher getInstance() {
        return Application.get().getInstance(ExternalSystemProjectRefresher.class);
    }

    /**
     * Asks to refresh all external projects of the target external system linked to the given ide project.
     * <p>
     * 'Refresh' here means 'obtain the most up-to-date version and apply it to the ide'.
     *
     * @param project          target ide project
     * @param externalSystemId target external system which projects should be refreshed
     * @param force            flag which defines if external project refresh should be performed if it's config is up-to-date
     * @deprecated use {@link  ExternalSystemUtil#refreshProjects(ImportSpecBuilder)}
     */
    @Deprecated
    default void refreshProjects(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId, boolean force) {
        refreshProjects(project, externalSystemId, force, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
    }

    /**
     * Asks to refresh all external projects of the target external system linked to the given ide project.
     * <p>
     * 'Refresh' here means 'obtain the most up-to-date version and apply it to the ide'.
     *
     * @param project          target ide project
     * @param externalSystemId target external system which projects should be refreshed
     * @param force            flag which defines if external project refresh should be performed if it's config is up-to-date
     * @deprecated use {@link  ExternalSystemUtil#refreshProjects(ImportSpecBuilder)}
     */
    @Deprecated
    default void refreshProjects(
        @Nonnull Project project,
        @Nonnull ProjectSystemId externalSystemId,
        boolean force,
        @Nonnull ProgressExecutionMode progressExecutionMode
    ) {
        refreshProjects(new ImportSpecBuilder(project, externalSystemId).forceWhenUptodate(force).use(progressExecutionMode));
    }

    /**
     * TODO[Vlad]: refactor the method to use {@link ImportSpecBuilder}
     * <p>
     * Queries slave gradle process to refresh target gradle project.
     *
     * @param project             target intellij project to use
     * @param externalProjectPath path of the target gradle project's file
     * @param callback            callback to be notified on refresh result
     * @param isPreviewMode       flag that identifies whether gradle libraries should be resolved during the refresh
     * @param reportRefreshError  prevent to show annoying error notification, e.g. if auto-import mode used
     */
    void refreshProject(@Nonnull Project project,
                        @Nonnull ProjectSystemId externalSystemId,
                        @Nonnull String externalProjectPath,
                        @Nonnull ExternalProjectRefreshCallback callback,
                        boolean isPreviewMode,
                        @Nonnull ProgressExecutionMode progressExecutionMode,
                        boolean reportRefreshError);

    /**
     * TODO[Vlad]: refactor the method to use {@link ImportSpecBuilder}
     * <p>
     * Queries slave gradle process to refresh target gradle project.
     *
     * @param project             target intellij project to use
     * @param externalProjectPath path of the target gradle project's file
     * @param callback            callback to be notified on refresh result
     * @param isPreviewMode       flag that identifies whether gradle libraries should be resolved during the refresh
     * @return the most up-to-date gradle project (if any)
     */
    default void refreshProject(
        @Nonnull Project project,
        @Nonnull ProjectSystemId externalSystemId,
        @Nonnull String externalProjectPath,
        @Nonnull ExternalProjectRefreshCallback callback,
        boolean isPreviewMode,
        @Nonnull ProgressExecutionMode progressExecutionMode
    ) {
        refreshProject(project, externalSystemId, externalProjectPath, callback, isPreviewMode, progressExecutionMode, true);
    }

    void refreshProjects(@Nonnull ImportSpecBuilder specBuilder);
}
