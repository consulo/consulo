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
 * Decorator that delegates all calls to a wrapped {@link ExternalProjectsView}.
 * Subclasses may override individual methods to add behaviour.
 *
 * @author Vladislav.Soroka
 */
public class ExternalProjectsViewAdapter implements ExternalProjectsView {
    private final ExternalProjectsView myDelegate;

    public ExternalProjectsViewAdapter(ExternalProjectsView delegate) {
        myDelegate = delegate;
    }

    @Override
   
    public Project getProject() {
        return myDelegate.getProject();
    }

    @Override
   
    public ExternalSystemUiAware getUiAware() {
        return myDelegate.getUiAware();
    }

    @Override
    @Nullable
    public ExternalProjectsStructure getStructure() {
        return myDelegate.getStructure();
    }

    @Override
   
    public ExternalSystemShortcutsManager getShortcutsManager() {
        return myDelegate.getShortcutsManager();
    }

    @Override
   
    public ExternalSystemTaskActivator getTaskActivator() {
        return myDelegate.getTaskActivator();
    }

    @Override
   
    public ProjectSystemId getSystemId() {
        return myDelegate.getSystemId();
    }

    @Override
    public void updateUpTo(ExternalSystemNode<?> node) {
        myDelegate.updateUpTo(node);
    }

    @Override
   
    public List<ExternalSystemNode<?>> createNodes(ExternalProjectsView view,
                                                   @Nullable ExternalSystemNode<?> parent,
                                                   DataNode<?> dataNode) {
        return myDelegate.createNodes(view, parent, dataNode);
    }

    @Override
   
    public ExternalProjectsStructure.ErrorLevel getErrorLevelRecursively(DataNode<?> node) {
        return myDelegate.getErrorLevelRecursively(node);
    }

    @Override
    public boolean getShowIgnored() {
        return myDelegate.getShowIgnored();
    }

    @Override
    public boolean getGroupTasks() {
        return myDelegate.getGroupTasks();
    }

    @Override
    public boolean getGroupModules() {
        return myDelegate.getGroupModules();
    }

    @Override
    public boolean showInheritedTasks() {
        return myDelegate.showInheritedTasks();
    }

    @Override
    @Nullable
    public String getDisplayName(@Nullable DataNode<?> node) {
        return myDelegate.getDisplayName(node);
    }

    @Override
    public void handleDoubleClickOrEnter(ExternalSystemNode<?> node,
                                         @Nullable String actionId,
                                         InputEvent inputEvent) {
        myDelegate.handleDoubleClickOrEnter(node, actionId, inputEvent);
    }

    @Override
    public void addListener(Listener listener) {
        myDelegate.addListener(listener);
    }

    @Override
    public void scheduleStructureUpdate() {
        myDelegate.scheduleStructureUpdate();
    }
}
