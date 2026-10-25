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
package consulo.versionControlSystem.impl.internal.workspace;

import consulo.application.HelpManager;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.MultiSelectListBox;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.workspace.VcsWorkspace;
import consulo.versionControlSystem.workspace.VcsWorkspaceDataKeys;
import consulo.versionControlSystem.workspace.VcsWorkspaceGroup;
import consulo.versionControlSystem.workspace.VcsWorkspaceLink;
import consulo.versionControlSystem.workspace.VcsWorkspaceProvider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VcsWorkspacePanel implements UiDataProvider, Disposable {
    private final Project myProject;
    private final VcsWorkspaceProvider myProvider;
    private final MutableFlatDataModel<VcsWorkspaceListEntry> myListModel;
    private final MultiSelectListBox<VcsWorkspaceListEntry> myList;
    private final ScrollableLayout myListScroll;
    private final DockLayout myContent;
    private final DockLayout myComponent;
    private final AtomicBoolean myRebuildScheduled = new AtomicBoolean();

    private List<VcsWorkspaceListEntry> myEntries = List.of();
    private List<? extends VcsWorkspaceGroup> myGroups = List.of();
    private @Nullable Component myEmptyPanel;
    private volatile boolean myDisposed;

    @RequiredUIAccess
    public VcsWorkspacePanel(Project project, VcsWorkspaceProvider provider) {
        myProject = project;
        myProvider = provider;
        myListModel = FlatDataModel.of(List.of());

        myList = MultiSelectListBox.create(myListModel);
        myList.setRenderer(ComponentItemRenderer.reusable(() -> new VcsWorkspaceRowComponent().getComponent(), (row, item) -> {
            VcsWorkspaceListEntry entry = item.getValue();
            VcsWorkspaceRowComponent rowComponent = row.getUserData(VcsWorkspaceRowComponent.KEY);
            if (entry != null && rowComponent != null) {
                rowComponent.configure(entry);
            }
        }));
        myList.setSpeedSearchConverter(entry -> entry.workspace().getName() + " " + entry.workspace().getRevisionText().get());
        myList.addContextMenuListener(this::showContextMenu);
        myList.addDoubleClickListener(event -> openSelected());

        myListScroll = ScrollableLayout.create(myList);
        myContent = DockLayout.create(Space.NONE);
        myContent.center(myListScroll);

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(provider.getActionPlace(), provider.getToolbarActions(), ActionToolbar.Style.VERTICAL);

        myComponent = DockLayout.create(Space.NONE);
        myComponent.left(toolbar.getUIComponent());
        myComponent.center(myContent);
        myComponent.putUserData(UiDataProvider.KEY, this);
        toolbar.setTargetUIComponent(myComponent);

        provider.addChangeListener(this::scheduleRebuild, this);
        applyGroups(List.of());
        scheduleRebuild();
    }

    public Component getComponent() {
        return myComponent;
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        List<VcsWorkspaceListEntry> selected = myList.getValue();
        List<VcsWorkspace> workspaces = new ArrayList<>();
        for (VcsWorkspaceListEntry entry : selected) {
            workspaces.add(entry.workspace());
        }
        sink.set(VcsWorkspaceDataKeys.PROVIDER, myProvider);
        sink.set(VcsWorkspaceDataKeys.SELECTED_WORKSPACES, workspaces);
        sink.set(VcsWorkspaceDataKeys.SELECTED_GROUP, resolveSelectedGroup(selected));
        String helpId = myProvider.getHelpId();
        if (helpId != null) {
            sink.set(HelpManager.HELP_ID, helpId);
        }
    }

    private @Nullable VcsWorkspaceGroup resolveSelectedGroup(List<VcsWorkspaceListEntry> selected) {
        List<VcsWorkspaceGroup> groups = selected.stream().map(VcsWorkspaceListEntry::group).distinct().toList();
        if (groups.size() == 1) {
            return groups.getFirst();
        }
        if (groups.isEmpty()) {
            return myGroups.size() == 1 ? myGroups.getFirst() : null;
        }
        return null;
    }

    private void scheduleRebuild() {
        if (myDisposed || !myRebuildScheduled.compareAndSet(false, true)) {
            return;
        }
        myProject.getApplication().getInstance(ApplicationConcurrency.class).getExecutorService().execute(() -> {
            myRebuildScheduled.set(false);
            if (myDisposed) {
                return;
            }
            List<? extends VcsWorkspaceGroup> groups = myProvider.getGroups();
            myProject.getUIAccess().give(() -> {
                if (!myDisposed) {
                    applyGroups(groups);
                }
            });
        });
    }

    @RequiredUIAccess
    private void applyGroups(List<? extends VcsWorkspaceGroup> groups) {
        myGroups = groups;
        boolean multiRoot = groups.size() > 1;
        List<VcsWorkspaceListEntry> entries = new ArrayList<>();
        for (VcsWorkspaceGroup group : groups) {
            for (VcsWorkspace workspace : group.getWorkspaces()) {
                entries.add(new VcsWorkspaceListEntry(group, workspace, multiRoot));
            }
        }
        if (entries.equals(myEntries) && !entries.isEmpty()) {
            return;
        }
        myEntries = entries;

        Set<FilePath> previouslySelectedPaths = new HashSet<>();
        for (VcsWorkspaceListEntry entry : myList.getValue()) {
            previouslySelectedPaths.add(entry.workspace().getPath());
        }

        myListModel.replaceAll(entries);

        List<VcsWorkspaceListEntry> toSelect = new ArrayList<>();
        for (VcsWorkspaceListEntry entry : entries) {
            if (previouslySelectedPaths.contains(entry.workspace().getPath())) {
                toSelect.add(entry);
            }
        }
        if (!toSelect.isEmpty()) {
            myList.setValue(toSelect);
        }
        else {
            selectDefaultEntry(entries);
        }

        updateEmptyState(entries.isEmpty());
    }

    @RequiredUIAccess
    private void selectDefaultEntry(List<VcsWorkspaceListEntry> entries) {
        VcsWorkspaceListEntry toSelect = entries.stream().filter(it -> it.workspace().isCurrent()).findFirst()
            .orElse(entries.isEmpty() ? null : entries.getFirst());
        if (toSelect != null) {
            myList.setValue(List.of(toSelect));
        }
    }

    @RequiredUIAccess
    private void updateEmptyState(boolean empty) {
        if (!empty) {
            if (myEmptyPanel != null) {
                myContent.center(myListScroll);
                myEmptyPanel = null;
            }
            return;
        }

        VerticalLayout layout = VerticalLayout.create(Space.MEDIUM, HorizontalAlignment.CENTER);
        layout.add(Label.create(myProvider.getEmptyText()));
        for (VcsWorkspaceLink link : myProvider.getEmptyLinks()) {
            Hyperlink hyperlink = Hyperlink.create(link.getText(), event -> link.run());
            Image icon = link.getIcon();
            if (icon != null) {
                hyperlink.setIcon(icon);
            }
            layout.add(hyperlink);
        }

        DockLayout panel = DockLayout.create(Space.NONE);
        panel.top(layout);
        panel.paddingBuilder().verticalSet(Space.LARGE).apply();
        myEmptyPanel = panel;
        myContent.center(panel);
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        if (myList.getValue().isEmpty()) {
            return;
        }
        ActionManager actionManager = ActionManager.getInstance();
        ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(myProvider.getActionPlace(), myProvider.getPopupActions());
        popupMenu.setTargetComponent(myList);
        popupMenu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    @RequiredUIAccess
    private void openSelected() {
        List<VcsWorkspaceListEntry> selected = myList.getValue();
        if (selected.size() == 1) {
            myProvider.open(selected.getFirst().workspace());
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }
}
