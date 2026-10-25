/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.graph;

import consulo.versionControlSystem.log.graph.LiteLinearGraph;
import consulo.versionControlSystem.log.impl.internal.util.BitSetFlags;
import consulo.versionControlSystem.log.impl.internal.util.Flags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.IntPredicate;

public class BfsWalk {
    private final int myStart;
    private final LiteLinearGraph myGraph;
    private final Flags myVisited;
    private final boolean myDown;

    protected final Queue<Integer> myQueue;

    public BfsWalk(int start, LiteLinearGraph graph) {
        this(start, graph, new BitSetFlags(graph.nodesCount()));
    }

    public BfsWalk(int start, LiteLinearGraph graph, Flags visited) {
        this(start, graph, visited, true);
    }

    public BfsWalk(int start, LiteLinearGraph graph, Flags visited, boolean down) {
        myStart = start;
        myGraph = graph;
        myVisited = visited;
        myDown = down;
        myQueue = new LinkedList<>();
        myQueue.add(start);
    }

    public int getStart() {
        return myStart;
    }

    public boolean isFinished() {
        return myQueue.isEmpty();
    }

    public Queue<Integer> currentNodes() {
        return myQueue;
    }

    public List<Integer> step() {
        return step(node -> true);
    }

    public List<Integer> step(IntPredicate consumer) {
        while (!myQueue.isEmpty()) {
            int node = myQueue.poll();
            if (!myVisited.get(node)) {
                myVisited.set(node, true);
                if (!consumer.test(node)) {
                    return Collections.emptyList();
                }
                List<Integer> next = new ArrayList<>(myGraph.getNodes(node, myDown ? LiteLinearGraph.NodeFilter.DOWN : LiteLinearGraph.NodeFilter.UP));
                if (myDown) {
                    Collections.sort(next);
                }
                else {
                    next.sort(Collections.reverseOrder());
                }
                myQueue.addAll(next);
                return next;
            }
        }
        return Collections.emptyList();
    }

    public void walk() {
        walk(node -> true);
    }

    public void walk(IntPredicate consumer) {
        while (!isFinished()) {
            step(consumer);
        }
    }
}
