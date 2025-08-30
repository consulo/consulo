/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.versionControlSystem.log.graph.LinearGraph;
import consulo.versionControlSystem.log.graph.LinearGraphUtils;
import consulo.versionControlSystem.log.graph.LiteLinearGraph;
import consulo.versionControlSystem.log.impl.internal.util.BitSetFlags;
import consulo.versionControlSystem.log.impl.internal.util.DfsUtil;
import consulo.versionControlSystem.log.impl.internal.util.Flags;
import consulo.versionControlSystem.log.impl.internal.util.UnsignedBitSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ReachableNodes {
  @Nonnull
  private final LiteLinearGraph myGraph;
  @Nonnull
  private final DfsUtil myDfsUtil = new DfsUtil();
  @Nonnull
  private final Flags myTempFlags;

  public ReachableNodes(@Nonnull LiteLinearGraph graph) {
    myGraph = graph;
    myTempFlags = new BitSetFlags(graph.nodesCount());
  }

  @Nonnull
  public static UnsignedBitSet getReachableNodes(@Nonnull LinearGraph permanentGraph, @Nullable Set<Integer> headNodeIndexes) {
    if (headNodeIndexes == null) {
      UnsignedBitSet nodesVisibility = new UnsignedBitSet();
      nodesVisibility.set(0, permanentGraph.nodesCount() - 1, true);
      return nodesVisibility;
    }

    UnsignedBitSet result = new UnsignedBitSet();
    ReachableNodes getter = new ReachableNodes(LinearGraphUtils.asLiteLinearGraph(permanentGraph));
    getter.walk(headNodeIndexes, node -> result.set(node, true));

    return result;
  }

  public Set<Integer> getContainingBranches(int nodeIndex, @Nonnull Collection<Integer> branchNodeIndexes) {
    Set<Integer> result = new HashSet<>();

    walk(Collections.singletonList(nodeIndex), false, integer -> {
      if (branchNodeIndexes.contains(integer)) result.add(integer);
    });

    return result;
  }

  public void walk(@Nonnull Collection<Integer> headIds, @Nonnull Consumer<Integer> consumer) {
    walk(headIds, true, consumer);
  }

  private void walk(@Nonnull Collection<Integer> startNodes, final boolean goDown, @Nonnull final Consumer<Integer> consumer) {
    synchronized (myTempFlags) {

      myTempFlags.setAll(false);
      for (int start : startNodes) {
        if (start < 0) continue;
        if (myTempFlags.get(start)) continue;
        myTempFlags.set(start, true);
        consumer.accept(start);

        myDfsUtil.nodeDfsIterator(start, new DfsUtil.NextNode() {
          @Override
          public int fun(int currentNode) {
            for (int downNode : myGraph.getNodes(currentNode, goDown ? LiteLinearGraph.NodeFilter.DOWN : LiteLinearGraph.NodeFilter.UP)) {
              if (!myTempFlags.get(downNode)) {
                myTempFlags.set(downNode, true);
                consumer.accept(downNode);
                return downNode;
              }
            }

            return NODE_NOT_FOUND;
          }
        });
      }
    }
  }
}
