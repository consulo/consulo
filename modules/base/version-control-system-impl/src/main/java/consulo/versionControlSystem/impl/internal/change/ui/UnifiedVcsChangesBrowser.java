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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.PlaceholderLayout;
import consulo.ui.style.ComponentColors;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.impl.internal.action.ShowDiffAction;
import consulo.versionControlSystem.ui.VcsChangesBrowser;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedVcsChangesBrowser implements VcsChangesBrowser {
    private static final String SHOW_DIFF_ACTION = "ChangesView.Diff";

    private final Project myProject;
    private final UnifiedChangesTree myTree;
    private final Label myEmptyText;
    private final PlaceholderLayout myContent = PlaceholderLayout.create();
    private final DockLayout myRoot = DockLayout.create();

    private List<Change> myChanges = List.of();
    private boolean myShowFlatten = true;

    @RequiredUIAccess
    public UnifiedVcsChangesBrowser(Project project, LocalizeValue emptyText, List<AnAction> toolbarActions, Disposable parentDisposable) {
        myProject = project;
        myTree = new UnifiedChangesTree(project, false, parentDisposable);
        myTree.setDoubleClickHandler(item -> {
            if (item instanceof ChangeTreeItem changeItem) {
                ShowDiffAction.showDiffForChange(myProject, myChanges, Math.max(0, myChanges.indexOf(changeItem.change())));
            }
        });

        myEmptyText = Label.create(emptyText, LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build());
        myEmptyText.setForegroundColor(ComponentColors.DISABLED_TEXT);

        DockLayout placeholder = DockLayout.create();
        placeholder.center(myEmptyText);

        myContent.setContent(myTree.getComponent());
        myContent.setPlaceholder(placeholder);
        myContent.setPlaceholderVisibility(() -> myChanges.isEmpty());

        myRoot.putUserData(UiDataProvider.KEY, myTree::uiDataSnapshot);
        myRoot.top(createToolbar(toolbarActions).getUIComponent());
        myRoot.center(myContent);

        myTree.setSnapshot(buildSnapshot());
    }

    @RequiredUIAccess
    private ActionToolbar createToolbar(List<AnAction> toolbarActions) {
        DefaultActionGroup group = new DefaultActionGroup();
        if (ActionManager.getInstance().getAction(SHOW_DIFF_ACTION) instanceof AnAction showDiff) {
            group.add(showDiff);
        }
        group.addSeparator();
        TreeExpander expander = TreeExpander.of(myTree.getComponent());
        CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
        group.add(commonActionsManager.createExpandAllAction(expander));
        group.add(commonActionsManager.createCollapseAllAction(expander));
        group.add(new GroupByDirectoryToggleAction(() -> !myShowFlatten, this::setGroupedByDirectory));
        if (!toolbarActions.isEmpty()) {
            group.addSeparator();
            for (AnAction action : toolbarActions) {
                group.add(action);
            }
        }

        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(myRoot);
        return toolbar;
    }

    @RequiredUIAccess
    private void setGroupedByDirectory(boolean grouped) {
        myShowFlatten = !grouped;
        myTree.setSnapshot(buildSnapshot());
    }

    private ChangesTreeSnapshot buildSnapshot() {
        return new ChangesTreeSnapshotBuilder(myShowFlatten, myProject.getBasePath()).changes(myChanges).build();
    }

    @Override
    public Component getComponent() {
        return myRoot;
    }

    @Override
    public List<Change> getChanges() {
        return myChanges;
    }

    @RequiredUIAccess
    @Override
    public void setChanges(List<Change> changes) {
        myChanges = List.copyOf(changes);
        myTree.setSnapshot(buildSnapshot());
        myContent.testPlaceholder();
    }

    @RequiredUIAccess
    @Override
    public void setEmptyText(LocalizeValue text) {
        myEmptyText.setText(text);
    }
}
