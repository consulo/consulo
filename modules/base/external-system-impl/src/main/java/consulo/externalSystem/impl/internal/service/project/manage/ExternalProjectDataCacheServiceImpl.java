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
package consulo.externalSystem.impl.internal.service.project.manage;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.project.Project;

import java.util.Collection;

/**
 * Caches top-level {@link DataNode}{@code <}{@link ProjectData}{@code >} nodes so that
 * {@link ExternalProjectsManagerImpl#getProjectData} can supply them to the view on demand.
 *
 * @author Vladislav.Soroka
 */
@ExtensionImpl
public class ExternalProjectDataCacheServiceImpl implements ProjectDataService<ProjectData, Void> {
    @Override
    public Key<ProjectData> getTargetDataKey() {
        return ProjectKeys.PROJECT;
    }

    @Override
    public void importData(Collection<DataNode<ProjectData>> toImport,
                           Project project,
                           boolean synchronous) {
        ExternalProjectsManagerImpl manager = ExternalProjectsManagerImpl.getInstance(project);
        for (DataNode<ProjectData> node : toImport) {
            manager.onProjectImported(node);
        }
    }

    @Override
    public void removeData(Collection<? extends Void> toRemove,
                           Project project,
                           boolean synchronous) {
        // nothing to remove — cache is replaced on next import
    }
}
