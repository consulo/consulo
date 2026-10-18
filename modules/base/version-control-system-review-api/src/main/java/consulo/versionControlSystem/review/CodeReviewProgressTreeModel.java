// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.versionControlSystem.review;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.RefComparisonChange;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class CodeReviewProgressTreeModel<T> {
    private static final NodeCodeReviewProgressState DEFAULT_NODE_STATE = new NodeCodeReviewProgressState(false, true, 0);

    private final List<Runnable> myListeners = new CopyOnWriteArrayList<>();

    private final Map<RefComparisonChange, NodeCodeReviewProgressState> myStateCache = new HashMap<>();

    public abstract ObservableValue<Boolean> isLoading();

    public abstract @Nullable T asLeaf(RefComparisonChange change);

    public abstract boolean isLoading(T leafValue);

    public abstract boolean isRead(T leafValue);

    public abstract int getUnresolvedDiscussionsCount(T leafValue);

    public NodeCodeReviewProgressState getState(RefComparisonChange change) {
        NodeCodeReviewProgressState cachedState = myStateCache.get(change);
        if (cachedState != null) {
            return cachedState;
        }

        T leaf = asLeaf(change);
        NodeCodeReviewProgressState nodeState = leaf == null || isLoading(leaf)
            ? DEFAULT_NODE_STATE
            : new NodeCodeReviewProgressState(false, isRead(leaf), getUnresolvedDiscussionsCount(leaf));

        myStateCache.put(change, nodeState);
        return nodeState;
    }

    public void addChangeListener(Disposable parent, Runnable listener) {
        myListeners.add(listener);
        Disposer.register(parent, () -> myListeners.remove(listener));
    }

    public void addChangeListener(Runnable listener) {
        myListeners.add(listener);
    }

    protected void fireModelChanged() {
        myStateCache.clear();
        for (Runnable listener : myListeners) {
            listener.run();
        }
    }
}
