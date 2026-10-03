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
package consulo.it;

import consulo.it.internal.ui.HeadlessFlameGraph;
import consulo.ui.Point2D;
import consulo.ui.chart.FlameGraph;
import consulo.ui.event.details.InputDetails;
import consulo.ui.impl.chart.FlameGraphIndex;
import consulo.ui.impl.chart.FlameGraphNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class FlameGraphTester<E> {
    private final HeadlessFlameGraph<E> myGraph;

    private FlameGraphTester(HeadlessFlameGraph<E> graph) {
        myGraph = graph;
    }

    public static <E> FlameGraphTester<E> of(FlameGraph<E> graph) {
        if (!(graph instanceof HeadlessFlameGraph<E> headless)) {
            throw new IllegalArgumentException("Not a headless flame graph: " + graph);
        }
        return new FlameGraphTester<>(headless);
    }

    public FlameGraph<E> getGraph() {
        return myGraph;
    }

    public FlameGraphTester<E> userSelect(@Nullable E value) {
        HeadlessUIThread.run(() -> myGraph.userSelect(value, userInput()));
        return this;
    }

    public FlameGraphTester<E> userDoubleClick(E value) {
        HeadlessUIThread.run(() -> myGraph.userDoubleClick(value, userInput()));
        return this;
    }

    public int getMaxDepth() {
        FlameGraphIndex<E> index = myGraph.getIndex();
        return HeadlessUIThread.compute(index::getMaxDepth);
    }

    public String dump() {
        return HeadlessUIThread.compute(() -> {
            StringBuilder builder = new StringBuilder();
            dump(builder, myGraph.getIndex().getRoot());
            return builder.toString();
        });
    }

    private void dump(StringBuilder builder, FlameGraphNode<E> node) {
        E value = node.getValue();
        String name = myGraph.getModel().getName(value);

        builder.append(" ".repeat(node.getDepth()));
        if (myGraph.isDimmed(value)) {
            builder.append('~');
        }
        if (Objects.equals(value, myGraph.getSelectedValue())) {
            builder.append('[').append(name).append(']');
        }
        else {
            builder.append(name);
        }
        builder.append(' ').append(node.getDuration());
        if (Objects.equals(value, myGraph.getFocused())) {
            builder.append(" *");
        }
        builder.append('\n');

        for (FlameGraphNode<E> child : node.getChildren()) {
            dump(builder, child);
        }
    }

    public void assertStructure(String expected) {
        Assertions.assertEquals(expected, dump());
    }

    private static InputDetails userInput() {
        return new InputDetails(new Point2D(0, 0), new Point2D(0, 0));
    }
}
