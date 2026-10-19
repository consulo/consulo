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
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.PlaceholderLayout;
import consulo.ui.style.ComponentColors;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.impl.internal.action.ShowDiffAction;
import consulo.versionControlSystem.impl.internal.change.action.RollbackDialogAction;
import consulo.versionControlSystem.ui.VcsChangesBrowser;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
    private final AnAction myDiffAction;

    @RequiredUIAccess
    public UnifiedVcsChangesBrowser(Project project,
                                    LocalizeValue emptyText,
                                    List<AnAction> toolbarActions,
                                    boolean withRollback,
                                    @Nullable BiConsumer<Change, TextItemPresentation> changeDecorator,
                                    @Nullable Consumer<Change> openHandler,
                                    Disposable parentDisposable) {
        myProject = project;
        AnAction showDiffAction = ActionManager.getInstance().getAction(SHOW_DIFF_ACTION) instanceof AnAction showDiff ? showDiff : new ShowDiffAction();
        myDiffAction = openHandler == null ? showDiffAction : createOpenAction(showDiffAction, openHandler);
        myTree = new UnifiedChangesTree(project, false, parentDisposable);
        myTree.setDoubleClickHandler(item -> {
            if (item instanceof ChangeTreeItem changeItem) {
                if (openHandler != null) {
                    openHandler.accept(changeItem.change());
                }
                else {
                    ShowDiffAction.showDiffForChange(myProject, myChanges, Math.max(0, myChanges.indexOf(changeItem.change())));
                }
            }
        });
        myTree.setChangeDecorator(changeDecorator);

        myEmptyText = Label.create(emptyText, LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build());
        myEmptyText.setForegroundColor(ComponentColors.DISABLED_TEXT);

        DockLayout placeholder = DockLayout.create();
        placeholder.center(myEmptyText);

        myContent.setContent(myTree.getComponent());
        myContent.setPlaceholder(placeholder);
        myContent.setPlaceholderVisibility(() -> myChanges.isEmpty());

        List<AnAction> actions = new ArrayList<>(toolbarActions);
        if (withRollback) {
            actions.add(new RollbackDialogAction());
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            ChangeListListener listener = new ChangeListListener() {
                @Override
                public void changeListUpdateDone() {
                    UIAccess uiAccess = project.getUIAccess();
                    if (uiAccess.isValid()) {
                        uiAccess.give(() -> dropRolledBackChanges(changeListManager));
                    }
                }
            };
            changeListManager.addChangeListListener(listener);
            Disposer.register(parentDisposable, () -> changeListManager.removeChangeListListener(listener));
        }

        myRoot.putUserData(UiDataProvider.KEY, myTree::uiDataSnapshot);
        myRoot.top(createToolbar(actions).getUIComponent());
        myRoot.center(myContent);

        myTree.setSnapshot(buildSnapshot());
    }

    private AnAction createOpenAction(AnAction showDiffAction, Consumer<Change> openHandler) {
        Presentation template = showDiffAction.getTemplatePresentation();
        return new DumbAwareAction(template.getTextValue(), template.getDescription(), template.getIcon()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                List<Change> selected = myTree.getSelectedChanges();
                if (!selected.isEmpty()) {
                    openHandler.accept(selected.get(0));
                }
            }
        };
    }

    @RequiredUIAccess
    private void dropRolledBackChanges(ChangeListManager changeListManager) {
        Collection<Change> allChanges = changeListManager.getAllChanges();
        List<Change> remaining = new ArrayList<>();
        for (Change change : myChanges) {
            if (allChanges.contains(change)) {
                remaining.add(change);
            }
        }
        if (remaining.size() != myChanges.size()) {
            setChanges(remaining);
        }
    }

    @RequiredUIAccess
    private ActionToolbar createToolbar(List<AnAction> toolbarActions) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(myDiffAction);
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
        return new ChangesTreeSnapshotBuilder(myProject, myShowFlatten).changes(myChanges).build();
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

    @Override
    public AnAction getDiffAction() {
        return myDiffAction;
    }

    @Override
    public List<Change> getSelectedChanges() {
        return myTree.getSelectedChanges();
    }

    @Override
    public boolean isGroupSelected() {
        return myTree.isGroupSelected();
    }

    @Override
    public void addSelectionListener(Disposable parentDisposable, Runnable listener) {
        Disposable registration = myTree.getComponent().addSelectListener(event -> listener.run());
        Disposer.register(parentDisposable, registration);
    }

    @RequiredUIAccess
    @Override
    public void selectChanges(Collection<Change> changes) {
        for (Change change : changes) {
            List<ChangesTreeItem> path = myTree.findPath(change);
            if (path != null) {
                myTree.selectPath(path);
                return;
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void selectFirst() {
        myTree.selectFirst();
    }
}
