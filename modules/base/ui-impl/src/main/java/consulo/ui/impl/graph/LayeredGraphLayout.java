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
package consulo.ui.impl.graph;

import consulo.ui.Rectangle2D;
import consulo.ui.Size2D;
import consulo.ui.graph.GraphGroup;
import consulo.ui.graph.GraphModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Places the nodes of a {@link GraphModel} in layers, so that arrows point from one layer down to a later one.
 * Arrows which close a cycle are turned around for the placement only. Inside a layer the nodes are ordered by
 * the mean position of the nodes they come from, which keeps crossing arrows down.
 *
 * @author VISTALL
 * @since 2026-09-30
 */
public final class LayeredGraphLayout {
    private static final int ORDER_SWEEPS = 4;

    private LayeredGraphLayout() {
    }

    public static <E> Map<E, Rectangle2D> layout(GraphModel<E> model, Function<E, Size2D> sizeFunction, int nodeGap, int layerGap) {
        return place(layers(model), sizeFunction, nodeGap, layerGap);
    }

    /**
     * The layers top to bottom, each ordered left to right - for a frontend which sizes and places the nodes itself.
     */
    public static <E> List<List<E>> layers(GraphModel<E> model) {
        List<E> nodes = new ArrayList<>(new LinkedHashSet<>(model.getNodes()));
        Set<E> known = new HashSet<>(nodes);

        Map<E, List<E>> successors = new HashMap<>();
        for (E node : nodes) {
            Set<E> targets = new LinkedHashSet<>();
            for (E target : model.getArrows(node)) {
                if (known.contains(target) && !target.equals(node)) {
                    targets.add(target);
                }
            }
            successors.put(node, new ArrayList<>(targets));
        }

        Map<E, List<E>> acyclic = removeCycles(nodes, successors);

        Map<E, Integer> layerOf = assignLayers(nodes, acyclic);

        List<List<E>> layers = new ArrayList<>();
        for (E node : nodes) {
            int layer = layerOf.get(node);
            while (layers.size() <= layer) {
                layers.add(new ArrayList<>());
            }
            layers.get(layer).add(node);
        }

        Map<E, List<E>> predecessors = new HashMap<>();
        for (E node : nodes) {
            predecessors.put(node, new ArrayList<>());
        }
        for (E node : nodes) {
            for (E target : acyclic.get(node)) {
                predecessors.get(target).add(node);
            }
        }

        orderLayers(layers, predecessors, acyclic);

        for (List<E> layer : layers) {
            clusterGroups(layer, model);
        }
        return layers;
    }

    private static <E> Map<E, List<E>> removeCycles(List<E> nodes, Map<E, List<E>> successors) {
        Map<E, List<E>> acyclic = new HashMap<>();
        for (E node : nodes) {
            acyclic.put(node, new ArrayList<>());
        }

        Set<E> visited = new HashSet<>();
        Set<E> onPath = new HashSet<>();
        for (E root : nodes) {
            if (visited.contains(root)) {
                continue;
            }

            List<E> stack = new ArrayList<>();
            List<Integer> nextIndex = new ArrayList<>();
            stack.add(root);
            nextIndex.add(0);
            visited.add(root);
            onPath.add(root);

            while (!stack.isEmpty()) {
                int top = stack.size() - 1;
                E node = stack.get(top);
                List<E> targets = successors.get(node);
                int index = nextIndex.get(top);
                if (index == targets.size()) {
                    stack.remove(top);
                    nextIndex.remove(top);
                    onPath.remove(node);
                    continue;
                }

                nextIndex.set(top, index + 1);

                E target = targets.get(index);
                if (onPath.contains(target)) {
                    addUnique(acyclic.get(target), node);
                }
                else {
                    addUnique(acyclic.get(node), target);
                    if (visited.add(target)) {
                        stack.add(target);
                        nextIndex.add(0);
                        onPath.add(target);
                    }
                }
            }
        }
        return acyclic;
    }

    private static <E> void addUnique(List<E> list, E value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    private static <E> Map<E, Integer> assignLayers(List<E> nodes, Map<E, List<E>> acyclic) {
        Map<E, Integer> incoming = new HashMap<>();
        for (E node : nodes) {
            incoming.put(node, 0);
        }
        for (E node : nodes) {
            for (E target : acyclic.get(node)) {
                incoming.merge(target, 1, Integer::sum);
            }
        }

        Map<E, Integer> layerOf = new HashMap<>();
        List<E> ready = new ArrayList<>();
        for (E node : nodes) {
            layerOf.put(node, 0);
            if (incoming.get(node) == 0) {
                ready.add(node);
            }
        }

        for (int i = 0; i < ready.size(); i++) {
            E node = ready.get(i);
            int next = layerOf.get(node) + 1;
            for (E target : acyclic.get(node)) {
                layerOf.merge(target, next, Math::max);
                if (incoming.merge(target, -1, Integer::sum) == 0) {
                    ready.add(target);
                }
            }
        }
        return layerOf;
    }

    private static <E> void orderLayers(List<List<E>> layers, Map<E, List<E>> predecessors, Map<E, List<E>> successors) {
        Map<E, Integer> position = new HashMap<>();
        updatePositions(layers, position);

        for (int sweep = 0; sweep < ORDER_SWEEPS; sweep++) {
            for (int i = 1; i < layers.size(); i++) {
                sortByNeighbours(layers.get(i), predecessors, position);
            }
            for (int i = layers.size() - 2; i >= 0; i--) {
                sortByNeighbours(layers.get(i), successors, position);
            }
        }
    }

    private static <E> void sortByNeighbours(List<E> layer, Map<E, List<E>> neighbours, Map<E, Integer> position) {
        Map<E, Double> keys = new HashMap<>();
        for (E node : layer) {
            List<E> around = neighbours.get(node);
            if (around.isEmpty()) {
                keys.put(node, (double) position.get(node));
            }
            else {
                double sum = 0;
                for (E other : around) {
                    sum += position.get(other);
                }
                keys.put(node, sum / around.size());
            }
        }

        layer.sort(Comparator.comparingDouble(keys::get));

        for (int i = 0; i < layer.size(); i++) {
            position.put(layer.get(i), i);
        }
    }

    private static <E> void clusterGroups(List<E> layer, GraphModel<E> model) {
        Map<GraphGroup, Integer> firstIndex = new HashMap<>();
        Map<E, Integer> keys = new HashMap<>();
        for (int i = 0; i < layer.size(); i++) {
            E node = layer.get(i);
            GraphGroup group = model.getGroup(node);
            int index = i;
            keys.put(node, group == null ? index : firstIndex.computeIfAbsent(group, it -> index));
        }
        layer.sort(Comparator.comparingInt(keys::get));
    }

    private static <E> void updatePositions(List<List<E>> layers, Map<E, Integer> position) {
        for (List<E> layer : layers) {
            for (int i = 0; i < layer.size(); i++) {
                position.put(layer.get(i), i);
            }
        }
    }

    private static <E> Map<E, Rectangle2D> place(List<List<E>> layers, Function<E, Size2D> sizeFunction, int nodeGap, int layerGap) {
        Map<E, Size2D> sizes = new HashMap<>();
        int[] layerWidth = new int[layers.size()];
        int[] layerHeight = new int[layers.size()];
        int maxWidth = 0;
        for (int i = 0; i < layers.size(); i++) {
            List<E> layer = layers.get(i);
            int width = 0;
            int height = 0;
            for (E node : layer) {
                Size2D size = sizeFunction.apply(node);
                sizes.put(node, size);
                width += size.width();
                height = Math.max(height, size.height());
            }
            width += nodeGap * Math.max(0, layer.size() - 1);
            layerWidth[i] = width;
            layerHeight[i] = height;
            maxWidth = Math.max(maxWidth, width);
        }

        Map<E, Rectangle2D> bounds = new LinkedHashMap<>();
        int y = 0;
        for (int i = 0; i < layers.size(); i++) {
            int x = (maxWidth - layerWidth[i]) / 2;
            for (E node : layers.get(i)) {
                Size2D size = sizes.get(node);
                bounds.put(node, new Rectangle2D(x, y + (layerHeight[i] - size.height()) / 2, size.width(), size.height()));
                x += size.width() + nodeGap;
            }
            y += layerHeight[i] + layerGap;
        }
        return bounds;
    }
}
