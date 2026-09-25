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
package consulo.ui.impl.tree;

import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TreeCollapseEvent;
import consulo.ui.event.TreeDoubleClickEvent;
import consulo.ui.event.TreeExpandEvent;
import consulo.ui.event.TreeSelectEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ProgrammaticInputDetails;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public final class TreeController<E> implements Disposable {
    public static final int EXPAND_ALL_DEPTH = 4;

    private static final Logger LOG = Logger.getInstance(TreeController.class);

    private final Tree<E> myTree;
    private final TreeModel<E> myModel;
    private final TreeExecutor myExecutor;
    private final TreeWidget<E> myWidget;
    private final TreeNodeImpl<E> myRoot;

    private volatile @Nullable TreeNodeImpl<E> mySelected;
    private volatile boolean myDisposed;

    private final Map<TreeNodeImpl<E>, CompletableFuture<List<TreeNodeImpl<E>>>> myDeferred = new LinkedHashMap<>();
    private final List<Runnable> myQueued = new ArrayList<>();

    private final Object myIdleLock = new Object();
    private int myPending;
    private List<CompletableFuture<Void>> myIdleWaiters = new ArrayList<>();

    public TreeController(Tree<E> tree, @Nullable E rootValue, TreeModel<E> model, TreeExecutor executor, TreeWidget<E> widget) {
        myTree = tree;
        myModel = model;
        myExecutor = executor;
        myWidget = widget;
        myRoot = new TreeNodeImpl<>(this, null, rootValue);
    }

    public TreeNodeImpl<E> getRoot() {
        return myRoot;
    }

    public @Nullable TreeNodeImpl<E> getSelected() {
        TreeNodeImpl<E> selected = mySelected;
        return selected == null || selected.myRemoved ? null : selected;
    }

    public @Nullable TreeNodeImpl<E> toNode(@Nullable TreeNode<E> node) {
        if (!(node instanceof TreeNodeImpl<E> impl) || !impl.belongsTo(this)) {
            return null;
        }

        TreeNodeImpl<E> resolved = impl.resolve();
        return resolved.myRemoved ? null : resolved;
    }

    @RequiredUIAccess
    public void bind() {
        drainQueued();
        restartDeferred();

        project(myRoot);

        TreeNodeImpl<E> selected = getSelected();
        if (selected != null) {
            myWidget.setSelected(selected);
        }

        loadChildren(myRoot);
    }

    private void restartDeferred() {
        Map<TreeNodeImpl<E>, CompletableFuture<List<TreeNodeImpl<E>>>> deferred;
        synchronized (myDeferred) {
            deferred = new LinkedHashMap<>(myDeferred);
            myDeferred.clear();
        }
        deferred.forEach(this::restart);
    }

    private void restart(TreeNodeImpl<E> node, CompletableFuture<List<TreeNodeImpl<E>>> result) {
        int epoch;
        boolean firstLoad;
        boolean current;
        CompletableFuture<List<TreeNodeImpl<E>>> newer;
        synchronized (node) {
            newer = node.myChildrenFuture;
            current = newer == result && !myDisposed && !node.myRemoved;
            epoch = node.myEpoch;
            firstLoad = !node.myLoaded;
        }

        if (current) {
            start(node, epoch, firstLoad, result);
        }
        else {
            follow(node, result, newer);
        }
    }

    @RequiredUIAccess
    private void project(TreeNodeImpl<E> node) {
        if (node.myLoading) {
            myWidget.showLoading(node);
        }

        if (!node.myLoaded) {
            return;
        }

        List<TreeNodeImpl<E>> children = node.myChildren;
        myWidget.setChildren(node, children);
        for (TreeNodeImpl<E> child : children) {
            project(child);
        }

        if (node != myRoot && node.myExpanded) {
            myWidget.setExpanded(node, true);
        }
    }

    public CompletableFuture<List<TreeNodeImpl<E>>> loadChildren(TreeNodeImpl<E> node) {
        CompletableFuture<List<TreeNodeImpl<E>>> future;
        int epoch;
        boolean firstLoad;
        synchronized (node) {
            if (myDisposed || node.myRemoved) {
                return CompletableFuture.completedFuture(List.of());
            }

            CompletableFuture<List<TreeNodeImpl<E>>> current = node.myChildrenFuture;
            if (current != null && !node.myOutdated) {
                return current;
            }

            if (current == null && node.myLeaf) {
                return CompletableFuture.completedFuture(List.of());
            }

            future = new CompletableFuture<>();
            node.myChildrenFuture = future;
            node.myOutdated = false;
            epoch = ++node.myEpoch;
            firstLoad = !node.myLoaded;
        }

        build(node, epoch, firstLoad, future);
        return future;
    }

    private void build(TreeNodeImpl<E> node, int epoch, boolean firstLoad, CompletableFuture<List<TreeNodeImpl<E>>> result) {
        track(result);

        start(node, epoch, firstLoad, result);
    }

    private void start(TreeNodeImpl<E> node, int epoch, boolean firstLoad, CompletableFuture<List<TreeNodeImpl<E>>> result) {
        if (!hasUI()) {
            CompletableFuture<List<TreeNodeImpl<E>>> previous;
            synchronized (myDeferred) {
                previous = myDeferred.put(node, result);
            }

            if (previous != null && previous != result) {
                follow(node, previous, result);
            }

            if (hasUI()) {
                runOnUI(this::restartDeferred);
            }
            return;
        }

        if (firstLoad) {
            runOnUI(() -> showLoading(node, epoch));
        }

        myExecutor.execute(myTree, () -> fetchChildren(node)).whenComplete((children, error) -> runOnUI(() -> {
            try {
                completeBuild(node, epoch, result, children, error);
            }
            catch (Throwable e) {
                LOG.error(e);
                result.complete(List.of());
            }
        }));
    }

    private List<TreeNodeImpl<E>> fetchChildren(TreeNodeImpl<E> parent) {
        List<TreeNodeImpl<E>> children = new ArrayList<>();
        myModel.buildChildren(value -> {
            TreeNodeImpl<E> child = new TreeNodeImpl<>(this, parent, value);
            children.add(child);
            return child;
        }, parent.myValue);

        Comparator<TreeNode<E>> comparator = myModel.getNodeComparator();
        if (comparator != null) {
            children.sort(comparator);
        }

        for (TreeNodeImpl<E> child : children) {
            child.computePresentation(myWidget.createPresentation());
            child.myPrebuild = !child.myLeaf && myModel.isNeedBuildChildrenBeforeOpen(child);
        }
        return children;
    }

    @RequiredUIAccess
    private void showLoading(TreeNodeImpl<E> node, int epoch) {
        synchronized (node) {
            if (node.myEpoch != epoch || node.myLoaded) {
                return;
            }
        }

        if (myDisposed || node.myRemoved || node.myLoading) {
            return;
        }

        node.myLoading = true;
        myWidget.showLoading(node);
    }

    @RequiredUIAccess
    private void hideLoading(TreeNodeImpl<E> node) {
        if (!node.myLoading) {
            return;
        }

        node.myLoading = false;
        myWidget.hideLoading(node);
    }

    @RequiredUIAccess
    private void completeBuild(TreeNodeImpl<E> node,
                               int epoch,
                               CompletableFuture<List<TreeNodeImpl<E>>> result,
                               @Nullable List<TreeNodeImpl<E>> built,
                               @Nullable Throwable error) {
        boolean stale;
        CompletableFuture<List<TreeNodeImpl<E>>> newer;
        synchronized (node) {
            stale = node.myEpoch != epoch;
            newer = node.myChildrenFuture;
        }

        if (stale) {
            follow(node, result, newer);
            return;
        }

        if (myDisposed) {
            result.complete(List.of());
            return;
        }

        hideLoading(node);

        if (error != null || built == null) {
            logBuildError(error);

            synchronized (node) {
                node.myChildrenFuture = node.myLoaded ? CompletableFuture.completedFuture(node.myChildren) : null;
                node.myOutdated = node.myLoaded;
            }
            result.complete(node.myChildren);
            return;
        }

        if (myDisposed || node.myRemoved) {
            result.complete(List.of());
            return;
        }

        List<TreeNodeImpl<E>> applied = List.copyOf(node.myLoaded ? merge(node.myChildren, built) : built);
        boolean collapse = applied.isEmpty() && node.myExpanded;
        synchronized (node) {
            node.myChildren = applied;
            node.myLoaded = true;
            node.myLeaf = applied.isEmpty();
            if (collapse) {
                node.myExpanded = false;
            }
        }

        myWidget.setChildren(node, applied);
        if (node != myRoot) {
            if (collapse) {
                myWidget.setExpanded(node, false);
            }
            myWidget.update(node);
        }

        List<CompletableFuture<?>> cascade = new ArrayList<>();
        for (TreeNodeImpl<E> child : applied) {
            if (child.myExpanded && needsLoad(child)) {
                cascade.add(loadChildren(child));
            }
        }

        if (isShown(node)) {
            prebuild(applied);
        }

        if (cascade.isEmpty()) {
            result.complete(applied);
        }
        else {
            CompletableFuture.allOf(cascade.toArray(CompletableFuture[]::new)).whenComplete((ignored, e) -> result.complete(applied));
        }
    }

    private void follow(TreeNodeImpl<E> node,
                        CompletableFuture<List<TreeNodeImpl<E>>> result,
                        @Nullable CompletableFuture<List<TreeNodeImpl<E>>> newer) {
        if (newer == null || newer == result) {
            result.complete(node.myChildren);
            return;
        }

        newer.whenComplete((children, error) -> result.complete(children == null ? node.myChildren : children));
    }

    @RequiredUIAccess
    private List<TreeNodeImpl<E>> merge(List<TreeNodeImpl<E>> old, List<TreeNodeImpl<E>> built) {
        Map<E, TreeNodeImpl<E>> byValue = new HashMap<>();
        for (TreeNodeImpl<E> child : old) {
            E value = child.myValue;
            if (value != null) {
                byValue.putIfAbsent(value, child);
            }
        }

        List<TreeNodeImpl<E>> merged = new ArrayList<>(built.size());
        for (TreeNodeImpl<E> fresh : built) {
            E value = fresh.myValue;
            TreeNodeImpl<E> reused = value == null ? null : byValue.remove(value);
            if (reused == null) {
                merged.add(fresh);
            }
            else {
                reuse(reused, fresh);
                merged.add(reused);
            }
        }

        Set<TreeNodeImpl<E>> kept = new HashSet<>(merged);
        for (TreeNodeImpl<E> child : old) {
            if (!kept.contains(child)) {
                remove(child);
            }
        }
        return merged;
    }

    @RequiredUIAccess
    private void reuse(TreeNodeImpl<E> reused, TreeNodeImpl<E> fresh) {
        reused.takeOver(fresh);

        if (!fresh.myLeaf) {
            synchronized (reused) {
                reused.myLeaf = false;
                if (reused.myChildrenFuture != null) {
                    reused.myOutdated = true;
                }
            }
            return;
        }

        List<TreeNodeImpl<E>> dropped;
        boolean wasExpanded;
        synchronized (reused) {
            dropped = reused.myChildren;
            wasExpanded = reused.myExpanded;
            reused.myChildren = List.of();
            reused.myChildrenFuture = null;
            reused.myLoaded = false;
            reused.myOutdated = false;
            reused.myEpoch++;
            reused.myLeaf = true;
            reused.myExpanded = false;
        }

        for (TreeNodeImpl<E> child : dropped) {
            remove(child);
        }

        hideLoading(reused);
        myWidget.setChildren(reused, List.of());
        if (wasExpanded) {
            myWidget.setExpanded(reused, false);
        }
    }

    @RequiredUIAccess
    private void remove(TreeNodeImpl<E> node) {
        node.myRemoved = true;
        if (mySelected == node) {
            mySelected = null;
        }

        for (TreeNodeImpl<E> child : node.myChildren) {
            remove(child);
        }
    }

    private boolean needsLoad(TreeNodeImpl<E> node) {
        synchronized (node) {
            return node.myChildrenFuture == null || node.myOutdated;
        }
    }

    private boolean isShown(TreeNodeImpl<E> node) {
        for (TreeNodeImpl<E> current = node; current != myRoot; current = current.getParent()) {
            if (current == null || current.myRemoved || !current.myExpanded) {
                return false;
            }
        }
        return true;
    }

    @RequiredUIAccess
    private void prebuild(List<TreeNodeImpl<E>> children) {
        for (TreeNodeImpl<E> child : children) {
            if (child.myPrebuild && !child.myLeaf && needsLoad(child)) {
                loadChildren(child);
            }
        }
    }

    public CompletableFuture<?> expand(TreeNode<E> handle, int depth) {
        TreeNodeImpl<E> node = toNode(handle);
        if (node == null) {
            return CompletableFuture.completedFuture(null);
        }

        return track(reveal(node).thenCompose(ignored -> expandDeep(node, depth)));
    }

    public CompletableFuture<?> expandAll() {
        return expandAll(EXPAND_ALL_DEPTH);
    }

    public CompletableFuture<?> expandAll(int depth) {
        return track(expandDeep(myRoot, depth == Integer.MAX_VALUE ? depth : depth + 1));
    }

    private CompletableFuture<?> reveal(TreeNodeImpl<E> node) {
        List<TreeNodeImpl<E>> path = pathTo(node);

        CompletableFuture<?> reveal = CompletableFuture.completedFuture(null);
        for (int i = 0; i < path.size() - 1; i++) {
            TreeNodeImpl<E> ancestor = path.get(i);
            reveal = reveal.thenCompose(ignored -> loadChildren(ancestor))
                .thenCompose(children -> open(ancestor, ProgrammaticInputDetails.INSTANCE));
        }
        return reveal;
    }

    private CompletableFuture<?> expandDeep(TreeNodeImpl<E> node, int depth) {
        if (depth <= 0 || node.myLeaf || node.myRemoved) {
            return CompletableFuture.completedFuture(null);
        }

        return loadChildren(node)
            .thenCompose(children -> open(node, ProgrammaticInputDetails.INSTANCE).thenApply(ignored -> children))
            .thenCompose(children -> CompletableFuture.allOf(
                children.stream().map(child -> expandDeep(child, depth - 1)).toArray(CompletableFuture[]::new)
            ));
    }

    private CompletableFuture<?> open(TreeNodeImpl<E> node, InputDetails details) {
        return onUI(() -> {
            if (node == myRoot || node.myRemoved || node.myExpanded || node.myChildren.isEmpty()) {
                return;
            }

            node.myExpanded = true;
            myWidget.setExpanded(node, true);
            fireExpand(node, details);

            if (isShown(node)) {
                prebuild(node.myChildren);
            }
        });
    }

    public CompletableFuture<?> collapseAll() {
        return track(onUI(() -> collapseBelow(myRoot)));
    }

    @RequiredUIAccess
    private void collapseBelow(TreeNodeImpl<E> node) {
        for (TreeNodeImpl<E> child : node.myChildren) {
            collapseBelow(child);
            close(child, ProgrammaticInputDetails.INSTANCE, true);
        }
    }

    public CompletableFuture<?> select(TreeNode<E> handle) {
        TreeNodeImpl<E> node = toNode(handle);
        if (node == null || node == myRoot) {
            return CompletableFuture.completedFuture(null);
        }

        return track(reveal(node).thenCompose(ignored -> onUI(() -> {
            if (!node.myRemoved) {
                setSelection(node, ProgrammaticInputDetails.INSTANCE, true);
            }
        })));
    }

    public CompletableFuture<?> refreshItem(TreeNode<E> handle, boolean refreshChildren) {
        TreeNodeImpl<E> node = toNode(handle);
        if (node == null) {
            return CompletableFuture.completedFuture(null);
        }

        return track(refresh(node, refreshChildren));
    }

    public CompletableFuture<?> refreshAll() {
        return track(refresh(myRoot, true));
    }

    private CompletableFuture<?> refresh(TreeNodeImpl<E> node, boolean refreshChildren) {
        CompletableFuture<?> presentation = node == myRoot ? CompletableFuture.completedFuture(null) : refreshPresentation(node);
        if (!refreshChildren) {
            return presentation;
        }

        synchronized (node) {
            if (node.myRemoved) {
                return presentation;
            }

            if (node.myChildrenFuture != null) {
                node.myOutdated = true;
            }
            else if (node.myLeaf) {
                node.myLeaf = false;
            }
            else if (!isShown(node)) {
                return presentation;
            }
        }

        return CompletableFuture.allOf(presentation, loadChildren(node));
    }

    private CompletableFuture<?> refreshPresentation(TreeNodeImpl<E> node) {
        CompletableFuture<Object> done = new CompletableFuture<>();

        myExecutor.execute(myTree, () -> {
            node.computePresentation(myWidget.createPresentation());
            return null;
        }).whenComplete((ignored, error) -> runOnUI(() -> {
            if (error != null) {
                logBuildError(error);
            }
            else if (!myDisposed && !node.myRemoved) {
                myWidget.update(node);
            }
            done.complete(null);
        }));

        return done;
    }

    public List<List<TreeNode<E>>> getExpandedPaths() {
        List<List<TreeNode<E>>> paths = new ArrayList<>();
        collectExpandedPaths(myRoot, paths);
        return paths;
    }

    private void collectExpandedPaths(TreeNodeImpl<E> node, List<List<TreeNode<E>>> paths) {
        for (TreeNodeImpl<E> child : node.myChildren) {
            if (child.myExpanded && !child.myRemoved) {
                paths.add(List.copyOf(pathTo(child)));

                collectExpandedPaths(child, paths);
            }
        }
    }

    public List<TreeNode<E>> getSelectedPath() {
        TreeNodeImpl<E> selected = getSelected();
        return selected == null ? List.of() : List.copyOf(pathTo(selected));
    }

    private List<TreeNodeImpl<E>> pathTo(TreeNodeImpl<E> node) {
        List<TreeNodeImpl<E>> path = new ArrayList<>();
        for (TreeNodeImpl<E> current = node; current != null; current = current.getParent()) {
            path.add(current);
        }
        Collections.reverse(path);
        return path;
    }

    CompletableFuture<TreeNode<E>> findChild(TreeNodeImpl<E> node, Predicate<E> predicate) {
        return loadChildren(node).<TreeNode<E>>thenApply(children -> {
            for (TreeNodeImpl<E> child : children) {
                if (predicate.test(child.myValue)) {
                    return child;
                }
            }
            return null;
        });
    }

    CompletableFuture<TreeNode<E>> findChildDeep(TreeNodeImpl<E> node, Predicate<E> predicate) {
        return loadChildren(node).thenCompose(children -> findDeep(children, predicate, 0));
    }

    private CompletableFuture<TreeNode<E>> findDeep(List<TreeNodeImpl<E>> children, Predicate<E> predicate, int from) {
        for (int index = from; index < children.size(); index++) {
            TreeNodeImpl<E> child = children.get(index);
            if (predicate.test(child.myValue)) {
                return CompletableFuture.completedFuture(child);
            }

            CompletableFuture<TreeNode<E>> deep = findChildDeep(child, predicate);
            if (deep.isDone() && !deep.isCompletedExceptionally()) {
                TreeNode<E> found = deep.join();
                if (found != null) {
                    return CompletableFuture.completedFuture(found);
                }
                continue;
            }

            int next = index + 1;
            return deep.thenCompose(found -> found != null ? CompletableFuture.completedFuture(found) : findDeep(children, predicate, next));
        }
        return CompletableFuture.completedFuture(null);
    }

    @RequiredUIAccess
    public void onExpanded(TreeNodeImpl<E> handle, InputDetails details) {
        TreeNodeImpl<E> node = toNode(handle);
        if (node != null) {
            expandByUser(node, details, false);
        }
    }

    @RequiredUIAccess
    public void onCollapsed(TreeNodeImpl<E> handle, InputDetails details) {
        TreeNodeImpl<E> node = toNode(handle);
        if (node != null) {
            close(node, details, false);
        }
    }

    @RequiredUIAccess
    public void onSelected(@Nullable TreeNodeImpl<E> handle, InputDetails details) {
        setSelection(handle == null ? null : toNode(handle), details, false);
    }

    @RequiredUIAccess
    public void onDoubleClick(TreeNodeImpl<E> handle, InputDetails details) {
        TreeNodeImpl<E> node = toNode(handle);
        if (node == null || node == myRoot) {
            return;
        }

        fireDoubleClick(node, details);

        if (!myModel.onDoubleClick(myTree, node, details)) {
            return;
        }

        if (node.myExpanded) {
            close(node, details, true);
        }
        else {
            expandByUser(node, details, true);
        }
    }

    @RequiredUIAccess
    private void expandByUser(TreeNodeImpl<E> node, InputDetails details, boolean updateWidget) {
        if (node == myRoot || node.myRemoved || node.myExpanded || node.myLeaf) {
            return;
        }

        node.myExpanded = true;
        if (updateWidget) {
            myWidget.setExpanded(node, true);
        }
        fireExpand(node, details);

        track(loadChildren(node).thenCompose(children -> onUI(() -> {
            if (isShown(node)) {
                prebuild(node.myChildren);
            }
        })));
    }

    @RequiredUIAccess
    private void close(TreeNodeImpl<E> node, InputDetails details, boolean updateWidget) {
        if (node == myRoot || !node.myExpanded) {
            return;
        }

        node.myExpanded = false;
        if (updateWidget) {
            myWidget.setExpanded(node, false);
        }
        fireCollapse(node, details);
    }

    @RequiredUIAccess
    private void setSelection(@Nullable TreeNodeImpl<E> node, InputDetails details, boolean updateWidget) {
        boolean changed = mySelected != node;
        mySelected = node;

        if (updateWidget) {
            myWidget.setSelected(node);
        }

        if (changed && node != null) {
            fireSelect(node, details);
        }
    }

    public CompletableFuture<?> whenIdle() {
        synchronized (myIdleLock) {
            if (myPending == 0) {
                return CompletableFuture.completedFuture(null);
            }

            CompletableFuture<Void> waiter = new CompletableFuture<>();
            myIdleWaiters.add(waiter);
            return waiter;
        }
    }

    public boolean isIdle() {
        synchronized (myIdleLock) {
            return myPending == 0;
        }
    }

    private <T> CompletableFuture<T> track(CompletableFuture<T> future) {
        synchronized (myIdleLock) {
            myPending++;
        }

        future.whenComplete((value, error) -> {
            List<CompletableFuture<Void>> waiters;
            synchronized (myIdleLock) {
                if (--myPending > 0) {
                    return;
                }

                waiters = myIdleWaiters;
                myIdleWaiters = new ArrayList<>();
            }

            for (CompletableFuture<Void> waiter : waiters) {
                waiter.complete(null);
            }
        });
        return future;
    }

    private CompletableFuture<?> onUI(Runnable action) {
        CompletableFuture<Object> done = new CompletableFuture<>();
        runOnUI(() -> {
            if (myDisposed) {
                done.complete(null);
                return;
            }

            try {
                action.run();
                done.complete(null);
            }
            catch (Throwable e) {
                LOG.error(e);
                done.completeExceptionally(e);
            }
        });
        return done;
    }

    private boolean hasUI() {
        UIAccess access = myWidget.getUIAccess();
        return access != null && access.isValid();
    }

    private void runOnUI(Runnable action) {
        UIAccess access = myWidget.getUIAccess();
        if (access == null || !access.isValid()) {
            enqueue(action);
            return;
        }

        if (myWidget.isUIThread()) {
            runSafely(action);
            return;
        }

        dispatch(access, action);
    }

    private void dispatch(UIAccess access, Runnable action) {
        AtomicBoolean started = new AtomicBoolean();
        access.giveAsync(() -> {
            started.set(true);
            runSafely(action);
            return null;
        }).whenComplete((ignored, error) -> {
            if (error == null || started.get()) {
                return;
            }

            UIAccess current = myWidget.getUIAccess();
            if (current != null && current != access && current.isValid()) {
                dispatch(current, action);
            }
            else {
                enqueue(action);
            }
        });
    }

    private void enqueue(Runnable action) {
        synchronized (myQueued) {
            myQueued.add(action);
        }

        UIAccess access = myWidget.getUIAccess();
        if (access != null && access.isValid()) {
            access.give(this::drainQueued);
        }
    }

    @RequiredUIAccess
    private void drainQueued() {
        List<Runnable> queued;
        synchronized (myQueued) {
            queued = List.copyOf(myQueued);
            myQueued.clear();
        }

        for (Runnable action : queued) {
            runSafely(action);
        }
    }

    private static void runSafely(Runnable action) {
        try {
            action.run();
        }
        catch (Throwable e) {
            LOG.error(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireExpand(TreeNodeImpl<E> node, InputDetails details) {
        myTree.getListenerDispatcher(TreeExpandEvent.class).onEvent(new TreeExpandEvent<>(myTree, node, details));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireCollapse(TreeNodeImpl<E> node, InputDetails details) {
        myTree.getListenerDispatcher(TreeCollapseEvent.class).onEvent(new TreeCollapseEvent<>(myTree, node, details));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireSelect(TreeNodeImpl<E> node, InputDetails details) {
        myTree.getListenerDispatcher(TreeSelectEvent.class).onEvent(new TreeSelectEvent<>(myTree, node, details));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fireDoubleClick(TreeNodeImpl<E> node, InputDetails details) {
        myTree.getListenerDispatcher(TreeDoubleClickEvent.class).onEvent(new TreeDoubleClickEvent<>(myTree, node, details));
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }

    private static void logBuildError(@Nullable Throwable error) {
        if (error == null) {
            return;
        }

        Throwable cause = unwrap(error);
        if (cause instanceof CancellationException || cause instanceof ProcessCanceledException) {
            return;
        }
        LOG.error(cause);
    }

    @Override
    public void dispose() {
        myDisposed = true;

        Map<TreeNodeImpl<E>, CompletableFuture<List<TreeNodeImpl<E>>>> deferred;
        synchronized (myDeferred) {
            deferred = new LinkedHashMap<>(myDeferred);
            myDeferred.clear();
        }
        deferred.forEach((node, result) -> result.complete(List.of()));

        drainQueued();
    }
}
