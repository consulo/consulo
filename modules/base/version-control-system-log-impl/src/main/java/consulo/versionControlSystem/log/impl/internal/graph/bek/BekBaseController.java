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
package consulo.versionControlSystem.log.impl.internal.graph.bek;

import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.log.graph.*;
import consulo.versionControlSystem.log.impl.internal.graph.CascadeController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public class BekBaseController extends CascadeController {
  @Nonnull
  private final BekIntMap myBekIntMap;
  @Nonnull
  private final LinearGraph myBekGraph;

  public BekBaseController(@Nonnull PermanentGraphInfo permanentGraphInfo, @Nonnull BekIntMap bekIntMap) {
    super(null, permanentGraphInfo);
    myBekIntMap = bekIntMap;
    myBekGraph = new BekLinearGraph(myBekIntMap, myPermanentGraphInfo.getLinearGraph());

    BekChecker.checkLinearGraph(myBekGraph);
  }

  @Nonnull
  @Override
  protected LinearGraphAnswer delegateGraphChanged(@Nonnull LinearGraphAnswer delegateAnswer) {
    throw new IllegalStateException();
  }

  @jakarta.annotation.Nullable
  @Override
  protected LinearGraphAnswer performAction(@Nonnull LinearGraphAction action) {
    return null;
  }

  @Nonnull
  public BekIntMap getBekIntMap() {
    return myBekIntMap;
  }

  @jakarta.annotation.Nullable
  @Override
  protected GraphElement convertToDelegate(@Nonnull GraphElement graphElement) {
    if (graphElement instanceof GraphEdge) {
      Integer upIndex = ((GraphEdge)graphElement).getUpNodeIndex();
      Integer downIndex = ((GraphEdge)graphElement).getDownNodeIndex();
      Integer convertedUpIndex = upIndex == null ? null : myBekIntMap.getUsualIndex(upIndex);
      Integer convertedDownIndex = downIndex == null ? null : myBekIntMap.getUsualIndex(downIndex);

      return new GraphEdge(convertedUpIndex, convertedDownIndex, ((GraphEdge)graphElement).getTargetId(), ((GraphEdge)graphElement).getType());
    }
    else if (graphElement instanceof GraphNode) {
      return new GraphNode(myBekIntMap.getUsualIndex((((GraphNode)graphElement).getNodeIndex())), ((GraphNode)graphElement).getType());
    }
    return null;
  }

  @Nonnull
  @Override
  public LinearGraph getCompiledGraph() {
    return myBekGraph;
  }

  public static class BekLinearGraph implements LinearGraph {
    @Nonnull
    private final LinearGraph myLinearGraph;
    @Nonnull
    private final BekIntMap myBekIntMap;

    public BekLinearGraph(@Nonnull BekIntMap bekIntMap, @Nonnull LinearGraph linearGraph) {
      myLinearGraph = linearGraph;
      myBekIntMap = bekIntMap;
    }

    @Override
    public int nodesCount() {
      return myLinearGraph.nodesCount();
    }

    @Nullable
    private Integer getNodeIndex(@Nullable Integer nodeId) {
      if (nodeId == null) return null;

      return myBekIntMap.getBekIndex(nodeId);
    }

    @Nonnull
    @Override
    public List<GraphEdge> getAdjacentEdges(int nodeIndex, @Nonnull EdgeFilter filter) {
      return ContainerUtil.map(myLinearGraph.getAdjacentEdges(myBekIntMap.getUsualIndex(nodeIndex), filter),
                 edge -> new GraphEdge(getNodeIndex(edge.getUpNodeIndex()), getNodeIndex(edge.getDownNodeIndex()), edge.getTargetId(), edge.getType()));
    }

    @Nonnull
    @Override
    public GraphNode getGraphNode(int nodeIndex) {
      assert inRanges(nodeIndex);

      return new GraphNode(nodeIndex, GraphNodeType.USUAL);
    }

    @Override
    public int getNodeId(int nodeIndex) {
      // see consulo.versionControlSystem.log.impl.internal.graph.PermanentLinearGraphImpl.getNodeId
      return myBekIntMap.getUsualIndex(nodeIndex);
    }

    @jakarta.annotation.Nullable
    @Override
    public Integer getNodeIndex(int nodeId) {
      if (!inRanges(nodeId)) return null;

      return myBekIntMap.getBekIndex(nodeId);
    }

    private boolean inRanges(int index) {
      return index >= 0 && index < nodesCount();
    }
  }
}
