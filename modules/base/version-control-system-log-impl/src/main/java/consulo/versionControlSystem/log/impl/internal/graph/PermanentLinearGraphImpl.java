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

import consulo.util.collection.SmartList;
import consulo.versionControlSystem.log.graph.*;
import consulo.versionControlSystem.log.impl.internal.util.BitSetFlags;
import consulo.versionControlSystem.log.impl.internal.util.CompressedIntList;
import consulo.versionControlSystem.log.impl.internal.util.Flags;
import consulo.versionControlSystem.log.impl.internal.util.IntList;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

public class PermanentLinearGraphImpl implements LinearGraph {
  private final Flags mySimpleNodes;

  // myNodeToEdgeIndex.length = nodesCount() + 1.
  private final IntList myNodeToEdgeIndex;
  private final IntList myLongEdges;

  /*package*/ PermanentLinearGraphImpl(Flags simpleNodes, int[] nodeToEdgeIndex, int[] longEdges) {
    mySimpleNodes = simpleNodes;
    myNodeToEdgeIndex = CompressedIntList.newInstance(nodeToEdgeIndex);
    myLongEdges = CompressedIntList.newInstance(longEdges);
  }

  @TestOnly
  public PermanentLinearGraphImpl() {
    this(new BitSetFlags(0), new int[0], new int[0]);
  }

  @Override
  public int nodesCount() {
    return mySimpleNodes.size();
  }

  @Nonnull
  @Override
  public List<GraphEdge> getAdjacentEdges(int nodeIndex, @Nonnull EdgeFilter filter) {
    List<GraphEdge> result = new SmartList<>();

    boolean hasUpSimpleEdge = nodeIndex != 0 && mySimpleNodes.get(nodeIndex - 1);
    if (hasUpSimpleEdge && filter.upNormal) result.add(new GraphEdge(nodeIndex - 1, nodeIndex, null, GraphEdgeType.USUAL));

    for (int i = myNodeToEdgeIndex.get(nodeIndex); i < myNodeToEdgeIndex.get(nodeIndex + 1); i++) {
      int adjacentNode = myLongEdges.get(i);

      if (adjacentNode < 0 && filter.special) {
        result.add(GraphEdge.createEdgeWithTargetId(nodeIndex, adjacentNode, GraphEdgeType.NOT_LOAD_COMMIT));
      }
      if (adjacentNode < 0) continue;

      if (nodeIndex > adjacentNode && filter.upNormal) result.add(new GraphEdge(adjacentNode, nodeIndex, null, GraphEdgeType.USUAL));
      if (nodeIndex < adjacentNode && filter.downNormal) result.add(new GraphEdge(nodeIndex, adjacentNode, null, GraphEdgeType.USUAL));
    }

    if (mySimpleNodes.get(nodeIndex) && filter.downNormal) result.add(new GraphEdge(nodeIndex, nodeIndex + 1, null, GraphEdgeType.USUAL));

    return result;
  }

  @Nonnull
  @Override
  public GraphNode getGraphNode(int nodeIndex) {
    return new GraphNode(nodeIndex);
  }

  @Override
  public int getNodeId(int nodeIndex) {
    assert nodeIndex >= 0 && nodeIndex < nodesCount() : "Bad nodeIndex: " + nodeIndex;
    return nodeIndex;
  }

  @Override
  public Integer getNodeIndex(int nodeId) {
    if (nodeId >= 0 && nodeId < nodesCount()) {
      return nodeId;
    }
    else {
      return null;
    }
  }
}
