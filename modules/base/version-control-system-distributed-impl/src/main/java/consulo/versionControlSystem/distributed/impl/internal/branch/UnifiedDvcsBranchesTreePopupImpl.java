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

import consulo.component.ComponentManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.HeavyPopup;
import consulo.ui.ImageBox;
import consulo.ui.Length;
import consulo.ui.LightPopup;
import consulo.ui.Popup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.event.details.MouseInputDetails;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.impl.internal.popup.UnifiedPopupImpl;
import consulo.ui.ex.internal.PopupTree;
import consulo.ui.ex.internal.UIInternalEx;
import consulo.ui.ex.popup.AsyncPopupStep;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.layout.DockLayout;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreeModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopup;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupActions;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupStepBase;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreeSeparator;
import org.jspecify.annotations.Nullable;

import javax.swing.event.ListSelectionListener;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedDvcsBranchesTreePopupImpl<T extends Repository> extends UnifiedPopupImpl implements DvcsBranchesTreePopup<T> {
    private static final Logger LOG = Logger.getInstance(UnifiedDvcsBranchesTreePopupImpl.class);

    private final @Nullable Project myProject;
    private final DvcsBranchesTreePopupModel<T> myModel;
    private final DvcsBranchesTreePopupStepBase myStep;

    private final List<Consumer<Object>> mySelectionListeners = new ArrayList<>();

    private @Nullable Popup myPopup;
    private @Nullable Tree<Object> myTree;
    private @Nullable PopupTree<Object> myPopupTree;
    private @Nullable TextBox mySearchField;
    private @Nullable JBPopup myChild;

    public UnifiedDvcsBranchesTreePopupImpl(@Nullable Project project, DvcsBranchesTreePopupModel<T> model) {
        myProject = project;
        myModel = model;

        DvcsBranchesTreeModel treeModel = model.createTreeModel();

        myStep = model.createTreePopupStep(treeModel);
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private Component createContent() {
        TextBox searchField = TextBox.create();
        searchField.addStyle(TextBoxStyle.INPLACE);
        searchField.setPlaceholder(myStep.getSearchFieldEmptyText());
        searchField.setPrefixComponent(ImageBox.create(PlatformIconGroup.actionsSearch()));
        mySearchField = searchField;

        Tree<Object> tree = UIInternalEx.get()._Components_popupTree(new UnifiedDvcsBranchesTreeModel(myStep));
        myTree = tree;

        if (tree instanceof PopupTree<?> popupTree) {
            PopupTree<Object> objectTree = (PopupTree<Object>) popupTree;
            objectTree.setSeparator(value -> value instanceof DvcsBranchesTreeSeparator);
            objectTree.setSelectOnHover(true);
            objectTree.setActivateListener(this::onActivated);
            myPopupTree = objectTree;
        }

        tree.addSelectListener(event -> {
            TreeNode<Object> node = event.getValue();
            Object value = node == null ? null : node.getValue();
            for (Consumer<Object> listener : new ArrayList<>(mySelectionListeners)) {
                listener.accept(value);
            }
        });

        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            myModel.getVcs().getId() + "BranchesPopup",
            DvcsBranchesTreePopupActions.createHeaderGroup(myModel, myStep, this::refreshTree),
            ActionToolbar.Style.INPLACE
        );
        toolbar.setTargetUIComponent(tree);
        searchField.setSuffixComponent(toolbar.getUIComponent());

        searchField.addValueListener(event -> onSearchPatternChanged(event.getValue()));
        searchField.addKeyPressedListener(event -> onKeyPressed(event.getInputDetails(), true));
        tree.addKeyPressedListener(event -> onKeyPressed(event.getInputDetails(), false));

        DockLayout searchRow = DockLayout.create(Space.NONE);
        searchRow.center(searchField);
        searchRow.borderBuilder().topSet().bottomSet().apply();
        searchRow.paddingBuilder().horizontalSet(Space.SMALL).apply();

        DockLayout content = DockLayout.create(Space.NONE);
        content.top(searchRow);
        content.center(tree);
        content.setSize(Length.ofFont(29), Length.ofFont(24));
        return content;
    }

    @RequiredUIAccess
    private void onSearchPatternChanged(@Nullable String pattern) {
        myStep.updateTreeModelIfNeeded(pattern);
        myStep.setSearchPattern(pattern);
        refreshTree();
    }

    @RequiredUIAccess
    private void refreshTree() {
        Tree<Object> tree = myTree;
        if (tree == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        tree.refreshAll()
            .thenCompose(ignored -> tree.expandAll(Integer.MAX_VALUE))
            .whenCompleteAsync((ignored, error) -> selectPreferred(), uiAccess);
    }

    @RequiredUIAccess
    private void selectPreferred() {
        Tree<Object> tree = myTree;
        TreeNode<Object> root = tree == null ? null : tree.getRootNode();
        if (tree == null || root == null) {
            return;
        }

        List<Object> path = myStep.getPreferredSelection();
        if (path == null) {
            TreeNode<Object> first = findFirstSelectable(root.getLoadedChildren());
            if (first != null) {
                tree.select(first);
            }
            return;
        }

        CompletableFuture<TreeNode<Object>> node = CompletableFuture.completedFuture(root);
        for (Object value : path.subList(1, path.size())) {
            node = node.thenCompose(parent -> parent == null
                ? CompletableFuture.<TreeNode<Object>>completedFuture(null)
                : parent.findChild(child -> Objects.equals(child, value)));
        }

        node.whenCompleteAsync((found, error) -> {
            if (found != null) {
                tree.select(found);
            }
        }, UIAccess.current());
    }

    private @Nullable TreeNode<Object> findFirstSelectable(List<TreeNode<Object>> nodes) {
        for (TreeNode<Object> node : nodes) {
            if (myStep.isSelectable(node.getValue())) {
                return node;
            }

            TreeNode<Object> child = findFirstSelectable(node.getLoadedChildren());
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void onKeyPressed(KeyboardInputDetails details, boolean fromSearchField) {
        PopupTree<Object> popupTree = myPopupTree;
        if (popupTree == null) {
            return;
        }

        KeyCode keyCode = details.getKeyCode();
        if (KeyCode.UP.equals(keyCode)) {
            popupTree.selectPrevious();
        }
        else if (KeyCode.DOWN.equals(keyCode)) {
            popupTree.selectNext();
        }
        else if (KeyCode.ENTER.equals(keyCode)) {
            handleSelect(selectedNode(), true, false);
        }
        else if (KeyCode.RIGHT.equals(keyCode) && canNavigateHorizontally(fromSearchField)) {
            TreeNode<Object> node = selectedNode();
            if (node != null && !node.isLeaf()) {
                popupTree.setExpanded(node, true);
            }
            else {
                handleSelect(node, false, false);
            }
        }
        else if (KeyCode.LEFT.equals(keyCode) && canNavigateHorizontally(fromSearchField)) {
            TreeNode<Object> node = selectedNode();
            if (node != null && popupTree.isExpanded(node)) {
                popupTree.setExpanded(node, false);
            }
        }
    }

    private boolean canNavigateHorizontally(boolean fromSearchField) {
        TextBox searchField = mySearchField;
        return !fromSearchField || searchField == null || StringUtil.isEmpty(searchField.getValue());
    }

    @RequiredUIAccess
    private void onActivated(TreeNode<Object> node, InputDetails details) {
        handleSelect(node, true, details instanceof MouseInputDetails);
    }

    private @Nullable TreeNode<Object> selectedNode() {
        Tree<Object> tree = myTree;
        return tree == null ? null : tree.getSelectedNode();
    }

    @RequiredUIAccess
    private void handleSelect(@Nullable TreeNode<Object> node, boolean handleFinalChoices, boolean toggleGroups) {
        if (node == null) {
            return;
        }

        Object value = node.getValue();
        if (!myStep.isSelectable(value)) {
            PopupTree<Object> popupTree = myPopupTree;
            if (toggleGroups && popupTree != null && !node.isLeaf()) {
                popupTree.setExpanded(node, !popupTree.isExpanded(node));
            }
            return;
        }

        closeChild();

        boolean hasNextStep = myStep.hasSubstep(value);
        if (!hasNextStep && !handleFinalChoices) {
            return;
        }

        PopupStep<?> queriedStep = myStep.onChosen(value, handleFinalChoices);

        if (queriedStep == PopupStep.FINAL_CHOICE || !hasNextStep) {
            setFinalRunnable(myStep.getFinalRunnable());
            closeOk(null);
        }
        else {
            showChild(queriedStep);
        }
    }

    @RequiredUIAccess
    private void showChild(@Nullable PopupStep<?> step) {
        if (step instanceof AsyncPopupStep<?> asyncStep) {
            UIAccess uiAccess = UIAccess.current();
            CompletableFuture.supplyAsync(() -> {
                try {
                    return asyncStep.call();
                }
                catch (Exception e) {
                    throw new CompletionException(e);
                }
            }).whenCompleteAsync((resolved, error) -> {
                if (error != null) {
                    LOG.error("Branch popup failed to build its substep", error);
                }
                else if (!isDisposed()) {
                    showChild(resolved);
                }
            }, uiAccess);
            return;
        }

        Tree<Object> tree = myTree;
        if (!(step instanceof ListPopupStep<?> listStep) || tree == null) {
            return;
        }

        ListPopup child = JBPopupFactory.getInstance().createListPopup(myProject, listStep);
        myChild = child;
        child.addListener(new JBPopupListener() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                if (myChild == child) {
                    myChild = null;
                }

                if (event.isOk()) {
                    closeOk(null);
                }
            }
        });
        child.showBy(tree, null, PopupPosition.END);
    }

    @RequiredUIAccess
    private void closeChild() {
        JBPopup child = myChild;
        myChild = null;
        if (child != null && child.isVisible()) {
            child.cancel();
        }
    }

    @Override
    @RequiredUIAccess
    public void showCenteredInCurrentWindow(ComponentManager project) {
        show(null, null);
    }

    @Override
    @RequiredUIAccess
    public void showBy(Component component, @Nullable InputDetails inputDetails) {
        show(component, null);
    }

    @Override
    @RequiredUIAccess
    public void showBy(Component component, @Nullable InputDetails inputDetails, PopupPosition position) {
        show(component, position);
    }

    @RequiredUIAccess
    private void show(@Nullable Component anchor, @Nullable PopupPosition position) {
        if (isDisposed() || myPopup != null) {
            return;
        }

        PopupOptions.Builder options = PopupOptions.builder().resizable().movable();
        if (position != null) {
            options.position(position);
        }

        Popup popup = anchor != null ? LightPopup.create(options.build()) : HeavyPopup.create(options.build());
        popup.setContent(createContent());
        popup.addCloseListener(event -> cancel(null));

        myPopup = popup;

        fireBeforeShown();

        if (anchor != null && popup instanceof LightPopup light) {
            light.showBy(anchor);
        }
        else if (popup instanceof HeavyPopup heavy) {
            heavy.showInCenterOf(null);
        }

        TextBox searchField = mySearchField;
        if (searchField != null) {
            searchField.focus();
        }

        refreshTree();
    }

    @Override
    @RequiredUIAccess
    public void cancel(@Nullable InputEvent e) {
        if (isDisposed()) {
            return;
        }

        closeChild();

        Popup popup = myPopup;
        myPopup = null;

        if (popup != null && popup.isVisible()) {
            popup.close();
        }

        if (!isOk()) {
            myStep.canceled();
        }

        finish();
    }

    @Override
    public boolean isVisible() {
        Popup popup = myPopup;
        return popup != null && popup.isVisible();
    }

    @Override
    public @Nullable UIAccess getUIAccess() {
        Popup popup = myPopup;
        return popup == null ? null : popup.getUIAccess();
    }

    @Override
    public void setCaption(String title) {
    }

    @Override
    public @Nullable ListPopupStep getListStep() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public void handleSelect(boolean handleFinalChoices) {
        handleSelect(selectedNode(), handleFinalChoices, false);
    }

    @Override
    @RequiredUIAccess
    public void handleSelect(boolean handleFinalChoices, InputEvent e) {
        handleSelect(selectedNode(), handleFinalChoices, false);
    }

    @Override
    public void setHandleAutoSelectionBeforeShow(boolean autoHandle) {
    }

    @Override
    public void addListSelectionListener(ListSelectionListener listSelectionListener) {
    }

    @Override
    public void addSelectionListener(Consumer<Object> selectionListener) {
        mySelectionListeners.add(selectionListener);
    }

    @Override
    public @Nullable Object getSelectedValue() {
        TreeNode<Object> node = selectedNode();
        return node == null ? null : node.getValue();
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        TextBox searchField = mySearchField;
        return searchField == null ? null : searchField.getValue();
    }
}
