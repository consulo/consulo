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
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.versionControlSystem.log.graph.EdgeFilter;
import consulo.versionControlSystem.log.graph.GraphEdge;
import consulo.versionControlSystem.log.graph.GraphNode;
import consulo.versionControlSystem.log.graph.LinearGraph;
import consulo.versionControlSystem.log.impl.internal.util.ListIntToIntMap;
import consulo.versionControlSystem.log.impl.internal.util.UnsignedBitSet;
import consulo.versionControlSystem.log.impl.internal.util.UpdatableIntToIntMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.PrimitiveIterator;
import java.util.concurrent.atomic.AtomicReference;

public class CollapsedGraph {

  public static CollapsedGraph newInstance(@Nonnull LinearGraph delegateGraph, @Nonnull UnsignedBitSet matchedNodeId) {
    return new CollapsedGraph(delegateGraph, matchedNodeId, matchedNodeId.clone(), new EdgeStorage());
  }

  public static CollapsedGraph updateInstance(@Nonnull CollapsedGraph prevCollapsedGraph, @Nonnull LinearGraph newDelegateGraph) {
    UnsignedBitSet visibleNodesId = prevCollapsedGraph.myDelegateNodesVisibility.getNodeVisibilityById();
    return new CollapsedGraph(newDelegateGraph, prevCollapsedGraph.myMatchedNodeId, visibleNodesId, prevCollapsedGraph.myEdgeStorage);
  }

  @Nonnull
  private final LinearGraph myDelegatedGraph;
  @Nonnull
  private final UnsignedBitSet myMatchedNodeId;
  @Nonnull
  private final GraphNodesVisibility myDelegateNodesVisibility;
  @Nonnull
  private final UpdatableIntToIntMap myNodesMap;
  @Nonnull
  private final EdgeStorage myEdgeStorage;
  @Nonnull
  private final CompiledGraph myCompiledGraph;
  @Nonnull
  private final AtomicReference<Modification> myCurrentModification = new AtomicReference<>(null);


  private CollapsedGraph(@Nonnull LinearGraph delegatedGraph,
                         @Nonnull UnsignedBitSet matchedNodeId,
                         @Nonnull UnsignedBitSet visibleNodesId,
                         @Nonnull EdgeStorage edgeStorage) {
    myDelegatedGraph = delegatedGraph;
    myMatchedNodeId = matchedNodeId;
    myDelegateNodesVisibility = new GraphNodesVisibility(delegatedGraph, visibleNodesId);
    myNodesMap = ListIntToIntMap.newInstance(myDelegateNodesVisibility.asFlags());
    myEdgeStorage = edgeStorage;
    myCompiledGraph = new CompiledGraph();
  }

  @Nonnull
  public LinearGraph getDelegatedGraph() {
    return myDelegatedGraph;
  }

  public boolean isNodeVisible(int delegateNodeIndex) {
    return myDelegateNodesVisibility.isVisible(delegateNodeIndex);
  }

  @Nonnull
  public Modification startModification() {
    Modification modification = new Modification();
    if (myCurrentModification.compareAndSet(null, modification)) {
      return modification;
    }
    throw new RuntimeException("Can not start a new modification while the other one is still running.");
  }

  @Nonnull
  public LinearGraph getCompiledGraph() {
    assertNotUnderModification();
    return myCompiledGraph;
  }

  public int convertToDelegateNodeIndex(int compiledNodeIndex) {
    assertNotUnderModification();
    return myNodesMap.getLongIndex(compiledNodeIndex);
  }

  @Nonnull
  public UnsignedBitSet getMatchedNodeId() {
    return myMatchedNodeId;
  }

  // todo proper name
  public boolean isMyCollapsedEdge(int upNodeIndex, int downNodeIndex) {
    return new EdgeStorageWrapper(myEdgeStorage, myDelegatedGraph).hasEdge(upNodeIndex, downNodeIndex);
  }

  // everywhere in this class "nodeIndexes" means "node indexes in delegated graph"
  public class Modification {
    private static final int COLLECTING = 0;
    private static final int APPLYING = 1;
    private static final int DONE = 2;

    @Nonnull
    private final EdgeStorageWrapper myEdgesToAdd = EdgeStorageWrapper.createSimpleEdgeStorage();
    @Nonnull
    private final EdgeStorageWrapper myEdgesToRemove = EdgeStorageWrapper.createSimpleEdgeStorage();
    @Nonnull
    private final IntSet myNodesToHide = IntSets.newHashSet();
    @Nonnull
    private final IntSet myNodesToShow = IntSets.newHashSet();
    private boolean myClearEdges = false;
    private boolean myClearVisibility = false;

    private volatile int myProgress = COLLECTING;
    private int minAffectedNodeIndex = Integer.MAX_VALUE;
    private int maxAffectedNodeIndex = Integer.MIN_VALUE;

    private void touchIndex(int nodeIndex) {
      assert myProgress == COLLECTING;
      minAffectedNodeIndex = Math.min(minAffectedNodeIndex, nodeIndex);
      maxAffectedNodeIndex = Math.max(maxAffectedNodeIndex, nodeIndex);
    }

    private void touchAll() {
      assert myProgress == COLLECTING;
      minAffectedNodeIndex = 0;
      maxAffectedNodeIndex = getDelegatedGraph().nodesCount() - 1;
    }

    private void touchEdge(@Nonnull GraphEdge edge) {
      assert myProgress == COLLECTING;
      if (edge.getUpNodeIndex() != null) touchIndex(edge.getUpNodeIndex());
      if (edge.getDownNodeIndex() != null) touchIndex(edge.getDownNodeIndex());
    }

    public void showNode(int nodeIndex) {
      assert myProgress == COLLECTING;
      myNodesToShow.add(nodeIndex);
      touchIndex(nodeIndex);
    }

    public void hideNode(int nodeIndex) {
      assert myProgress == COLLECTING;
      myNodesToHide.add(nodeIndex);
      touchIndex(nodeIndex);
    }

    public void createEdge(@Nonnull GraphEdge edge) {
      assert myProgress == COLLECTING;
      myEdgesToAdd.createEdge(edge);
      touchEdge(edge);
    }

    public void removeEdge(@Nonnull GraphEdge edge) { // todo add support for removing edge from delegate graph
      assert myProgress == COLLECTING;
      myEdgesToRemove.createEdge(edge);
      touchEdge(edge);
    }

    public void removeAdditionalEdges() {
      assert myProgress == COLLECTING;
      myClearEdges = true;
      touchAll();
    }

    public void resetNodesVisibility() {
      assert myProgress == COLLECTING;
      myClearVisibility = true;
      touchAll();
    }

    // "package private" means "I'm not entirely happy about this method"
    @Nonnull
    /*package private*/ EdgeStorageWrapper getEdgesToAdd() {
      assert myProgress == COLLECTING;
      return myEdgesToAdd;
    }

    /*package private*/
    boolean isNodeHidden(int nodeIndex) {
      assert myProgress == COLLECTING;
      return myNodesToHide.contains(nodeIndex);
    }

    /*package private*/
    boolean isNodeShown(int nodeIndex) {
      assert myProgress == COLLECTING;
      return myNodesToShow.contains(nodeIndex);
    }

    public void apply() {
      assert myCurrentModification.get() == this;
      myProgress = APPLYING;

      if (myClearVisibility) {
        myDelegateNodesVisibility.setNodeVisibilityById(myMatchedNodeId.clone());
      }
      if (myClearEdges) {
        myEdgeStorage.removeAll();
      }

      PrimitiveIterator.OfInt toShow = myNodesToShow.iterator();
      while (toShow.hasNext()) {
        myDelegateNodesVisibility.show(toShow.next());
      }
      PrimitiveIterator.OfInt toHide = myNodesToHide.iterator();
      while (toHide.hasNext()) {
        myDelegateNodesVisibility.hide(toHide.next());
      }

      EdgeStorageWrapper edgeStorageWrapper = new EdgeStorageWrapper(myEdgeStorage, getDelegatedGraph());
      for (GraphEdge edge : myEdgesToAdd.getEdges()) {
        edgeStorageWrapper.createEdge(edge);
      }
      for (GraphEdge edge : myEdgesToRemove.getEdges()) {
        edgeStorageWrapper.removeEdge(edge);
      }

      if (minAffectedNodeIndex != Integer.MAX_VALUE && maxAffectedNodeIndex != Integer.MIN_VALUE) {
        myNodesMap.update(minAffectedNodeIndex, maxAffectedNodeIndex);
      }

      myProgress = DONE;
      myCurrentModification.set(null);
    }
  }

  private void assertNotUnderModification() {
    Modification modification = myCurrentModification.get();
    if (modification != null && modification.myProgress == Modification.APPLYING) {
      throw new IllegalStateException("CompiledGraph is under modification");
    }
  }

  private class CompiledGraph implements LinearGraph {
    @Nonnull
    private final EdgeStorageWrapper myEdgeStorageWrapper;

    private CompiledGraph() {
      myEdgeStorageWrapper = new EdgeStorageWrapper(myEdgeStorage, this);
    }

    @Override
    public int nodesCount() {
      assertNotUnderModification();
      return myNodesMap.shortSize();
    }

    @Nonnull
    private GraphEdge createEdge(@Nonnull GraphEdge delegateEdge, @Nullable Integer upNodeIndex, @Nullable Integer downNodeIndex) {
      return new GraphEdge(upNodeIndex, downNodeIndex, delegateEdge.getTargetId(), delegateEdge.getType());
    }

    @Nullable
    private Integer compiledNodeIndex(@Nullable Integer delegateNodeIndex) {
      if (delegateNodeIndex == null) return null;
      if (myDelegateNodesVisibility.isVisible(delegateNodeIndex)) {
        return myNodesMap.getShortIndex(delegateNodeIndex);
      }
      else {
        return -1;
      }
    }

    private boolean isVisibleEdge(@Nullable Integer compiledUpNode, @Nullable Integer compiledDownNode) {
      return (compiledUpNode == null || compiledUpNode != -1) && (compiledDownNode == null || compiledDownNode != -1);
    }

    @Nonnull
    @Override
    public List<GraphEdge> getAdjacentEdges(int nodeIndex, @Nonnull EdgeFilter filter) {
      assertNotUnderModification();
      List<GraphEdge> result = new SmartList<>();
      int delegateIndex = myNodesMap.getLongIndex(nodeIndex);

      // add delegate edges
      for (GraphEdge delegateEdge : myDelegatedGraph.getAdjacentEdges(delegateIndex, filter)) {
        Integer compiledUpIndex = compiledNodeIndex(delegateEdge.getUpNodeIndex());
        Integer compiledDownIndex = compiledNodeIndex(delegateEdge.getDownNodeIndex());
        if (isVisibleEdge(compiledUpIndex, compiledDownIndex)) result.add(createEdge(delegateEdge, compiledUpIndex, compiledDownIndex));
      }

      result.addAll(myEdgeStorageWrapper.getAdjacentEdges(nodeIndex, filter));

      return result;
    }

    @Nonnull
    @Override
    public GraphNode getGraphNode(int nodeIndex) {
      assertNotUnderModification();
      int delegateIndex = myNodesMap.getLongIndex(nodeIndex);
      GraphNode graphNode = myDelegatedGraph.getGraphNode(delegateIndex);
      return new GraphNode(nodeIndex, graphNode.getType());
    }

    @Override
    public int getNodeId(int nodeIndex) {
      assertNotUnderModification();
      int delegateIndex = myNodesMap.getLongIndex(nodeIndex);
      return myDelegatedGraph.getNodeId(delegateIndex);
    }

    @Override
    @Nullable
    public Integer getNodeIndex(int nodeId) {
      assertNotUnderModification();
      Integer delegateIndex = myDelegatedGraph.getNodeIndex(nodeId);
      if (delegateIndex == null) return null;
      if (myDelegateNodesVisibility.isVisible(delegateIndex)) {
        return myNodesMap.getShortIndex(delegateIndex);
      }
      else {
        return null;
      }
    }
  }
}
