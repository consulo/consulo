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
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.Space;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.DockLayout;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.impl.internal.action.ShowDiffAction;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedShowChangesDialog extends DialogDescriptor {
    private static final String SHOW_DIFF_ACTION = "ChangesView.Diff";

    private final Project myProject;
    private final List<Change> myChanges;
    private final LocalizeValue myDescription;

    private @Nullable UnifiedChangesTree myTree;
    private boolean myShowFlatten = true;

    public UnifiedShowChangesDialog(LocalizeValue title, Project project, List<Change> changes, LocalizeValue description) {
        super(title);
        myProject = project;
        myChanges = changes;
        myDescription = description;
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        UnifiedChangesTree tree = new UnifiedChangesTree(myProject, false, uiDisposable);
        tree.setDoubleClickHandler(item -> {
            if (item instanceof ChangeTreeItem changeItem) {
                ShowDiffAction.showDiff(myProject, new Change[]{changeItem.change()}, tree.getAllChanges());
            }
        });
        tree.setSnapshot(buildSnapshot());
        myTree = tree;

        DockLayout changesPanel = DockLayout.create();
        changesPanel.putUserData(UiDataProvider.KEY, tree::uiDataSnapshot);
        changesPanel.top(createToolbar(tree, changesPanel).getUIComponent());
        changesPanel.center(tree.getComponent());

        DockLayout root = DockLayout.create();
        if (myDescription.isNotEmpty()) {
            HtmlLabel description = HtmlLabel.create(myDescription);
            DockLayout descriptionPanel = DockLayout.create();
            descriptionPanel.center(description);
            descriptionPanel.paddingBuilder().bottomSet(Space.MEDIUM).apply();
            root.top(descriptionPanel);
        }
        root.center(changesPanel);
        return root;
    }

    @RequiredUIAccess
    private ActionToolbar createToolbar(UnifiedChangesTree tree, Component target) {
        DefaultActionGroup group = new DefaultActionGroup();
        if (ActionManager.getInstance().getAction(SHOW_DIFF_ACTION) instanceof AnAction showDiff) {
            group.add(showDiff);
        }
        group.addSeparator();
        TreeExpander expander = TreeExpander.of(tree.getComponent());
        CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
        group.add(commonActionsManager.createExpandAllAction(expander));
        group.add(commonActionsManager.createCollapseAllAction(expander));
        group.add(new GroupByDirectoryToggleAction(() -> !myShowFlatten, grouped -> {
            myShowFlatten = !grouped;
            UnifiedChangesTree current = myTree;
            if (current != null) {
                current.setSnapshot(buildSnapshot());
            }
        }));

        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(target);
        return toolbar;
    }

    private ChangesTreeSnapshot buildSnapshot() {
        return new ChangesTreeSnapshotBuilder(myProject, myShowFlatten).changes(myChanges).build();
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(CommonLocalize.buttonClose());
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        return new AnAction[]{createOkAction()};
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(70, 30);
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return "UnifiedShowChangesDialog";
    }
}
