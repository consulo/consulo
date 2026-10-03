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
import consulo.ui.impl.chart.model.HNode;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class FlameGraphNode<E> implements HNode<FlameGraphNode<E>> {
    private final E myValue;
    private final @Nullable FlameGraphNode<E> myParent;
    private final long myStart;
    private final long myEnd;
    private final int myDepth;
    private final List<FlameGraphNode<E>> myChildren = new ArrayList<>();

    private FlameGraphNode(E value, @Nullable FlameGraphNode<E> parent, long start, long end, int depth) {
        myValue = value;
        myParent = parent;
        myStart = start;
        myEnd = end;
        myDepth = depth;
    }

    public static <E> FlameGraphNode<E> build(FlameGraphModel<E> model, E root) {
        FlameGraphNode<E> node = new FlameGraphNode<>(root, null, 0, Math.max(0, model.getWeight(root)), 0);
        fill(model, node);
        return node;
    }

    private static <E> void fill(FlameGraphModel<E> model, FlameGraphNode<E> parent) {
        long start = parent.myStart;
        for (E child : model.getChildren(parent.myValue)) {
            long weight = Math.max(0, model.getWeight(child));
            FlameGraphNode<E> node = new FlameGraphNode<>(child, parent, start, start + weight, parent.myDepth + 1);
            parent.myChildren.add(node);
            fill(model, node);
            start += weight;
        }
    }

    public E getValue() {
        return myValue;
    }

    public List<FlameGraphNode<E>> getChildren() {
        return myChildren;
    }

    @Override
    public int getChildCount() {
        return myChildren.size();
    }

    @Override
    public FlameGraphNode<E> getChildAt(int index) {
        return myChildren.get(index);
    }

    @Override
    public @Nullable FlameGraphNode<E> getParent() {
        return myParent;
    }

    @Override
    public long getStart() {
        return myStart;
    }

    @Override
    public long getEnd() {
        return myEnd;
    }

    @Override
    public int getDepth() {
        return myDepth;
    }
}
