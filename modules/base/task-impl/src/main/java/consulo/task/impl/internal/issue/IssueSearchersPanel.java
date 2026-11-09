/*
 * Copyright 2013-2026 consulo.io
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
package consulo.task.impl.internal.issue;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.task.TaskRepositoryType;
import consulo.task.impl.internal.TaskProjectConfiguration;
import consulo.task.impl.internal.TaskSearcher;
import consulo.task.impl.internal.TaskSearchersWorkspace;
import consulo.task.localize.TaskLocalize;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
final class IssueSearchersPanel {
    private final Project myProject;
    private final TaskRepositoryType<?> myType;
    private final Runnable myHide;
    private final Consumer<TaskSearcher> mySelect;
    private final MutableFlatDataModel<Object> myModel = FlatDataModel.of(List.of());
    private final ListBox<Object> myList;
    private final DockLayout myRoot;
    private final IssueSearcherItem myAllIssues;

    @RequiredUIAccess
    IssueSearchersPanel(Project project, TaskRepositoryType<?> type, Runnable hide, Consumer<TaskSearcher> select) {
        myProject = project;
        myType = type;
        myHide = hide;
        mySelect = select;

        TaskSearcher all = new TaskSearcher();
        all.name = TaskLocalize.searchersAllIssues().get();
        all.type = type.getId();
        myAllIssues = new IssueSearcherItem(all, false, true);

        myList = ListBox.create(myModel);
        myList.isSeparator(value -> value instanceof IssueSearcherGroup);
        myList.setSeparatorCaption(value -> value instanceof IssueSearcherGroup group ? group.getTitle() : LocalizeValue.empty());
        myList.setRenderer((presentation, renderItem) -> {
            Object value = renderItem.getValue();
            if (value instanceof IssueSearcherGroup group) {
                presentation.append(group.getTitle());
            }
            else if (value instanceof IssueSearcherItem item) {
                presentation.withIcon(item.builtIn ? PlatformIconGroup.nodesFolder() : PlatformIconGroup.actionsSearch());
                presentation.append(item.searcher.name);
            }
        });
        myList.addValueListener(event -> {
            if (event.getValue() instanceof IssueSearcherItem item) {
                mySelect.accept(item.searcher);
            }
        });

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        group.add(new HideAction());
        group.addSeparator();
        group.add(new AddAction());
        group.add(new EditAction());
        group.add(new DeleteAction());
        group.add(new ShareAction());
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar("TaskIssueSearchers", group.build(), ActionToolbar.Style.VERTICAL);
        toolbar.setTargetUIComponent(myList);

        DockLayout toolbarColumn = DockLayout.create(Space.NONE);
        toolbarColumn.center(toolbar.getUIComponent());
        toolbarColumn.borderBuilder().rightSet().apply();

        myRoot = DockLayout.create(Space.NONE);
        myRoot.left(toolbarColumn);
        myRoot.center(ScrollableLayout.create(myList));

        reload(myAllIssues.searcher, false);
    }

    Component getComponent() {
        return myRoot;
    }

    TaskSearcher getDefaultSearcher() {
        return myAllIssues.searcher;
    }

    private List<TaskSearcher> getStore(boolean project) {
        return project
            ? myProject.getInstance(TaskProjectConfiguration.class).getState().searchers
            : myProject.getInstance(TaskSearchersWorkspace.class).getState().searchers;
    }

    @RequiredUIAccess
    private void reload(@Nullable TaskSearcher toSelect) {
        reload(toSelect, true);
    }

    @RequiredUIAccess
    private void reload(@Nullable TaskSearcher toSelect, boolean notify) {
        List<Object> items = new ArrayList<>();
        List<IssueSearcherItem> project = collect(true);
        if (!project.isEmpty()) {
            items.add(IssueSearcherGroup.PROJECT);
            items.addAll(project);
        }
        items.add(IssueSearcherGroup.PERSONAL);
        items.add(myAllIssues);
        items.addAll(collect(false));
        myModel.replaceAll(items);

        for (Object item : items) {
            if (item instanceof IssueSearcherItem searcherItem && searcherItem.searcher == toSelect) {
                myList.setValue(item);
                if (notify) {
                    mySelect.accept(searcherItem.searcher);
                }
                return;
            }
        }
    }

    private List<IssueSearcherItem> collect(boolean project) {
        List<IssueSearcherItem> result = new ArrayList<>();
        for (TaskSearcher searcher : getStore(project)) {
            if (myType.getId().equals(searcher.type)) {
                result.add(new IssueSearcherItem(searcher, project, false));
            }
        }
        return result;
    }

    private @Nullable IssueSearcherItem getSelectedItem() {
        return myList.getValue() instanceof IssueSearcherItem item ? item : null;
    }

    @RequiredUIAccess
    private void store(IssueSearcherItem item) {
        getStore(item.project).add(item.searcher);
        reload(item.searcher);
    }

    private class HideAction extends DumbAwareAction {
        private HideAction() {
            super(TaskLocalize.searchersActionHide(), LocalizeValue.empty(), PlatformIconGroup.generalArrowleft());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            myHide.run();
        }
    }

    private class AddAction extends DumbAwareAction {
        private AddAction() {
            super(TaskLocalize.searchersActionAdd(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            TaskSearcher searcher = new TaskSearcher();
            searcher.type = myType.getId();
            IssueSearcherDialogDescriptor.showAsync(TaskLocalize.searchersDialogAddTitle(), new IssueSearcherItem(searcher, false, false))
                .thenAccept(item -> {
                    if (item != null) {
                        store(item);
                    }
                });
        }
    }

    private class EditAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private EditAction() {
            super(TaskLocalize.searchersActionEdit(), LocalizeValue.empty(), PlatformIconGroup.actionsEdit());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            IssueSearcherItem selected = getSelectedItem();
            if (selected == null || selected.builtIn) {
                return;
            }
            IssueSearcherDialogDescriptor.showAsync(TaskLocalize.searchersDialogEditTitle(), selected).thenAccept(item -> {
                if (item != null) {
                    getStore(selected.project).remove(selected.searcher);
                    store(item);
                }
            });
        }

        @Override
        public void update(AnActionEvent e) {
            IssueSearcherItem selected = getSelectedItem();
            e.getPresentation().setEnabled(selected != null && !selected.builtIn);
        }
    }

    private class DeleteAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private DeleteAction() {
            super(TaskLocalize.searchersActionDelete(), LocalizeValue.empty(), PlatformIconGroup.generalRemove());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            IssueSearcherItem selected = getSelectedItem();
            if (selected == null || selected.builtIn) {
                return;
            }
            getStore(selected.project).remove(selected.searcher);
            reload(myAllIssues.searcher);
        }

        @Override
        public void update(AnActionEvent e) {
            IssueSearcherItem selected = getSelectedItem();
            e.getPresentation().setEnabled(selected != null && !selected.builtIn);
        }
    }

    private class ShareAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private ShareAction() {
            super(TaskLocalize.searchersActionShare(), LocalizeValue.empty(), PlatformIconGroup.nodesShared());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            IssueSearcherItem selected = getSelectedItem();
            if (selected == null || selected.builtIn) {
                return;
            }
            getStore(selected.project).remove(selected.searcher);
            store(new IssueSearcherItem(selected.searcher, !selected.project, false));
        }

        @Override
        public void update(AnActionEvent e) {
            IssueSearcherItem selected = getSelectedItem();
            e.getPresentation().setEnabled(selected != null && !selected.builtIn);
            e.getPresentation().setText(selected != null && selected.project
                ? TaskLocalize.searchersActionMakePersonal()
                : TaskLocalize.searchersActionShare());
        }
    }
}
