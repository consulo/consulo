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
package consulo.externalSystem.service.project.manage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.view.ExternalProjectsView;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Central manager for external projects UI lifecycle.
 * Owns shortcut and task activator services, manages registered views per system.
 *
 * @author Vladislav.Soroka
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ExternalProjectsManager {
    static ExternalProjectsManager getInstance(Project project) {
        return project.getInstance(ExternalProjectsManager.class);
    }

    Project getProject();

    /**
     * Run the given action on EDT after initialization is complete.
     * If already initialized, runs immediately on EDT.
     */
    void runWhenInitialized(Runnable runnable);

    ExternalSystemShortcutsManager getShortcutsManager();

    ExternalSystemTaskActivator getTaskActivator();

    void registerView(ProjectSystemId systemId, ExternalProjectsView view);

    @Nullable
    ExternalProjectsView getView(ProjectSystemId systemId);

    boolean isIgnored(DataNode<?> dataNode);

    void setIgnored(DataNode<?> dataNode, boolean ignored);

    /**
     * Returns cached project data nodes for the given system.
     */
    Collection<DataNode<ProjectData>> getProjectData(ProjectSystemId systemId);
}
