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

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataSink;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.action.Location;
import consulo.execution.event.RunManagerListener;
import consulo.execution.event.RunManagerListenerEvent;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.externalSystem.model.task.TaskData;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.project.manage.ExternalProjectsManager;
import consulo.externalSystem.service.project.manage.ExternalSystemShortcutsManager;
import consulo.externalSystem.service.project.manage.ExternalSystemTaskActivator;
import consulo.externalSystem.task.ExternalSystemTaskLocation;
import consulo.externalSystem.ui.ExternalSystemUiAware;
import consulo.externalSystem.ui.awt.ExternalSystemUiUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.SimpleToolWindowPanel;
import consulo.ui.ex.awt.tree.SimpleTree;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.collection.MultiMap;
import consulo.util.collection.SmartList;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * Tool window panel for an external system. Replaces the old {@link consulo.externalSystem.ui.awt.ExternalSystemTasksPanel}.
 *
 * @author Vladislav.Soroka
 */
public class ExternalProjectsViewImpl extends SimpleToolWindowPanel implements ExternalProjectsView {

    private final Disposable myParentDisposable;
    private final Project myProject;
    private final ExternalProjectsManager myProjectsManager;
    private final ToolWindow myToolWindow;
    private final ProjectSystemId myExternalSystemId;
    private final ExternalSystemUiAware myUiAware;
    private final Set<Listener> myListeners = new HashSet<>();

    @Nullable
    private ExternalProjectsStructure myStructure;
    private SimpleTree myTree;

    private final List<ExternalSystemViewContributor> myViewContributors;

    private ExternalProjectsViewState myState = new ExternalProjectsViewState();

    @RequiredUIAccess
    public ExternalProjectsViewImpl(Disposable parentDisposable,
                                    Project project,
                                    ToolWindow toolWindow,
                                    ProjectSystemId externalSystemId) {
        super(true, true);
        myParentDisposable = parentDisposable;
        myProject = project;
        myToolWindow = toolWindow;
        myExternalSystemId = externalSystemId;
        myUiAware = ExternalSystemUiUtil.getUiAware(externalSystemId);
        myProjectsManager = ExternalProjectsManager.getInstance(project);

        // System-specific contributors must come before the IDE (catch-all) contributor
        // so their getDisplayName() overrides take priority.
        List<ExternalSystemViewContributor> allContributors =
            project.getApplication().getExtensionList(ExternalSystemViewContributor.class);
        myViewContributors = new ArrayList<>();
        for (ExternalSystemViewContributor c : allContributors) {
            if (myExternalSystemId.equals(c.getSystemId())) myViewContributors.add(c);
        }
        for (ExternalSystemViewContributor c : allContributors) {
            if (ProjectSystemId.IDE.equals(c.getSystemId())) myViewContributors.add(c);
        }

        Disposer.register(parentDisposable, () -> {
            myListeners.clear();
            myViewContributors.clear();
            myStructure = null;
            myTree = null;
        });
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        super.uiDataSnapshot(sink);
        sink.set(ExternalSystemDataKeys.VIEW, this);
        sink.set(CommonDataKeys.PROJECT, myProject);
        sink.set(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID, myExternalSystemId);
        sink.set(ExternalSystemDataKeys.UI_AWARE, myUiAware);
        sink.set(ExternalSystemDataKeys.PROJECTS_TREE, myTree);
        //sink.set(ExternalSystemDataKeys.NOTIFICATION_GROUP, myNotificationGroup);

        //noinspection rawtypes
        List<ExternalSystemNode> selection = getSelectedNodes(ExternalSystemNode.class);
        ProjectNode projectNode = ObjectUtil.tryCast(ContainerUtil.getOnlyItem(selection), ProjectNode.class);

        sink.set(ExternalSystemDataKeys.SELECTED_NODES, selection);
        sink.set(ExternalSystemDataKeys.SELECTED_PROJECT_NODE, projectNode);

        sink.set(CommonDataKeys.VIRTUAL_FILE, selection.isEmpty() ? null : selection.get(0).getVirtualFile());
        sink.set(CommonDataKeys.VIRTUAL_FILE_ARRAY, VirtualFileUtil.toVirtualFileArray(
            JBIterable.from(selection).filterMap(ExternalSystemNode::getVirtualFile).toList()));
        sink.set(CommonDataKeys.NAVIGATABLE_ARRAY, JBIterable.from(selection)
            .filterMap(ExternalSystemNode::getNavigatable).toArray(Navigatable.EMPTY_ARRAY));

        sink.lazy(Location.DATA_KEY, () -> extractLocation(selection));
    }

    @RequiredReadAction
    private @Nullable ExternalSystemTaskLocation extractLocation(List<ExternalSystemNode> selectedNodes) {
        if (selectedNodes.isEmpty()) return null;

        List<TaskData> tasks = new SmartList<>();

        ExternalTaskExecutionInfo taskExecutionInfo = new ExternalTaskExecutionInfo();

        String projectPath = null;

        for (ExternalSystemNode<?> node : selectedNodes) {
            Object data = node.getData();
            if (data instanceof TaskData taskData) {
                if (projectPath == null) {
                    projectPath = taskData.getLinkedExternalProjectPath();
                }
                else if (!taskData.getLinkedExternalProjectPath().equals(projectPath)) {
                    return null;
                }

                taskExecutionInfo.getSettings().getTaskNames().add(taskData.getName());
                taskExecutionInfo.getSettings().getTaskDescriptions().add(taskData.getDescription());
                tasks.add(taskData);
            }
        }

        if (tasks.isEmpty()) return null;

        taskExecutionInfo.getSettings().setExternalSystemIdString(myExternalSystemId.toString());
        taskExecutionInfo.getSettings().setExternalProjectPath(projectPath);

        return ExternalSystemTaskLocation.create(myProject, myExternalSystemId, projectPath, taskExecutionInfo);
    }

    @RequiredUIAccess
    public void init() {
        initTree();

        myProject.getMessageBus().connect(myParentDisposable)
            .subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
                boolean wasVisible;

                @Override
                public void toolWindowShown(ToolWindow toolWindow) {
                    if (!toolWindow.getId().equals(myExternalSystemId.getToolWindowId())) return;
                    if (!wasVisible) {
                        wasVisible = true;
                        scheduleStructureUpdate();
                    }
                }

                @Override
                public void stateChanged(ToolWindowManager toolWindowManager) {
                    boolean visible = myToolWindow.isVisible();
                    if (!visible && wasVisible) {
                        wasVisible = false;
                        scheduleStructureCleanupCache();
                    }
                }
            });

        getShortcutsManager().addListener(this::scheduleTaskAndRunConfigUpdate, myParentDisposable);
        getTaskActivator().addListener(this::scheduleTaskAndRunConfigUpdate, myParentDisposable);

        myProject.getMessageBus().connect(myParentDisposable)
            .subscribe(RunManagerListener.class, new RunManagerListener() {
                private void changed() {
                    scheduleStructureRequest(() -> {
                        if (myStructure != null) {
                            myStructure.visitExistingNodes(ModuleNode.class, ModuleNode::updateRunConfigurations);
                        }
                    });
                }

                @Override
                public void runConfigurationAdded(RunManagerListenerEvent event) { changed(); }

                @Override
                public void runConfigurationRemoved(RunManagerListenerEvent event) { changed(); }

                @Override
                public void runConfigurationChanged(RunManagerListenerEvent event) { changed(); }
            });

        myToolWindow.setAdditionalGearActions(createGearActionsGroup());
        scheduleStructureUpdate();
    }

    private void scheduleTaskAndRunConfigUpdate() {
        scheduleStructureRequest(() -> {
            if (myStructure != null) {
                myStructure.updateNodesAsync(Arrays.asList(TaskNode.class, RunConfigurationNode.class));
            }
        });
    }

    private ActionGroup createGearActionsGroup() {
        DefaultActionGroup group = new DefaultActionGroup();
        // Use inline ToggleAction instances that directly capture the view's state via closure,
        // because gear menu actions run in the tool window header's data context, not the content
        // panel's, so ExternalSystemDataKeys.VIEW would be null when using registered actions.
        group.add(new ToggleAction(LocalizeValue.localizeTODO("Group Modules")) {
            @Override
            public boolean isSelected(AnActionEvent e) { return myState.groupModules; }
            @Override
            public void setSelected(AnActionEvent e, boolean state) { setGroupModules(state); }
        });
        group.add(new ToggleAction(LocalizeValue.localizeTODO("Group Tasks")) {
            @Override
            public boolean isSelected(AnActionEvent e) { return myState.groupTasks; }
            @Override
            public void setSelected(AnActionEvent e, boolean state) { setGroupTasks(state); }
        });
        group.add(new ToggleAction(LocalizeValue.localizeTODO("Show Inherited Tasks")) {
            @Override
            public boolean isSelected(AnActionEvent e) { return myState.showInheritedTasks; }
            @Override
            public void setSelected(AnActionEvent e, boolean state) { setShowInheritedTasks(state); }
        });
        group.add(new ToggleAction(LocalizeValue.localizeTODO("Show Ignored")) {
            @Override
            public boolean isSelected(AnActionEvent e) { return myState.showIgnored; }
            @Override
            public void setSelected(AnActionEvent e, boolean state) { setShowIgnored(state); }
        });
        return group;
    }

    @RequiredUIAccess
    private void initTree() {
        myTree = new SimpleTree();
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup toolbarGroup = (ActionGroup)actionManager.getAction("ExternalSystemView.ActionsToolbar");
        if (toolbarGroup != null) {
            ActionToolbar toolbar = actionManager.createActionToolbar(
                myExternalSystemId.getId() + " View Toolbar", toolbarGroup, true);
            toolbar.setTargetComponent(this);
            setToolbar(toolbar.getComponent());
        }
        setContent(ScrollPaneFactory.createScrollPane(myTree));

        myTree.addMouseListener(new PopupHandler() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public void invokePopup(Component comp, int x, int y) {
                String menuId = getMenuId((List<ExternalSystemNode<?>>) (List<?>) getSelectedNodes(ExternalSystemNode.class));
                if (menuId != null) {
                    ActionGroup menuGroup = (ActionGroup)actionManager.getAction(menuId);
                    if (menuGroup != null) {
                        actionManager.createActionPopupMenu(ExternalProjectsViewImpl.this.getName(), menuGroup)
                            .getComponent().show(comp, x, y);
                    }
                }
            }

            @Nullable
            private String getMenuId(Collection<? extends ExternalSystemNode<?>> nodes) {
                String id = null;
                for (ExternalSystemNode<?> node : nodes) {
                    String menuId = node.getMenuId();
                    if (menuId == null) return null;
                    if (id == null) id = menuId;
                    else if (!id.equals(menuId)) return null;
                }
                return id;
            }
        });
    }

    @Override
    public void scheduleStructureUpdate() {
        scheduleStructureRequest(() -> {
            Collection<DataNode<ProjectData>> projects = myProjectsManager.getProjectData(myExternalSystemId);
            if (myStructure != null) {
                myStructure.updateProjects(projects);
            }
        });
    }

    private void scheduleStructureCleanupCache() {
        scheduleStructureRequest(() -> {
            if (myStructure != null) myStructure.cleanupCache();
        });
    }

    private void initStructure() {
        myStructure = new ExternalProjectsStructure(myProject, myTree);
        Disposer.register(myParentDisposable, myStructure);
        myStructure.init(this);
    }

    private void scheduleStructureRequest(Runnable r) {
        myProject.getApplication().invokeLater(() -> {
            if (myStructure == null) initStructure();
            r.run();
        }, myProject.getDisposed());
    }

    @Override
    public void updateUpTo(ExternalSystemNode<?> node) {
        if (myStructure != null) myStructure.updateUpTo(node);
    }

    @Override
    @Nullable
    public ExternalProjectsStructure getStructure() {
        return myStructure;
    }

    @Override
    public List<ExternalSystemNode<?>> createNodes(ExternalProjectsView view,
                                                   @Nullable ExternalSystemNode<?> parent,
                                                   DataNode<?> dataNode) {
        List<ExternalSystemNode<?>> result = new SmartList<>();
        MultiMap<consulo.externalSystem.model.Key<?>, DataNode<?>> groups = new MultiMap<>();
        for (DataNode<?> child : dataNode.getChildren()) {
            groups.putValue(child.getKey(), child);
        }

        for (ExternalSystemViewContributor contributor : myViewContributors) {
            MultiMap<consulo.externalSystem.model.Key<?>, DataNode<?>> dataNodes = new MultiMap<>();
            for (consulo.externalSystem.model.Key<?> key : contributor.getKeys()) {
                Collection<DataNode<?>> nodes = groups.get(key);
                if (!nodes.isEmpty()) dataNodes.putValues(key, nodes);
            }
            if (dataNodes.isEmpty()) continue;

            List<ExternalSystemNode<?>> childNodes = contributor.createNodes(view, dataNodes);
            result.addAll(childNodes);
            if (parent != null) {
                for (ExternalSystemNode<?> childNode : childNodes) childNode.setParent(parent);
            }
        }
        return result;
    }

    @Override
    public ExternalProjectsStructure.ErrorLevel getErrorLevelRecursively(DataNode<?> node) {
        ExternalProjectsStructure.ErrorLevel[] max = {ExternalProjectsStructure.ErrorLevel.NONE};
        visitNode(node, currentNode -> {
            for (ExternalSystemViewContributor contributor : myViewContributors) {
                ExternalProjectsStructure.ErrorLevel level = contributor.getErrorLevel(currentNode);
                if (level.compareTo(max[0]) > 0) max[0] = level;
            }
        });
        return max[0];
    }

    private static void visitNode(DataNode<?> node, Consumer<DataNode<?>> consumer) {
        consumer.accept(node);
        for (DataNode<?> child : node.getChildren()) visitNode(child, consumer);
    }

    @Override
    public void handleDoubleClickOrEnter(ExternalSystemNode<?> node, @Nullable String actionId, InputEvent inputEvent) {
        if (actionId != null) {
            AnAction action = ActionManager.getInstance().getAction(actionId);
            if (action != null) {
                // Build a DataContext from the view's own data so that VIEW, EXTERNAL_SYSTEM_ID,
                // SELECTED_NODES, and PROJECT are all available to the action.
                // Using createFromInputEvent() would derive the context from the tree's AWT component,
                // which may not reach this panel's getData() in Consulo's component hierarchy.
                List<ExternalSystemNode> selectedNodes = getSelectedNodes(ExternalSystemNode.class);
                DataContext dataContext = DataContext.builder()
                    .add(ExternalSystemDataKeys.VIEW, this)
                    .add(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID, myExternalSystemId)
                    .add(ExternalSystemDataKeys.SELECTED_NODES, selectedNodes)
                    .add(Project.KEY, myProject)
                    .build();
                action.actionPerformed(AnActionEvent.createFromAnAction(action, inputEvent, getName(), dataContext));
            }
        }
        for (Listener listener : myListeners) listener.onDoubleClickOrEnter(node, inputEvent);
    }

    @Override
    public void addListener(Listener listener) {
        myListeners.add(listener);
    }

    @Override
    public Project getProject() { return myProject; }

    @Override
    public ExternalSystemUiAware getUiAware() { return myUiAware; }

    @Override
    public ExternalSystemShortcutsManager getShortcutsManager() { return myProjectsManager.getShortcutsManager(); }

    @Override
    public ExternalSystemTaskActivator getTaskActivator() { return myProjectsManager.getTaskActivator(); }

    @Override
    public ProjectSystemId getSystemId() { return myExternalSystemId; }

    @Override
    public boolean getShowIgnored() { return myState.showIgnored; }

    public void setShowIgnored(boolean value) {
        if (myState.showIgnored != value) { myState.showIgnored = value; scheduleStructureUpdate(); }
    }

    @Override
    public boolean getGroupTasks() { return myState.groupTasks; }

    public void setGroupTasks(boolean value) {
        if (myState.groupTasks != value) { myState.groupTasks = value; scheduleNodesRebuild(TasksNode.class); }
    }

    @Override
    public boolean getGroupModules() { return myState.groupModules; }

    public void setGroupModules(boolean value) {
        if (myState.groupModules != value) {
            myState.groupModules = value;
            scheduleNodesRebuild(ModuleNode.class);
            scheduleNodesRebuild(ProjectNode.class);
        }
    }

    @Override
    public boolean showInheritedTasks() { return myState.showInheritedTasks; }

    public void setShowInheritedTasks(boolean value) {
        if (myState.showInheritedTasks != value) { myState.showInheritedTasks = value; scheduleStructureUpdate(); }
    }

    @Override
    @Nullable
    public String getDisplayName(@Nullable DataNode<?> node) {
        if (node == null) return null;
        for (ExternalSystemViewContributor contributor : myViewContributors) {
            String name = contributor.getDisplayName(node);
            if (name != null) return name;
        }
        return null;
    }

    public ExternalProjectsViewState getState() { return myState; }

    public void loadState(ExternalProjectsViewState state) { myState = state; }

    private <T extends ExternalSystemNode<?>> void scheduleNodesRebuild(Class<T> nodeClass) {
        scheduleStructureRequest(() -> {
            if (myStructure != null) {
                for (T node : myStructure.getNodes(nodeClass)) node.cleanUpCache();
                myStructure.updateNodesAsync(Collections.singleton(nodeClass));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends ExternalSystemNode<?>> List<T> getSelectedNodes(Class<T> aClass) {
        return myStructure != null ? myStructure.getSelectedNodes(myTree, aClass) : Collections.emptyList();
    }
}
