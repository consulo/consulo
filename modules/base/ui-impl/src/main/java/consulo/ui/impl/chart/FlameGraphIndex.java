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
package consulo.ui.impl.chart;

import consulo.ui.chart.FlameGraphModel;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class FlameGraphIndex<E> {
    private final FlameGraphModel<E> myModel;
    private final Map<E, FlameGraphNode<E>> myNodes = new HashMap<>();

    private FlameGraphNode<E> myRoot;
    private int myMaxDepth;

    public FlameGraphIndex(FlameGraphModel<E> model) {
        myModel = model;
        myRoot = build();
    }

    public void rebuild() {
        myRoot = build();
    }

    private FlameGraphNode<E> build() {
        myNodes.clear();
        myMaxDepth = 0;
        FlameGraphNode<E> root = FlameGraphNode.build(myModel, myModel.getRoot());
        index(root);
        return root;
    }

    private void index(FlameGraphNode<E> node) {
        myNodes.put(node.getValue(), node);
        myMaxDepth = Math.max(myMaxDepth, node.getDepth());
        for (FlameGraphNode<E> child : node.getChildren()) {
            index(child);
        }
    }

    public FlameGraphModel<E> getModel() {
        return myModel;
    }

    public FlameGraphNode<E> getRoot() {
        return myRoot;
    }

    public int getMaxDepth() {
        return myMaxDepth;
    }

    public @Nullable FlameGraphNode<E> getNode(@Nullable E value) {
        return value == null ? null : myNodes.get(value);
    }

    public boolean contains(@Nullable E value) {
        return value != null && myNodes.containsKey(value);
    }

    public @Nullable E retain(@Nullable E value) {
        return contains(value) ? value : null;
    }
}
