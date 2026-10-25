// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.log.impl.internal.graph;

import consulo.versionControlSystem.log.graph.LinearGraph;
import consulo.versionControlSystem.log.graph.LinearGraphUtils;
import consulo.versionControlSystem.log.graph.LiteLinearGraph;
import consulo.versionControlSystem.log.impl.internal.util.BitSetFlags;
import consulo.versionControlSystem.log.impl.internal.util.IntHashSetFlags;

import java.util.Set;

public final class GraphUtil {
    private GraphUtil() {
    }

    public static Set<Integer> subgraphDifference(LinearGraph graph, int node1, int node2) {
        LiteLinearGraph liteLinearGraph = LinearGraphUtils.asLiteLinearGraph(graph);

        BitSetFlags visited2 = new BitSetFlags(graph.nodesCount());
        BfsWalk bfsWalk2 = new BfsWalk(node2, liteLinearGraph, visited2);
        IntHashSetFlags visited1 = new IntHashSetFlags(graph.nodesCount()) {
            @Override
            public boolean get(int index) {
                return super.get(index) || visited2.get(index) || bfsWalk2.currentNodes().contains(index);
            }
        };
        BfsWalk bfsWalk1 = new BfsWalk(node1, liteLinearGraph, visited1);

        int max1 = maxOrDefault(bfsWalk1.currentNodes());
        int min2 = minOrDefault(bfsWalk2.currentNodes());
        while (!bfsWalk1.isFinished()) {
            if (max1 < min2) {
                max1 = Math.max(max1, maxOrDefault(bfsWalk1.step()));
            }
            else {
                bfsWalk2.step();
                min2 = minOrDefault(bfsWalk2.currentNodes());
            }
        }

        return visited1.getData();
    }

    private static int minOrDefault(Iterable<Integer> values) {
        int result = Integer.MAX_VALUE;
        for (int value : values) {
            result = Math.min(result, value);
        }
        return result;
    }

    private static int maxOrDefault(Iterable<Integer> values) {
        int result = Integer.MIN_VALUE;
        for (int value : values) {
            result = Math.max(result, value);
        }
        return result;
    }
}
