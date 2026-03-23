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
package consulo.externalSystem.impl.internal.view;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.project.*;
import consulo.externalSystem.model.task.TaskData;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.Order;
import consulo.externalSystem.view.*;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.collection.MultiMap;
import consulo.util.collection.SmartList;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Default contributor for the external system project tree.
 * Creates nodes for modules, tasks, and dependencies.
 *
 * @author Vladislav.Soroka
 */
@ExtensionImpl
public class ExternalSystemViewDefaultContributor extends ExternalSystemViewContributor {

    private static final Key<?>[] KEYS = new Key<?>[]{
        ProjectKeys.MODULE,
        ProjectKeys.MODULE_DEPENDENCY,
        ProjectKeys.LIBRARY_DEPENDENCY,
        ProjectKeys.TASK
    };

    @Override
    public ProjectSystemId getSystemId() {
        return ProjectSystemId.IDE;
    }

    @Override
    public List<Key<?>> getKeys() {
        return Arrays.asList(KEYS);
    }

    @Override
    @Nullable
    public String getDisplayName(DataNode<?> node) {
        Object data = node.getData();
        if (data instanceof ProjectData projectData) {
            return projectData.getExternalName();
        }
        if (data instanceof ModuleData moduleData) {
            return moduleData.getExternalName();
        }
        if (data instanceof TaskData taskData) {
            return taskData.getName();
        }
        if (data instanceof ModuleDependencyData depData) {
            return depData.getExternalName();
        }
        if (data instanceof LibraryDependencyData libData) {
            return libData.getExternalName();
        }
        return null;
    }

    @Override
    public List<ExternalSystemNode<?>> createNodes(ExternalProjectsView externalProjectsView,
                                                   MultiMap<Key<?>, DataNode<?>> dataNodes) {
        final List<ExternalSystemNode<?>> result = new SmartList<>();

        addModuleNodes(externalProjectsView, dataNodes, result);

        // add tasks
        Collection<DataNode<?>> tasksNodes = dataNodes.get(ProjectKeys.TASK);
        if (!tasksNodes.isEmpty()) {
            result.add(new TasksNode(externalProjectsView, tasksNodes));
        }

        addDependenciesNode(externalProjectsView, dataNodes, result);

        return result;
    }

    private static void addModuleNodes(ExternalProjectsView externalProjectsView,
                                       MultiMap<Key<?>, DataNode<?>> dataNodes,
                                       List<? super ExternalSystemNode<?>> result) {
        final Collection<DataNode<?>> moduleDataNodes = dataNodes.get(ProjectKeys.MODULE);
        if (moduleDataNodes.isEmpty()) return;

        final AbstractExternalSystemSettings<?, ?, ?> systemSettings;
        try {
            systemSettings = ExternalSystemApiUtil.getSettings(
                externalProjectsView.getProject(), externalProjectsView.getSystemId());
        }
        catch (IllegalArgumentException e) {
            return;
        }

        final Map<String, ModuleNode> groupToModule = new HashMap<>(moduleDataNodes.size());
        List<ModuleNode> moduleNodes = new ArrayList<>();

        for (DataNode<?> dataNode : moduleDataNodes) {
            if (!(dataNode.getData() instanceof ModuleData data)) continue;

            ExternalProjectSettings projectSettings =
                systemSettings.getLinkedProjectSettings(data.getLinkedExternalProjectPath());
            DataNode<ProjectData> projectDataNode =
                ExternalSystemApiUtil.findParent(dataNode, ProjectKeys.PROJECT);

            final boolean isRoot = projectSettings != null
                && data.getLinkedExternalProjectPath().equals(projectSettings.getExternalProjectPath())
                && projectDataNode != null
                && projectDataNode.getData().getInternalName().equals(data.getInternalName());

            //noinspection unchecked
            final ModuleNode moduleNode = new ModuleNode(externalProjectsView,
                (DataNode<ModuleData>) dataNode, null, isRoot);
            moduleNodes.add(moduleNode);

            String group = moduleNode.getIdeGrouping();
            if (group != null) {
                groupToModule.put(group, moduleNode);
            }
        }

        for (ModuleNode moduleNode : moduleNodes) {
            moduleNode.setAllModules(moduleNodes);
            String parentGroup = moduleNode.getIdeParentGrouping();
            ModuleNode parent = parentGroup != null ? groupToModule.get(parentGroup) : null;
            if (parent == null) continue;
            moduleNode.setParent(parent);
        }

        result.addAll(moduleNodes);
    }

    private static void addDependenciesNode(ExternalProjectsView externalProjectsView,
                                            MultiMap<Key<?>, DataNode<?>> dataNodes,
                                            List<? super ExternalSystemNode<?>> result) {
        final Collection<DataNode<?>> moduleDeps = dataNodes.get(ProjectKeys.MODULE_DEPENDENCY);
        final Collection<DataNode<?>> libDeps = dataNodes.get(ProjectKeys.LIBRARY_DEPENDENCY);

        if (moduleDeps.isEmpty() && libDeps.isEmpty()) return;

        List<ExternalSystemNode<?>> depNodeChildren = new ArrayList<>();

        for (DataNode<?> dataNode : moduleDeps) {
            if (!(dataNode.getData() instanceof ModuleDependencyData)) continue;
            //noinspection unchecked
            ExternalSystemNode<ModuleDependencyData> node =
                new ModuleDependencyNode(externalProjectsView, (DataNode<ModuleDependencyData>) dataNode);
            if (dataNode.getParent() != null && dataNode.getParent().getData() instanceof AbstractDependencyData) {
                result.add(node);
            }
            else {
                depNodeChildren.add(node);
            }
        }

        for (DataNode<?> dataNode : libDeps) {
            if (!(dataNode.getData() instanceof LibraryDependencyData libDepData)) continue;
            //noinspection unchecked
            LibraryDependencyNode libNode =
                new LibraryDependencyNode(externalProjectsView, (DataNode<LibraryDependencyData>) dataNode);
            if (libDepData.getTarget().isUnresolved()) {
                libNode.setErrorLevel(ExternalProjectsStructure.ErrorLevel.ERROR,
                    "Unable to resolve " + libDepData.getTarget().getExternalName());
            }
            if (dataNode.getParent() != null && dataNode.getParent().getData() instanceof ModuleData) {
                depNodeChildren.add(libNode);
            }
            else {
                result.add(libNode);
            }
        }

        if (!depNodeChildren.isEmpty()) {
            DependenciesNode depNode = new DependenciesNode(externalProjectsView);
            depNode.addAll(depNodeChildren);
            result.add(depNode);
        }
    }

    // ---- Inner node types ----

    @Order(ExternalSystemNode.BUILTIN_DEPENDENCIES_DATA_NODE_ORDER)
    private static class DependenciesNode extends ExternalSystemNode<Object> {
        DependenciesNode(ExternalProjectsView externalProjectsView) {
            super(externalProjectsView, null, null);
        }

        @Override
        public String getName() {
            return "Dependencies";
        }

        @Override
        protected void update(PresentationData presentation) {
            super.update(presentation);
            presentation.setIcon(PlatformIconGroup.nodesConfigfolder());
        }
    }

    private static class ModuleDependencyNode extends ExternalSystemNode<ModuleDependencyData> {
        ModuleDependencyNode(ExternalProjectsView view, DataNode<ModuleDependencyData> node) {
            super(view, null, node);
        }

        @Override
        public String getName() {
            ModuleDependencyData data = getData();
            return data != null ? data.getExternalName() : "";
        }

        @Override
        protected void update(PresentationData presentation) {
            super.update(presentation);
            presentation.setIcon(PlatformIconGroup.nodesModule());
            ModuleDependencyData data = getData();
            if (data != null) {
                String scope = data.getScope() != null ? data.getScope().getDisplayName() : null;
                setNameAndTooltip(presentation, getName(), null, scope);
            }
        }

        @Override
        public boolean isAlwaysLeaf() {
            return true;
        }
    }

    private static class LibraryDependencyNode extends ExternalSystemNode<LibraryDependencyData> {
        LibraryDependencyNode(ExternalProjectsView view, DataNode<LibraryDependencyData> node) {
            super(view, null, node);
        }

        @Override
        public String getName() {
            LibraryDependencyData data = getData();
            return data != null ? data.getExternalName() : "";
        }

        @Override
        protected void update(PresentationData presentation) {
            super.update(presentation);
            presentation.setIcon(PlatformIconGroup.nodesPplib());
            LibraryDependencyData data = getData();
            if (data != null) {
                String scope = data.getScope() != null ? data.getScope().getDisplayName() : null;
                String tooltip = null;
                if (data.getTarget().isUnresolved()) {
                    tooltip = "Unable to resolve: " + data.getTarget().getExternalName();
                }
                else if (!StringUtil.isEmptyOrSpaces(data.getTarget().getExternalName())) {
                    tooltip = data.getTarget().getExternalName();
                }
                setNameAndTooltip(presentation, getName(), tooltip, scope);
            }
        }

        @Override
        public boolean isAlwaysLeaf() {
            return true;
        }

        @Override
        @Nullable
        protected String getMenuId() {
            return "ExternalSystemView.LibraryDependencyMenu";
        }
    }
}
