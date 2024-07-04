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
package consulo.ide.impl.idea.vcs.log.graph.collapsing;

import consulo.ide.impl.idea.vcs.log.graph.api.LinearGraph;
import consulo.ide.impl.idea.vcs.log.graph.api.elements.GraphEdge;
import consulo.ide.impl.idea.vcs.log.graph.api.elements.GraphElement;
import consulo.ide.impl.idea.vcs.log.graph.api.elements.GraphNode;
import consulo.ide.impl.idea.vcs.log.graph.api.permanent.PermanentGraphInfo;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.CascadeController;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.GraphChanges;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.ReachableNodes;
import consulo.ide.impl.idea.vcs.log.graph.utils.UnsignedBitSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class CollapsedController extends CascadeController {
  @Nonnull
  private CollapsedGraph myCollapsedGraph;

  public CollapsedController(@Nonnull CascadeController delegateLinearGraphController,
                             @Nonnull final PermanentGraphInfo<?> permanentGraphInfo,
                             @Nullable Set<Integer> idsOfVisibleBranches) {
    super(delegateLinearGraphController, permanentGraphInfo);
    UnsignedBitSet initVisibility =
      ReachableNodes.getReachableNodes(permanentGraphInfo.getLinearGraph(), idsOfVisibleBranches);
    myCollapsedGraph = CollapsedGraph.newInstance(getDelegateController().getCompiledGraph(), initVisibility);
  }

  @Nonnull
  @Override
  protected LinearGraphAnswer delegateGraphChanged(@Nonnull LinearGraphAnswer delegateAnswer) {
    if (delegateAnswer.getGraphChanges() != null) {
      LinearGraph delegateGraph = getDelegateController().getCompiledGraph();
      myCollapsedGraph = CollapsedGraph.updateInstance(myCollapsedGraph, delegateGraph);

      // some new edges and node appeared, so we expand them
      applyDelegateChanges(delegateGraph, delegateAnswer.getGraphChanges());
    }
    return delegateAnswer; // if somebody outside actually uses changes we return here they are screwed
  }

  private void applyDelegateChanges(LinearGraph graph, GraphChanges<Integer> changes) {
    Set<Integer> nodesToShow = new HashSet<>();

    for (GraphChanges.Edge<Integer> e : changes.getChangedEdges()) {
      if (!e.removed()) {
        Integer upId = e.upNodeId();
        if (upId != null) {
          Integer upIndex = graph.getNodeIndex(upId);
          if (upIndex != null) {
            nodesToShow.add(upIndex);
          }
        }
        Integer downId = e.downNodeId();
        if (downId != null) {
          Integer downIndex = graph.getNodeIndex(downId);
          if (downIndex != null) {
            nodesToShow.add(downIndex);
          }
        }
      }
    }

    for (GraphChanges.Node<Integer> e : changes.getChangedNodes()) {
      if (!e.removed()) {
        Integer nodeIndex = graph.getNodeIndex(e.getNodeId());
        if (nodeIndex != null) {
          nodesToShow.add(nodeIndex);
        }
      }
    }

    CollapsedActionManager.expandNodes(myCollapsedGraph, nodesToShow);
  }

  @Nullable
  @Override
  protected LinearGraphAnswer performAction(@Nonnull LinearGraphAction action) {
    return CollapsedActionManager.performAction(this, action);
  }

  @Nonnull
  @Override
  public LinearGraph getCompiledGraph() {
    return myCollapsedGraph.getCompiledGraph();
  }

  @Nonnull
  protected CollapsedGraph getCollapsedGraph() {
    return myCollapsedGraph;
  }

  @Nullable
  @Override
  protected GraphElement convertToDelegate(@Nonnull GraphElement graphElement) {
    return convertToDelegate(graphElement, myCollapsedGraph);
  }

  @Nullable
  public static GraphElement convertToDelegate(@Nonnull GraphElement graphElement, CollapsedGraph collapsedGraph) {
    if (graphElement instanceof GraphEdge edge) {
      Integer upIndex = edge.getUpNodeIndex();
      Integer downIndex = edge.getDownNodeIndex();
      if (upIndex != null && downIndex != null && collapsedGraph.isMyCollapsedEdge(upIndex, downIndex)) return null;

      Integer convertedUpIndex = upIndex == null ? null : collapsedGraph.convertToDelegateNodeIndex(upIndex);
      Integer convertedDownIndex = downIndex == null ? null : collapsedGraph.convertToDelegateNodeIndex(downIndex);

      return new GraphEdge(convertedUpIndex, convertedDownIndex, edge.getTargetId(), edge.getType());
    }
    else if (graphElement instanceof GraphNode node) {
      return new GraphNode(collapsedGraph.convertToDelegateNodeIndex(node.getNodeIndex()), node.getType());
    }
    return null;
  }
}
