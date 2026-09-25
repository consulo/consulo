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
package consulo.it.ui.tree;

import consulo.component.ProcessCanceledException;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
final class MapTreeModel implements TreeModel<String> {
    static final String ROOT = "root";

    private final Map<String, List<String>> myChildren = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> myBuilds = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> myCancellations = new ConcurrentHashMap<>();
    private final Set<String> myPrebuilt = ConcurrentHashMap.newKeySet();
    private final Set<Boolean> myRenderedOnUIThread = ConcurrentHashMap.newKeySet();

    private volatile @Nullable Comparator<TreeNode<String>> myComparator;
    private volatile boolean myDoubleClickToggles = true;

    MapTreeModel put(String parent, String... children) {
        myChildren.put(parent, List.of(children));
        return this;
    }

    MapTreeModel prebuild(String value) {
        myPrebuilt.add(value);
        return this;
    }

    MapTreeModel cancelBuilds(String parent, int times) {
        myCancellations.put(parent, new AtomicInteger(times));
        return this;
    }

    MapTreeModel sortBy(Comparator<TreeNode<String>> comparator) {
        myComparator = comparator;
        return this;
    }

    MapTreeModel keepDoubleClicks() {
        myDoubleClickToggles = false;
        return this;
    }

    int getBuildCount(String parent) {
        AtomicInteger builds = myBuilds.get(parent);
        return builds == null ? 0 : builds.get();
    }

    Set<Boolean> getRenderedOnUIThread() {
        return Set.copyOf(myRenderedOnUIThread);
    }

    @Override
    public void buildChildren(Function<String, TreeNode<String>> nodeFactory, @Nullable String parentValue) {
        String parent = parentValue == null ? ROOT : parentValue;
        myBuilds.computeIfAbsent(parent, key -> new AtomicInteger()).incrementAndGet();

        AtomicInteger cancellations = myCancellations.get(parent);
        if (cancellations != null && cancellations.getAndDecrement() > 0) {
            throw new ProcessCanceledException();
        }

        for (String child : myChildren.getOrDefault(parent, List.of())) {
            TreeNode<String> node = nodeFactory.apply(child);
            node.setLeaf(!myChildren.containsKey(child));
            node.setRenderer((value, presentation) -> {
                myRenderedOnUIThread.add(UIAccess.isUIThread());
                presentation.append(value);
            });
        }
    }

    @Override
    public boolean isNeedBuildChildrenBeforeOpen(TreeNode<String> node) {
        String value = node.getValue();
        return value != null && myPrebuilt.contains(value);
    }

    @Override
    public boolean onDoubleClick(Tree<String> tree, TreeNode<String> node) {
        return myDoubleClickToggles;
    }

    @Override
    public @Nullable Comparator<TreeNode<String>> getNodeComparator() {
        return myComparator;
    }
}
