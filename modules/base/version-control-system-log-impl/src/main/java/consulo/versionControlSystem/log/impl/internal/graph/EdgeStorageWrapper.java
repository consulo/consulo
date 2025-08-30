/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.function.Functions;
import consulo.versionControlSystem.log.graph.EdgeFilter;
import consulo.versionControlSystem.log.graph.GraphEdge;
import consulo.versionControlSystem.log.graph.GraphEdgeType;
import consulo.versionControlSystem.log.graph.LinearGraph;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static consulo.versionControlSystem.log.graph.LinearGraphUtils.intEqual;

public class EdgeStorageWrapper {
  @Nonnull
  private final EdgeStorage myEdgeStorage;
  @Nonnull
  private final Function<Integer, Integer> myGetNodeIndexById;
  @Nonnull
  private final Function<Integer, Integer> myGetNodeIdByIndex;

  public EdgeStorageWrapper(@Nonnull EdgeStorage edgeStorage, @Nonnull final LinearGraph graph) {
    this(edgeStorage, graph::getNodeIndex, graph::getNodeId);
  }

  public EdgeStorageWrapper(
    @Nonnull EdgeStorage edgeStorage,
    @Nonnull Function<Integer, Integer> getNodeIndexById,
    @Nonnull Function<Integer, Integer> getNodeIdByIndex
  ) {
    myEdgeStorage = edgeStorage;
    myGetNodeIndexById = getNodeIndexById;
    myGetNodeIdByIndex = getNodeIdByIndex;
  }

  public void removeEdge(@Nonnull GraphEdge graphEdge) {
    Couple<Integer> nodeIds = getNodeIds(graphEdge);
    myEdgeStorage.removeEdge(nodeIds.first, nodeIds.second, graphEdge.getType());
  }

  public void createEdge(@Nonnull GraphEdge graphEdge) {
    Couple<Integer> nodeIds = getNodeIds(graphEdge);
    myEdgeStorage.createEdge(nodeIds.first, nodeIds.second, graphEdge.getType());
  }

  public boolean hasEdge(int fromIndex, int toIndex) {
    int toId = myGetNodeIdByIndex.apply(toIndex);
    for (Pair<Integer, GraphEdgeType> edge : myEdgeStorage.getEdges(myGetNodeIdByIndex.apply(fromIndex))) {
      if (edge.second.isNormalEdge() && intEqual(edge.first, toId)) return true;
    }
    return false;
  }

  @Nonnull
  public List<GraphEdge> getAdjacentEdges(int nodeIndex, @Nonnull EdgeFilter filter) {
    List<GraphEdge> result = new SmartList<>();
    for (Pair<Integer, GraphEdgeType> retrievedEdge : myEdgeStorage.getEdges(myGetNodeIdByIndex.apply(nodeIndex))) {
      GraphEdge edge = decompressEdge(nodeIndex, retrievedEdge.first, retrievedEdge.second);
      if (matchedEdge(nodeIndex, edge, filter)) result.add(edge);
    }
    return result;
  }

  @Nonnull
  public Set<GraphEdge> getEdges() {
    Set<GraphEdge> result = new HashSet<>();
    for (int id : myEdgeStorage.getKnownIds()) {
      result.addAll(getAdjacentEdges(myGetNodeIndexById.apply(id), EdgeFilter.ALL));
    }
    return result;
  }

  @Nonnull
  private Couple<Integer> getNodeIds(@Nonnull GraphEdge graphEdge) {
    if (graphEdge.getUpNodeIndex() != null) {
      Integer mainId = myGetNodeIdByIndex.apply(graphEdge.getUpNodeIndex());
      if (graphEdge.getDownNodeIndex() != null) {
        return Couple.of(mainId, myGetNodeIdByIndex.apply(graphEdge.getDownNodeIndex()));
      }
      else {
        return Couple.of(mainId, convertToInt(graphEdge.getTargetId()));
      }
    }
    else {
      assert graphEdge.getDownNodeIndex() != null;
      return Couple.of(myGetNodeIdByIndex.apply(graphEdge.getDownNodeIndex()), convertToInt(graphEdge.getTargetId()));
    }
  }

  @Nullable
  private GraphEdge decompressEdge(int nodeIndex, @Nullable Integer targetId, @Nonnull GraphEdgeType edgeType) {
    if (edgeType.isNormalEdge()) {
      assert targetId != null;
      Integer anotherNodeIndex = myGetNodeIndexById.apply(targetId);
      if (anotherNodeIndex == null) return null; // todo edge to hide node

      return GraphEdge.createNormalEdge(nodeIndex, anotherNodeIndex, edgeType);
    }
    else {
      return GraphEdge.createEdgeWithTargetId(nodeIndex, targetId, edgeType);
    }
  }

  private static boolean matchedEdge(int startNodeIndex, @Nullable GraphEdge edge, @Nonnull EdgeFilter filter) {
    if (edge == null) return false;
    if (edge.getType().isNormalEdge()) {
      return (startNodeIndex == convertToInt(edge.getDownNodeIndex()) && filter.upNormal) ||
             (startNodeIndex == convertToInt(edge.getUpNodeIndex()) && filter.downNormal);
    }
    else {
      return filter.special;
    }
  }

  private static int convertToInt(@Nullable Integer value) {
    return value == null ? EdgeStorage.NULL_ID : value;
  }

  public void removeAll() {
    myEdgeStorage.removeAll();
  }

  public static EdgeStorageWrapper createSimpleEdgeStorage() {
    return new EdgeStorageWrapper(new EdgeStorage(), Functions.<Integer>id(), Functions.<Integer>id());
  }
}
