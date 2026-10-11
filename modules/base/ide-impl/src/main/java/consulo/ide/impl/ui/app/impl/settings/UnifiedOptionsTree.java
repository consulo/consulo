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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.configurable.Configurable;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.KeyPressedEvent;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class UnifiedOptionsTree implements OptionsEditorColleague, Disposable {
    private static final long SELECTION_DELAY = 150;

    private final OptionsEditorContext myContext;
    private final Tree<Configurable> myTree;

    private @Nullable Configurable myQueuedConfigurable;
    private @Nullable ScheduledFuture<?> myQueuedSelection;

    private @Nullable List<List<Configurable>> myExpandedBeforeFilter;
    private boolean myWasHoldingFilter;
    private boolean myRefiltering;
    private boolean myDisposed;

    @RequiredUIAccess
    public UnifiedOptionsTree(
        ApplicationTreeExecutorFactory executorFactory,
        Supplier<Configurable[]> configurables,
        OptionsEditorContext context,
        Predicate<Configurable> filter,
        Function<Configurable, @Nullable LocalizeValue> projectTooltip
    ) {
        myContext = context;

        myTree = Tree.create(
            null,
            new UnifiedOptionsTreeModel(configurables, filter, projectTooltip),
            executorFactory.forBackgroundThreadWithoutReadAction(this)
        );
        myTree.setSpeedSearchConverter(null);
        myTree.addSelectListener(event -> {
            TreeNode<Configurable> node = event.getValue();
            onTreeSelected(node == null ? null : node.getValue());
        });
        myTree.addExpandListener(event -> invalidateExpansions());
        myTree.addCollapseListener(event -> invalidateExpansions());

        Disposer.register(this, myTree.destroyHook());
    }

    public Tree<Configurable> getTree() {
        return myTree;
    }

    public Component getComponent() {
        return myTree;
    }

    public void addKeyListener(Consumer<KeyPressedEvent> listener) {
        myTree.addKeyPressedListener(listener::accept);
    }

    @RequiredUIAccess
    public CompletableFuture<?> rebuild() {
        UIAccess uiAccess = UIAccess.current();
        return myTree.refreshAll().thenComposeAsync(ignored -> {
            TreeNode<Configurable> root = myTree.getRootNode();
            return root == null ? CompletableFuture.completedFuture(null) : root.findChild(value -> true);
        }, uiAccess);
    }

    @RequiredUIAccess
    public CompletableFuture<?> refilter() {
        UIAccess uiAccess = UIAccess.current();

        boolean holding = myContext.isHoldingFilter();

        List<List<Configurable>> toRestore = null;
        if (holding && !myWasHoldingFilter && myExpandedBeforeFilter == null) {
            myExpandedBeforeFilter = getExpandedValuePaths();
        }
        else if (!holding && myWasHoldingFilter && myExpandedBeforeFilter != null) {
            toRestore = myExpandedBeforeFilter;
            myExpandedBeforeFilter = null;
        }

        myWasHoldingFilter = holding;
        myRefiltering = true;

        List<List<Configurable>> restore = toRestore;
        return myTree.refreshAll()
            .thenComposeAsync(ignored -> afterRefilter(holding, restore), uiAccess)
            .whenCompleteAsync((value, error) -> myRefiltering = false, uiAccess);
    }

    @RequiredUIAccess
    private CompletableFuture<?> afterRefilter(boolean holding, @Nullable List<List<Configurable>> restore) {
        if (myDisposed) {
            return CompletableFuture.completedFuture(null);
        }

        if (holding) {
            return myTree.isExpandCollapseAllSupported() ? myTree.expandAll(Integer.MAX_VALUE) : CompletableFuture.completedFuture(null);
        }

        if (restore != null) {
            return restoreExpanded(restore);
        }

        return CompletableFuture.completedFuture(null);
    }

    @RequiredUIAccess
    public CompletableFuture<?> select(@Nullable Configurable configurable) {
        return select(configurable, false);
    }

    @RequiredUIAccess
    public CompletableFuture<?> selectOrFirst(@Nullable Configurable configurable) {
        return configurable == null ? selectFirstShown() : select(configurable, true);
    }

    @RequiredUIAccess
    private CompletableFuture<?> selectFirstShown() {
        TreeNode<Configurable> root = myTree.getRootNode();
        if (root == null) {
            return CompletableFuture.completedFuture(null);
        }

        UIAccess uiAccess = UIAccess.current();
        return root.findChild(value -> true).thenComposeAsync(this::selectFirst, uiAccess);
    }

    @RequiredUIAccess
    private CompletableFuture<?> select(@Nullable Configurable configurable, boolean firstIfHidden) {
        cancelQueuedSelection();
        myQueuedConfigurable = configurable;

        if (configurable == null) {
            return myContext.fireSelected(null, this);
        }

        UIAccess uiAccess = UIAccess.current();
        return findNode(path(configurable)).thenComposeAsync(node -> selectNode(configurable, node, firstIfHidden), uiAccess);
    }

    @RequiredUIAccess
    private CompletableFuture<?> selectNode(Configurable configurable, @Nullable TreeNode<Configurable> node, boolean firstIfHidden) {
        if (myDisposed || myQueuedConfigurable != configurable) {
            return CompletableFuture.failedFuture(new CancellationException());
        }

        if (node == null) {
            return firstIfHidden ? selectFirstShown() : CompletableFuture.failedFuture(new CancellationException());
        }

        CompletableFuture<Void> selected = myContext.fireSelected(configurable, this);
        myTree.select(node);
        return selected;
    }

    @RequiredUIAccess
    private CompletableFuture<?> selectFirst(@Nullable TreeNode<Configurable> node) {
        Configurable value = node == null ? null : node.getValue();
        if (myDisposed || value == null) {
            return CompletableFuture.completedFuture(null);
        }
        return select(value, false);
    }

    @RequiredUIAccess
    public void moveSelection(boolean down) {
        List<TreeNode<Configurable>> rows = getVisibleRows();
        if (rows.isEmpty()) {
            return;
        }

        TreeNode<Configurable> selected = myTree.getSelectedNode();
        Configurable selectedValue = selected == null ? null : selected.getValue();

        int index = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getValue() == selectedValue) {
                index = i;
                break;
            }
        }

        int target = index == -1 ? 0 : down ? index + 1 : index - 1;
        if (target < 0 || target >= rows.size()) {
            return;
        }

        myTree.select(rows.get(target));
    }

    @RequiredUIAccess
    private void onTreeSelected(@Nullable Configurable configurable) {
        if (configurable == null || configurable == myContext.getCurrentConfigurable() && myQueuedSelection == null) {
            return;
        }

        cancelQueuedSelection();
        myQueuedConfigurable = configurable;

        myQueuedSelection = UIAccess.current().getScheduler().schedule(() -> {
            myQueuedSelection = null;

            if (myDisposed || myQueuedConfigurable != configurable || myContext.getCurrentConfigurable() == configurable) {
                return;
            }

            myContext.fireSelected(configurable, this);
        }, SELECTION_DELAY, TimeUnit.MILLISECONDS);
    }

    private void cancelQueuedSelection() {
        ScheduledFuture<?> queued = myQueuedSelection;
        if (queued != null) {
            queued.cancel(false);
            myQueuedSelection = null;
        }
    }

    private void invalidateExpansions() {
        if (!myRefiltering) {
            myExpandedBeforeFilter = null;
        }
    }

    private List<Configurable> path(Configurable configurable) {
        List<Configurable> path = new ArrayList<>();
        for (Configurable each = configurable; each != null; each = myContext.getParentConfigurable(each)) {
            if (each == configurable || !UnifiedOptionsTreeModel.isInvisible(each)) {
                path.add(each);
            }
        }
        Collections.reverse(path);
        return path;
    }

    private CompletableFuture<@Nullable TreeNode<Configurable>> findNode(List<Configurable> path) {
        TreeNode<Configurable> root = myTree.getRootNode();

        CompletableFuture<@Nullable TreeNode<Configurable>> result = CompletableFuture.completedFuture(root);
        for (Configurable each : path) {
            result = result.thenCompose(node -> findChild(node, each));
        }
        return result;
    }

    private static CompletableFuture<@Nullable TreeNode<Configurable>> findChild(@Nullable TreeNode<Configurable> node, Configurable value) {
        if (node == null) {
            return CompletableFuture.completedFuture(null);
        }
        return node.findChild(each -> each == value);
    }

    @RequiredUIAccess
    private CompletableFuture<?> restoreExpanded(List<List<Configurable>> paths) {
        UIAccess uiAccess = UIAccess.current();

        CompletableFuture<?> result = myTree.collapseAll();
        for (List<Configurable> path : paths) {
            result = result.thenComposeAsync(ignored -> findNode(path), uiAccess).thenComposeAsync(this::expand, uiAccess);
        }

        TreeNode<Configurable> selected = myTree.getSelectedNode();
        if (selected != null) {
            result = result.whenCompleteAsync((value, error) -> {
                if (!myDisposed) {
                    myTree.select(selected);
                }
            }, uiAccess);
        }
        return result;
    }

    private CompletableFuture<?> expand(@Nullable TreeNode<Configurable> node) {
        if (node == null || myDisposed) {
            return CompletableFuture.completedFuture(null);
        }
        return myTree.expand(node);
    }

    private List<List<Configurable>> getExpandedValuePaths() {
        List<List<Configurable>> result = new ArrayList<>();
        for (List<TreeNode<Configurable>> path : myTree.getExpandedPaths()) {
            List<Configurable> values = new ArrayList<>(path.size());
            for (TreeNode<Configurable> node : path) {
                Configurable value = node.getValue();
                if (value != null) {
                    values.add(value);
                }
            }

            if (!values.isEmpty()) {
                result.add(values);
            }
        }
        return result;
    }

    private List<TreeNode<Configurable>> getVisibleRows() {
        Map<Configurable, Boolean> expanded = new IdentityHashMap<>();
        for (List<TreeNode<Configurable>> path : myTree.getExpandedPaths()) {
            if (!path.isEmpty()) {
                Configurable value = path.getLast().getValue();
                if (value != null) {
                    expanded.put(value, Boolean.TRUE);
                }
            }
        }

        List<TreeNode<Configurable>> rows = new ArrayList<>();
        TreeNode<Configurable> root = myTree.getRootNode();
        if (root != null) {
            collectVisibleRows(root, expanded, rows);
        }
        return rows;
    }

    private static void collectVisibleRows(
        TreeNode<Configurable> node,
        Map<Configurable, Boolean> expanded,
        List<TreeNode<Configurable>> rows
    ) {
        for (TreeNode<Configurable> child : node.getLoadedChildren()) {
            rows.add(child);

            Configurable value = child.getValue();
            if (value != null && expanded.containsKey(value)) {
                collectVisibleRows(child, expanded, rows);
            }
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
        cancelQueuedSelection();
        myQueuedConfigurable = null;
    }
}
