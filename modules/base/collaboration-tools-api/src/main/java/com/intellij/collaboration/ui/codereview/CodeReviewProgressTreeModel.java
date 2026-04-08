// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview;

import consulo.disposer.Disposable;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.collection.TreeTraversal;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class CodeReviewProgressTreeModel<T> {
    public abstract @Nonnull StateFlow<Boolean> getIsLoading();

    private final DisposableWrapperList<Runnable> listeners = new DisposableWrapperList<>();

    private final NodeCodeReviewProgressState defaultState = new NodeCodeReviewProgressState(false, true, 0);

    private final Map<ChangesBrowserNode<?>, NodeCodeReviewProgressState> stateCache = new HashMap<>();

    public abstract @Nullable T asLeaf(@Nonnull ChangesBrowserNode<?> node);

    public abstract boolean isLoading(@Nonnull T leafValue);

    public abstract boolean isRead(@Nonnull T leafValue);

    public abstract int getUnresolvedDiscussionsCount(@Nonnull T leafValue);

    @Nonnull
    NodeCodeReviewProgressState getState(@Nonnull ChangesBrowserNode<?> node) {
        NodeCodeReviewProgressState cachedState = stateCache.get(node);
        if (cachedState != null) {
            return cachedState;
        }
        // can be rewritten to hand-made bfs not to go down to leafs if state is cached
        NodeCodeReviewProgressState[] calculatedState = {defaultState};
        TreeUtil.treeNodeTraverser(node).traverse(TreeTraversal.POST_ORDER_DFS)
            .forEach(it -> {
                if (!(it instanceof ChangesBrowserNode<?> changesNode)) {
                    return;
                }
                T leafValue = asLeaf(changesNode);
                if (leafValue == null) {
                    return;
                }
                NodeCodeReviewProgressState leafState = getState(leafValue);
                calculatedState[0] = new NodeCodeReviewProgressState(
                    calculatedState[0].isLoading() || leafState.isLoading(),
                    calculatedState[0].isRead() && leafState.isRead(),
                    calculatedState[0].discussionsCount() + leafState.discussionsCount()
                );
            });
        stateCache.put(node, calculatedState[0]);
        return calculatedState[0];
    }

    public void addChangeListener(@Nonnull Disposable parent, @Nonnull Runnable listener) {
        listeners.add(listener, parent);
    }

    public void addChangeListener(@Nonnull Runnable listener) {
        listeners.add(listener);
    }

    private @Nonnull NodeCodeReviewProgressState getState(@Nonnull T leafValue) {
        return new NodeCodeReviewProgressState(isLoading(leafValue), isRead(leafValue), getUnresolvedDiscussionsCount(leafValue));
    }

    protected void fireModelChanged() {
        stateCache.clear();
        listeners.forEach(Runnable::run);
    }
}
