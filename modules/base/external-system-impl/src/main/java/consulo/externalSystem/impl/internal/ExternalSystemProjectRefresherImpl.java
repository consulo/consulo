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
package consulo.externalSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.importing.ImportSpecBuilder;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.externalSystem.service.project.ExternalProjectRefreshCallback;
import consulo.externalSystem.service.project.ExternalSystemProjectRefresher;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-04-10
 */
@ServiceImpl
@Singleton
public class ExternalSystemProjectRefresherImpl implements ExternalSystemProjectRefresher {
    @Override
    public void refreshProject(@Nonnull Project project,
                               @Nonnull ProjectSystemId externalSystemId,
                               @Nonnull String externalProjectPath,
                               @Nonnull ExternalProjectRefreshCallback callback,
                               boolean isPreviewMode,
                               @Nonnull ProgressExecutionMode progressExecutionMode,
                               boolean reportRefreshError) {
        ExternalSystemUtil.refreshProject(project, externalSystemId, externalProjectPath, callback, isPreviewMode, progressExecutionMode, reportRefreshError);
    }

    @Override
    public void refreshProjects(@Nonnull ImportSpecBuilder specBuilder) {
        ExternalSystemUtil.refreshProjects(specBuilder);
    }
}
