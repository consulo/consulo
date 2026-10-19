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
package consulo.versionControlSystem.distributed.impl.internal.branch;

import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.PlatformDataKeys;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.event.details.KeyCode;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.distributed.branch.DvcsBranchDataKeys;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreeModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreeNodePresentation;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsRef;
import consulo.versionControlSystem.log.VcsLogObjectsFactory;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.event.VcsLogListener;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
final class DvcsBranchesLogSidePanel {
    static final Key<DvcsBranchesLogSidePanel> KEY = Key.create("DvcsBranchesLogSidePanel");

    private class BranchesTreeModel implements TreeModel<Object> {
        @Override
        public void buildChildren(Function<Object, TreeNode<Object>> nodeFactory, @Nullable Object parentValue) {
            DvcsBranchesTreeModel model = myTreeModel;
            DvcsBranchesTreeNodePresentation presentation = myPresentation;
            Object parent = parentValue == null ? model.getRoot() : parentValue;

            for (Object child : model.getChildNodes(parent)) {
                TreeNode<Object> node = nodeFactory.apply(child);
                node.setLeaf(model.isLeaf(child));
                node.setRenderer(presentation::render);
            }
        }

        @Override
        public boolean onDoubleClick(Tree<Object> tree, TreeNode<Object> node) {
            DvcsRef ref = DvcsBranchesTreeModel.asRef(node.getValue());
            if (ref == null) {
                return true;
            }
            filterBy(ref);
            return false;
        }
    }

    private final Project myProject;
    private final VcsLogUi myLogUi;
    private final DvcsBranchesTreePopupModel<?> myModel;
    private final TextBox mySearchField;
    private final Tree<Object> myTree;
    private final DockLayout myRoot;

    private DvcsBranchesTreeModel myTreeModel;
    private DvcsBranchesTreeNodePresentation myPresentation;
    private @Nullable String myPattern;

    @RequiredUIAccess
    DvcsBranchesLogSidePanel(Project project, VcsLogUi logUi, DvcsBranchesTreePopupModel<?> model, Disposable parentDisposable) {
        myProject = project;
        myLogUi = logUi;
        myModel = model;
        myTreeModel = model.createRefTreeModel();
        myPresentation = createPresentation(myTreeModel);

        mySearchField = TextBox.create();
        mySearchField.addStyle(TextBoxStyle.INPLACE);
        mySearchField.setPlaceholder(DistributedVcsLocalize.branchesPaneSearchPlaceholder());
        mySearchField.setPrefixComponent(ImageBox.create(PlatformIconGroup.actionsSearch()));
        mySearchField.addValueListener(event -> {
            myPattern = event.getValue();
            myTreeModel.setSearchPattern(myPattern);
            refresh();
        });

        myTree = Tree.create(new BranchesTreeModel());
        myTree.addKeyPressedListener(event -> {
            if (KeyCode.ENTER.equals(event.getInputDetails().getKeyCode())) {
                filterBySelectedRef();
            }
        });
        myTree.addContextMenuListener(this::showContextMenu);

        DockLayout searchRow = DockLayout.create(Space.NONE);
        searchRow.center(mySearchField);
        searchRow.borderBuilder().bottomSet().apply();

        TreeExpander expander = TreeExpander.of(myTree);
        myRoot = DockLayout.create(Space.NONE);
        myRoot.top(searchRow);
        myRoot.center(ScrollableLayout.create(myTree));
        myRoot.putUserData(UiDataProvider.KEY, sink -> {
            sink.set(KEY, this);
            sink.set(PlatformDataKeys.TREE_EXPANDER, expander);
            sink.set(DvcsBranchDataKeys.REPOSITORY, myModel.getRepository());
            sink.set(DvcsBranchDataKeys.SELECTED_REF, getSelectedRef());
        });

        VcsLogListener logListener = (dataPack, refreshHappened) -> {
            if (refreshHappened) {
                rebuild();
            }
        };
        logUi.addLogListener(logListener);
        Disposer.register(parentDisposable, () -> logUi.removeLogListener(logListener));

        refresh();
    }

    Component getComponent() {
        return myRoot;
    }

    DvcsBranchesTreePopupModel<?> getModel() {
        return myModel;
    }

    @RequiredUIAccess
    void focusSearch() {
        mySearchField.focus();
    }

    boolean isDirectoryGrouping() {
        return myTreeModel.isDirectoryGrouping();
    }

    @RequiredUIAccess
    void setDirectoryGrouping(boolean directoryGrouping) {
        myTreeModel.setDirectoryGrouping(directoryGrouping);
        myTreeModel.setSearchPattern(myPattern);
        refresh();
    }

    @Nullable
    DvcsRef getSelectedRef() {
        TreeNode<Object> node = myTree.getSelectedNode();
        return node == null ? null : DvcsBranchesTreeModel.asRef(node.getValue());
    }

    @RequiredUIAccess
    void filterBySelectedRef() {
        DvcsRef ref = getSelectedRef();
        if (ref != null) {
            filterBy(ref);
        }
    }

    @RequiredUIAccess
    private void filterBy(DvcsRef ref) {
        VcsLogObjectsFactory factory = myProject.getInstance(VcsLogObjectsFactory.class);
        myLogUi.getFilterUi().setFilter(factory.createBranchFilter(List.of(ref.getName())));
    }

    @RequiredUIAccess
    private void rebuild() {
        boolean directoryGrouping = myTreeModel.isDirectoryGrouping();
        DvcsBranchesTreeModel treeModel = myModel.createRefTreeModel();
        treeModel.setDirectoryGrouping(directoryGrouping);
        treeModel.setSearchPattern(myPattern);
        myTreeModel = treeModel;
        myPresentation = createPresentation(treeModel);
        refresh();
    }

    @RequiredUIAccess
    private void refresh() {
        myTree.refreshAll().thenCompose(ignored -> myTree.expandAll(Integer.MAX_VALUE));
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        DvcsRef ref = getSelectedRef();
        ActionGroup group = ref == null ? null : myModel.createRefActionGroup(ref);
        if (group == null) {
            return;
        }
        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
        menu.setTargetComponent(myTree);
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    private static DvcsBranchesTreeNodePresentation createPresentation(DvcsBranchesTreeModel treeModel) {
        return new DvcsBranchesTreeNodePresentation(() -> treeModel, null, treeModel.getRepositories());
    }
}
