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
package consulo.configurable;

import consulo.configurable.internal.FullContentConfigurable;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.Space;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public abstract class UnifiedMasterDetailsComponent implements Configurable, FullContentConfigurable {
    private static final Logger LOG = Logger.getInstance(UnifiedMasterDetailsComponent.class);

    private static final Object ROOT = new Object();

    private final Map<Object, List<Object>> myChildren = new IdentityHashMap<>();
    private final Map<Object, Object> myParents = new IdentityHashMap<>();
    private final Map<Object, @Nullable MasterDetailsConfigurable<?>> myConfigurables = new IdentityHashMap<>();
    private final Set<MasterDetailsConfigurable<?>> myInitializedConfigurables = new LinkedHashSet<>();
    private final Map<Object, TreeNode<Object>> myTreeNodes = new IdentityHashMap<>();

    private @Nullable Tree<Object> myTree;
    private @Nullable ActionToolbar myToolbar;
    private @Nullable DockLayout myDetailsPanel;
    private @Nullable DockLayout myNorthPanel;
    private @Nullable Component myBannerComponent;
    private @Nullable Disposable myUIDisposable;

    private volatile @Nullable Object mySelectedValue;
    private volatile List<Object> mySelectedValues = List.of();
    private @Nullable MasterDetailsConfigurable<?> myCurrentConfigurable;
    private boolean myHasDeletedItems;

    @RequiredUIAccess
    protected abstract void loadTree();

    @RequiredUIAccess
    protected abstract @Nullable MasterDetailsConfigurable<?> createConfigurable(Object value);

    protected abstract void renderValue(Object value, TextItemPresentation presentation);

    protected abstract String getValueText(Object value);

    protected @Nullable Comparator<Object> getValueComparator() {
        return null;
    }

    protected List<AnAction> createActions() {
        return List.of();
    }

    protected List<AnAction> createPopupActions() {
        return createActions();
    }

    protected void uiDataSnapshot(DataSink sink) {
    }

    protected LocalizeValue getEmptySelectionText() {
        return LocalizeValue.empty();
    }

    protected boolean isExpandAllByDefault() {
        return false;
    }

    protected boolean isMultipleSelectAllowed() {
        return false;
    }

    protected void processRemovedItems() {
    }

    protected boolean wasObjectStored(Object value) {
        return false;
    }

    @RequiredUIAccess
    protected void onItemDeleted(Object value) {
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentDisposable) {
        myUIDisposable = parentDisposable;

        Tree<Object> tree = Tree.create(new ValueTreeModel());
        tree.setSpeedSearchConverter(node -> {
            Object value = node.getValue();
            return value == null ? "" : getValueText(value);
        });
        tree.setAllowMultipleSelect(isMultipleSelectAllowed());
        tree.addSelectListener(event -> onNodeSelected(event.getValue()));
        tree.addContextMenuListener(this::showContextMenu);
        Disposer.register(parentDisposable, tree.destroyHook());
        myTree = tree;

        DockLayout masterPanel = DockLayout.create(Space.NONE);

        DockLayout northPanel = DockLayout.create(Space.NONE);
        Component bannerComponent = myBannerComponent;
        if (bannerComponent != null) {
            northPanel.top(bannerComponent);
        }
        masterPanel.top(northPanel);
        myNorthPanel = northPanel;

        List<AnAction> actions = createActions();
        if (!actions.isEmpty()) {
            ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
            for (AnAction action : actions) {
                group.add(action);
            }

            ActionToolbar toolbar = ActionToolbarFactory.getInstance()
                .createActionToolbar(ActionPlaces.UNKNOWN, group.build(), ActionToolbar.Style.HORIZONTAL);
            toolbar.setTargetUIComponent(tree);
            northPanel.center(toolbar.getUIComponent());
            myToolbar = toolbar;
        }

        masterPanel.center(ScrollableLayout.create(tree));

        DockLayout detailsPanel = DockLayout.create(Space.NONE);
        detailsPanel.paddingBuilder().allSet(Space.LARGE).topSet(Space.SMALL).apply();
        myDetailsPanel = detailsPanel;

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        splitLayout.setFirstComponent(masterPanel);
        splitLayout.setSecondComponent(detailsPanel);
        splitLayout.setProportion(30);
        splitLayout.putUserData(UiDataProvider.KEY, this::uiDataSnapshot);

        showEmpty();

        return splitLayout;
    }

    @RequiredUIAccess
    @Override
    public void setBannerComponent(Component bannerComponent) {
        myBannerComponent = bannerComponent;

        DockLayout northPanel = myNorthPanel;
        if (northPanel != null) {
            northPanel.top(bannerComponent);
        }
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedUIComponent() {
        return myTree;
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        clearValues();
        myHasDeletedItems = false;
        showEmpty();

        loadTree();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        if (myHasDeletedItems) {
            return true;
        }

        for (MasterDetailsConfigurable<?> configurable : myInitializedConfigurables) {
            if (configurable.isModified()) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        processRemovedItems();

        for (MasterDetailsConfigurable<?> configurable : new ArrayList<>(myInitializedConfigurables)) {
            if (configurable.isModified()) {
                configurable.apply();
            }
        }

        myHasDeletedItems = false;
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        clearValues();

        myTree = null;
        myToolbar = null;
        myDetailsPanel = null;
        myNorthPanel = null;
        myUIDisposable = null;
        mySelectedValue = null;
        mySelectedValues = List.of();
    }

    @Override
    public @Nullable String getHelpTopic() {
        MasterDetailsConfigurable<?> configurable = myCurrentConfigurable;
        return configurable != null ? configurable.getHelpTopic() : null;
    }

    public @Nullable Tree<Object> getTree() {
        return myTree;
    }

    public @Nullable Object getSelectedValue() {
        return mySelectedValue;
    }

    public List<Object> getSelectedValues() {
        return mySelectedValues;
    }

    public @Nullable MasterDetailsConfigurable<?> getSelectedConfigurable() {
        return myCurrentConfigurable;
    }

    protected void addValue(@Nullable Object parent, Object value) {
        Object parentKey = parent == null ? ROOT : parent;
        myChildren.computeIfAbsent(parentKey, key -> new ArrayList<>()).add(value);
        myParents.put(value, parentKey);
    }

    public List<Object> getChildren(@Nullable Object parent) {
        List<Object> children = myChildren.get(parent == null ? ROOT : parent);
        return children == null ? List.of() : List.copyOf(children);
    }

    public @Nullable Object getParent(Object value) {
        Object parent = myParents.get(value);
        return parent == ROOT ? null : parent;
    }

    public boolean isAttached(Object value) {
        return myParents.containsKey(value);
    }

    public @Nullable Object findValue(Predicate<Object> condition) {
        for (List<Object> children : myChildren.values()) {
            for (Object value : children) {
                if (condition.test(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    @RequiredUIAccess
    public @Nullable MasterDetailsConfigurable<?> getConfigurable(Object value) {
        MasterDetailsConfigurable<?> configurable = myConfigurables.get(value);
        if (configurable == null && !myConfigurables.containsKey(value)) {
            configurable = createConfigurable(value);
            myConfigurables.put(value, configurable);
        }
        return configurable;
    }

    @RequiredUIAccess
    protected void removeValue(Object value) {
        for (Object child : getChildren(value)) {
            removeValue(child);
        }

        Object parent = myParents.remove(value);
        if (parent != null) {
            List<Object> siblings = myChildren.get(parent);
            if (siblings != null) {
                siblings.remove(value);
            }
        }
        myChildren.remove(value);
        myTreeNodes.remove(value);

        MasterDetailsConfigurable<?> configurable = myConfigurables.remove(value);
        if (configurable != null) {
            myInitializedConfigurables.remove(configurable);
            configurable.disposeUIResources();
        }

        myHasDeletedItems |= wasObjectStored(value);

        if (mySelectedValues.contains(value)) {
            List<Object> selectedValues = new ArrayList<>(mySelectedValues);
            selectedValues.remove(value);
            mySelectedValues = List.copyOf(selectedValues);
        }

        if (value == mySelectedValue) {
            mySelectedValue = null;
            updateSelection(null);
        }

        onItemDeleted(value);
    }

    @RequiredUIAccess
    protected CompletableFuture<?> removeSelected() {
        List<Object> selected = new ArrayList<>();
        for (Object value : mySelectedValues) {
            if (isAttached(value)) {
                selected.add(value);
            }
        }
        if (selected.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Object anchor = selected.get(0);
        Object anchorParent = getParent(anchor);
        int index = getSortedChildren(anchorParent).indexOf(anchor);

        List<Object> parents = new ArrayList<>();
        for (Object value : selected) {
            if (!isAttached(value)) {
                continue;
            }

            Object parent = getParent(value);
            if (parent != null && !parents.contains(parent)) {
                parents.add(parent);
            }
            removeValue(value);
        }

        for (Object parent : parents) {
            Object current = parent;
            while (current != null && isAttached(current) && getChildren(current).isEmpty() && isRemovedWhenEmpty(current)) {
                Object grandParent = getParent(current);
                if (current == anchorParent) {
                    anchorParent = grandParent;
                    index = getSortedChildren(grandParent).indexOf(current);
                }
                removeValue(current);
                current = grandParent;
            }
        }

        List<Object> left = getSortedChildren(anchorParent);
        Object toSelect = left.isEmpty() ? anchorParent : left.get(Math.max(0, Math.min(index, left.size() - 1)));
        return refreshTree(toSelect);
    }

    protected boolean isRemovedWhenEmpty(Object value) {
        return false;
    }

    private List<Object> getSortedChildren(@Nullable Object parent) {
        List<Object> children = new ArrayList<>(getChildren(parent));
        Comparator<Object> comparator = getValueComparator();
        if (comparator != null) {
            children.sort(comparator);
        }
        return children;
    }

    @RequiredUIAccess
    public void refreshValue(Object value) {
        Tree<Object> tree = myTree;
        TreeNode<Object> node = myTreeNodes.get(value);
        if (tree != null && node != null) {
            tree.refreshItem(node);
        }
    }

    @RequiredUIAccess
    public CompletableFuture<?> refreshTree(@Nullable Object valueToSelect) {
        Tree<Object> tree = myTree;
        if (tree == null) {
            return CompletableFuture.completedFuture(null);
        }

        myTreeNodes.clear();

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture<Object> result = new CompletableFuture<>();
        tree.refreshAll().whenCompleteAsync((ignored, error) -> {
            if (myTree != tree) {
                result.complete(null);
                return;
            }

            CompletableFuture<?> expand = isExpandAllByDefault() ? tree.expandAll() : CompletableFuture.completedFuture(null);
            expand.whenCompleteAsync((expanded, expandError) -> {
                if (valueToSelect != null && isAttached(valueToSelect)) {
                    select(valueToSelect).whenComplete((selected, selectError) -> result.complete(null));
                }
                else {
                    result.complete(null);
                }
            }, uiAccess);
        }, uiAccess);
        return result;
    }

    @RequiredUIAccess
    public CompletableFuture<?> reloadTree(@Nullable Object valueToSelect) {
        Map<Object, @Nullable MasterDetailsConfigurable<?>> configurables = new IdentityHashMap<>(myConfigurables);
        Object selectedValue = mySelectedValue;
        List<Object> selectedValues = mySelectedValues;
        Object selectedParent = selectedValue == null ? null : getParent(selectedValue);
        int selectedIndex = selectedValue == null ? -1 : getSortedChildren(selectedParent).indexOf(selectedValue);

        myChildren.clear();
        myParents.clear();
        myTreeNodes.clear();
        myConfigurables.clear();

        loadTree();

        for (Map.Entry<Object, @Nullable MasterDetailsConfigurable<?>> entry : configurables.entrySet()) {
            Object value = entry.getKey();
            MasterDetailsConfigurable<?> configurable = entry.getValue();
            if (isAttached(value)) {
                myConfigurables.put(value, configurable);
            }
            else if (configurable != null) {
                myInitializedConfigurables.remove(configurable);
                configurable.disposeUIResources();
            }
        }

        Object toSelect = valueToSelect != null ? valueToSelect : selectedValue;
        if (selectedValue != null && !isAttached(selectedValue)) {
            mySelectedValue = null;
            mySelectedValues = List.of();
            updateSelection(null);

            if (valueToSelect == null) {
                toSelect = findReplacement(selectedValues, selectedParent, selectedIndex);
            }
        }

        return refreshTree(toSelect);
    }

    private @Nullable Object findReplacement(List<Object> selectedValues, @Nullable Object parent, int index) {
        for (Object value : selectedValues) {
            if (isAttached(value)) {
                return value;
            }
        }

        Object current = parent;
        while (current != null && !isAttached(current)) {
            current = getParent(current);
        }

        List<Object> siblings = getSortedChildren(current);
        if (siblings.isEmpty()) {
            return current;
        }
        return siblings.get(Math.max(0, Math.min(index, siblings.size() - 1)));
    }

    @RequiredUIAccess
    public CompletableFuture<?> select(@Nullable Object value) {
        Tree<Object> tree = myTree;
        if (tree == null || value == null) {
            return CompletableFuture.completedFuture(null);
        }

        TreeNode<Object> rootNode = tree.getRootNode();
        if (rootNode == null) {
            return CompletableFuture.completedFuture(null);
        }

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture<Object> result = new CompletableFuture<>();
        rootNode.findChildDeep(candidate -> candidate == value).whenCompleteAsync((node, error) -> {
            if (node != null && myTree == tree) {
                tree.select(node);
                if (mySelectedValue != node.getValue()) {
                    onNodeSelected(node);
                }
            }
            result.complete(null);
        }, uiAccess);
        return result;
    }

    @RequiredUIAccess
    protected void updateActions() {
        ActionToolbar toolbar = myToolbar;
        if (toolbar != null) {
            toolbar.updateActionsAsync();
        }
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        Tree<Object> tree = myTree;
        List<AnAction> actions = createPopupActions();
        if (tree == null || actions.isEmpty()) {
            return;
        }

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        for (AnAction action : actions) {
            group.add(action);
        }

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group.build());
        menu.setTargetComponent(tree);
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    @RequiredUIAccess
    private void onNodeSelected(@Nullable TreeNode<Object> node) {
        Object value = node == null ? null : node.getValue();
        if (value != null && !isAttached(value)) {
            value = null;
        }

        mySelectedValue = value;
        mySelectedValues = collectSelectedValues(value);

        updateSelection(value == null ? null : getConfigurable(value));
        updateActions();
    }

    @RequiredUIAccess
    private List<Object> collectSelectedValues(@Nullable Object lead) {
        List<Object> values = new ArrayList<>();
        Tree<Object> tree = myTree;
        if (tree != null) {
            for (TreeNode<Object> node : tree.getSelectedNodes()) {
                Object value = node.getValue();
                if (value != null && isAttached(value) && !values.contains(value)) {
                    values.add(value);
                }
            }
        }

        if (lead != null && !values.contains(lead)) {
            values.add(lead);
        }
        return List.copyOf(values);
    }

    @RequiredUIAccess
    protected void updateSelection(@Nullable MasterDetailsConfigurable<?> configurable) {
        myCurrentConfigurable = configurable;

        DockLayout detailsPanel = myDetailsPanel;
        Disposable uiDisposable = myUIDisposable;
        if (detailsPanel == null || uiDisposable == null) {
            return;
        }

        if (configurable == null) {
            showEmpty();
            return;
        }

        Component component = configurable.createUIComponent(uiDisposable);
        if (component == null) {
            LOG.error("createUIComponent() returned null. configurable=" + configurable);
            showEmpty();
            return;
        }

        detailsPanel.center(component);

        ensureInitialized(configurable);
    }

    @RequiredUIAccess
    public void ensureInitialized(MasterDetailsConfigurable<?> configurable) {
        if (!myInitializedConfigurables.contains(configurable)) {
            configurable.initialize();
            configurable.reset();
            myInitializedConfigurables.add(configurable);
        }
    }

    public boolean isInitialized(MasterDetailsConfigurable<?> configurable) {
        return myInitializedConfigurables.contains(configurable);
    }

    @RequiredUIAccess
    private void showEmpty() {
        DockLayout detailsPanel = myDetailsPanel;
        if (detailsPanel == null) {
            return;
        }

        Label emptyLabel = Label.create(getEmptySelectionText(), LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build());
        detailsPanel.center(emptyLabel);
    }

    @RequiredUIAccess
    private void clearValues() {
        for (MasterDetailsConfigurable<?> configurable : myConfigurables.values()) {
            if (configurable != null) {
                configurable.disposeUIResources();
            }
        }

        myConfigurables.clear();
        myInitializedConfigurables.clear();
        myChildren.clear();
        myParents.clear();
        myTreeNodes.clear();
        myCurrentConfigurable = null;
        mySelectedValue = null;
        mySelectedValues = List.of();
    }

    private class ValueTreeModel implements TreeModel<Object> {
        @Override
        public void buildChildren(Function<Object, TreeNode<Object>> nodeFactory, @Nullable Object parentValue) {
            for (Object child : getChildren(parentValue)) {
                TreeNode<Object> node = nodeFactory.apply(child);
                node.setLeaf(getChildren(child).isEmpty());
                node.setRenderer(UnifiedMasterDetailsComponent.this::renderValue);
                myTreeNodes.put(child, node);
            }
        }

        @Override
        public @Nullable Comparator<TreeNode<Object>> getNodeComparator() {
            Comparator<Object> comparator = getValueComparator();
            if (comparator == null) {
                return null;
            }
            return (o1, o2) -> {
                Object v1 = o1.getValue();
                Object v2 = o2.getValue();
                if (v1 == null || v2 == null) {
                    return 0;
                }
                return comparator.compare(v1, v2);
            };
        }
    }
}
