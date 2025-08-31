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
import consulo.versionControlSystem.log.impl.internal.graph.EdgeStorageWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class LinearBekGraph implements LinearGraph {
  @Nonnull
  protected final LinearGraph myGraph;
  @Nonnull
  protected final EdgeStorageWrapper myHiddenEdges;
  @Nonnull
  protected final EdgeStorageWrapper myDottedEdges;

  public LinearBekGraph(@Nonnull LinearGraph graph) {
    myGraph = graph;
    myHiddenEdges = EdgeStorageWrapper.createSimpleEdgeStorage();
    myDottedEdges = EdgeStorageWrapper.createSimpleEdgeStorage();
  }

  @Override
  public int nodesCount() {
    return myGraph.nodesCount();
  }

  @Nonnull
  @Override
  public List<GraphEdge> getAdjacentEdges(int nodeIndex, @Nonnull EdgeFilter filter) {
    List<GraphEdge> result = new ArrayList<>();
    result.addAll(myDottedEdges.getAdjacentEdges(nodeIndex, filter));
    result.addAll(myGraph.getAdjacentEdges(nodeIndex, filter));
    result.removeAll(myHiddenEdges.getAdjacentEdges(nodeIndex, filter));
    return result;
  }

  @Nonnull
  @Override
  public GraphNode getGraphNode(int nodeIndex) {
    return myGraph.getGraphNode(nodeIndex);
  }

  @Override
  public int getNodeId(int nodeIndex) {
    return myGraph.getNodeId(nodeIndex);
  }

  @Nullable
  @Override
  public Integer getNodeIndex(int nodeId) {
    return myGraph.getNodeIndex(nodeId);
  }

  public Collection<GraphEdge> expandEdge(@Nonnull GraphEdge edge) {
    Set<GraphEdge> result = new HashSet<>();

    assert edge.getType() == GraphEdgeType.DOTTED;
    myDottedEdges.removeEdge(edge);

    Integer tail = edge.getUpNodeIndex();
    Integer firstChild = edge.getDownNodeIndex();
    assert tail != null : "Collapsed from to an unloaded node";
    assert firstChild != null : "Collapsed edge to an unloaded node";

    List<GraphEdge> downDottedEdges = myHiddenEdges.getAdjacentEdges(tail, EdgeFilter.NORMAL_DOWN);
    List<GraphEdge> upDottedEdges = myHiddenEdges.getAdjacentEdges(firstChild, EdgeFilter.NORMAL_UP);
    for (GraphEdge e : ContainerUtil.concat(downDottedEdges, upDottedEdges)) {
      myHiddenEdges.removeEdge(e);
      if (e.getType() == GraphEdgeType.DOTTED) {
        result.addAll(expandEdge(e));
      }
      else {
        result.add(e);
      }
    }

    return result;
  }

  public static class WorkingLinearBekGraph extends LinearBekGraph {
    private final LinearBekGraph myLinearGraph;

    public WorkingLinearBekGraph(@Nonnull LinearBekGraph graph) {
      super(graph.myGraph);
      myLinearGraph = graph;
    }

    public Collection<GraphEdge> getAddedEdges() {
      Set<GraphEdge> result = myDottedEdges.getEdges();
      result.removeAll(ContainerUtil.filter(myHiddenEdges.getEdges(), graphEdge-> graphEdge.getType() == GraphEdgeType.DOTTED));
      result.removeAll(myLinearGraph.myDottedEdges.getEdges());
      return result;
    }

    public Collection<GraphEdge> getRemovedEdges() {
      Set<GraphEdge> result = new HashSet<>();
      Set<GraphEdge> hidden = myHiddenEdges.getEdges();
      result.addAll(ContainerUtil.filter(hidden, graphEdge-> graphEdge.getType() != GraphEdgeType.DOTTED));
      result.addAll(ContainerUtil.intersection(hidden, myLinearGraph.myDottedEdges.getEdges()));
      result.removeAll(myLinearGraph.myHiddenEdges.getEdges());
      return result;
    }

    public void applyChanges() {
      myLinearGraph.myDottedEdges.removeAll();
      myLinearGraph.myHiddenEdges.removeAll();

      for (GraphEdge e : myDottedEdges.getEdges()) {
        myLinearGraph.myDottedEdges.createEdge(e);
      }
      for (GraphEdge e : myHiddenEdges.getEdges()) {
        myLinearGraph.myHiddenEdges.createEdge(e);
      }
    }
  }
}
