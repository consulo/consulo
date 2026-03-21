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
package consulo.externalSystem.view;

import consulo.externalSystem.ui.ExternalSystemUiAware;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.manage.ExternalSystemShortcutsManager;
import consulo.externalSystem.service.project.manage.ExternalSystemTaskActivator;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.awt.event.InputEvent;
import java.util.List;

/**
 * Interface for the external projects tool window view.
 * Provides access to view state, UI customization, and node creation.
 *
 * @author Vladislav.Soroka
 */
public interface ExternalProjectsView {
   
    Project getProject();

   
    ExternalSystemUiAware getUiAware();

    @Nullable
    ExternalProjectsStructure getStructure();

   
    ExternalSystemShortcutsManager getShortcutsManager();

   
    ExternalSystemTaskActivator getTaskActivator();

   
    ProjectSystemId getSystemId();

    void updateUpTo(ExternalSystemNode<?> node);

   
    List<ExternalSystemNode<?>> createNodes(ExternalProjectsView view,
                                            @Nullable ExternalSystemNode<?> parent,
                                            DataNode<?> dataNode);

   
    ExternalProjectsStructure.ErrorLevel getErrorLevelRecursively(DataNode<?> node);

    boolean getShowIgnored();

    boolean getGroupTasks();

    boolean getGroupModules();

    boolean showInheritedTasks();

    @Nullable
    String getDisplayName(@Nullable DataNode<?> node);

    void handleDoubleClickOrEnter(ExternalSystemNode<?> node,
                                  @Nullable String actionId,
                                  InputEvent inputEvent);

    void addListener(Listener listener);

    /**
     * Schedule a full update of the tree structure from backing data.
     */
    void scheduleStructureUpdate();

    interface Listener {
        void onDoubleClickOrEnter(ExternalSystemNode<?> node, InputEvent inputEvent);
    }
}
