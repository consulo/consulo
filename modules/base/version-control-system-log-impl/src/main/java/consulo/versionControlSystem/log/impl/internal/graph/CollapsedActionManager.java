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

import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.log.graph.*;
import consulo.versionControlSystem.log.graph.LinearGraphController.LinearGraphAction;
import consulo.versionControlSystem.log.graph.LinearGraphController.LinearGraphAnswer;
import consulo.versionControlSystem.log.graph.action.GraphAction;
import consulo.versionControlSystem.log.impl.internal.util.UnsignedBitSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

class CollapsedActionManager {

  @Nullable
  public static LinearGraphAnswer performAction(@Nonnull CollapsedController graphController, @Nonnull LinearGraphAction action) {
    ActionContext context = new ActionContext(graphController.getCollapsedGraph(), graphController.getPermanentGraphInfo(), action);

    for (ActionCase actionCase : FILTER_ACTION_CASES) {
      if (actionCase.supportedActionTypes().contains(context.getActionType())) {
        LinearGraphAnswer graphAnswer = actionCase.performAction(context);
        if (graphAnswer != null) return graphAnswer;
      }
    }
    return null;
  }

  public static void expandNodes(@Nonnull final CollapsedGraph collapsedGraph, Set<Integer> nodesToShow) {
    FragmentGenerator generator =
      new FragmentGenerator(LinearGraphUtils.asLiteLinearGraph(collapsedGraph.getDelegatedGraph()), collapsedGraph::isNodeVisible);

    CollapsedGraph.Modification modification = collapsedGraph.startModification();

    for (Integer nodeToShow : nodesToShow) {
      if (modification.isNodeShown(nodeToShow)) continue;

      FragmentGenerator.GreenFragment fragment = generator.getGreenFragmentForCollapse(nodeToShow, Integer.MAX_VALUE);
      if (fragment.getUpRedNode() == null ||
          fragment.getDownRedNode() == null ||
          fragment.getUpRedNode().equals(fragment.getDownRedNode())) {
        continue;
      }

      for (Integer n : fragment.getMiddleGreenNodes()) {
        modification.showNode(n);
      }

      modification.removeEdge(GraphEdge.createNormalEdge(fragment.getUpRedNode(), fragment.getDownRedNode(), GraphEdgeType.DOTTED));
    }

    modification.apply();
  }


  private interface ActionCase {
    @Nullable
    LinearGraphAnswer performAction(@Nonnull ActionContext context);

    @Nonnull
    Set<GraphAction.Type> supportedActionTypes();
  }

  private static class ActionContext {
    @Nonnull
    private final CollapsedGraph myCollapsedGraph;
    @Nonnull
    private final LinearGraphAction myGraphAction;
    @Nonnull
    private final FragmentGenerators myDelegatedFragmentGenerators;
    @Nonnull
    private final FragmentGenerators myCompiledFragmentGenerators;

    private ActionContext(@Nonnull CollapsedGraph collapsedGraph,
                          @Nonnull PermanentGraphInfo permanentGraphInfo,
                          @Nonnull LinearGraphAction graphAction) {
      myCollapsedGraph = collapsedGraph;
      myGraphAction = graphAction;
      myDelegatedFragmentGenerators =
        new FragmentGenerators(collapsedGraph.getDelegatedGraph(), permanentGraphInfo, collapsedGraph.getMatchedNodeId());
      myCompiledFragmentGenerators =
        new FragmentGenerators(collapsedGraph.getCompiledGraph(), permanentGraphInfo, collapsedGraph.getMatchedNodeId());
    }

    @Nonnull
    GraphAction.Type getActionType() {
      return myGraphAction.getType();
    }

    @Nullable
    GraphElement getAffectedGraphElement() {
      return myGraphAction.getAffectedElement() == null ? null : myGraphAction.getAffectedElement().getGraphElement();
    }

    @Nonnull
    LinearGraph getDelegatedGraph() {
      return myCollapsedGraph.getDelegatedGraph();
    }

    @Nonnull
    LinearGraph getCompiledGraph() {
      return myCollapsedGraph.getCompiledGraph();
    }

    int convertToDelegateNodeIndex(int compiledNodeIndex) {
      return myCollapsedGraph.convertToDelegateNodeIndex(compiledNodeIndex);
    }

    @Nonnull
    Set<Integer> convertToDelegateNodeIndex(@Nonnull Collection<Integer> compiledNodeIndexes) {
      return ContainerUtil.map2Set(compiledNodeIndexes, this::convertToDelegateNodeIndex);
    }

    @Nonnull
    GraphEdge convertToDelegateEdge(@Nonnull GraphEdge compiledEdge) {
      Integer upNodeIndex = null, downNodeIndex = null;
      if (compiledEdge.getUpNodeIndex() != null) upNodeIndex = convertToDelegateNodeIndex(compiledEdge.getUpNodeIndex());
      if (compiledEdge.getDownNodeIndex() != null) downNodeIndex = convertToDelegateNodeIndex(compiledEdge.getDownNodeIndex());

      return new GraphEdge(upNodeIndex, downNodeIndex, compiledEdge.getTargetId(), compiledEdge.getType());
    }
  }

  private static class FragmentGenerators {
    @Nonnull
    private final FragmentGenerator fragmentGenerator;
    @Nonnull
    private final LinearFragmentGenerator linearFragmentGenerator;

    private FragmentGenerators(
      @Nonnull final LinearGraph linearGraph,
      @Nonnull PermanentGraphInfo<?> permanentGraphInfo,
      @Nonnull final UnsignedBitSet matchedNodeId
    ) {
      fragmentGenerator = new FragmentGenerator(
        LinearGraphUtils.asLiteLinearGraph(linearGraph),
        nodeIndex -> matchedNodeId.get(linearGraph.getNodeId(nodeIndex))
      );

      Set<Integer> branchNodeIndexes = LinearGraphUtils.convertIdsToNodeIndexes(linearGraph, permanentGraphInfo.getBranchNodeIds());
      linearFragmentGenerator = new LinearFragmentGenerator(LinearGraphUtils.asLiteLinearGraph(linearGraph), branchNodeIndexes);
    }
  }

  private final static ActionCase LINEAR_COLLAPSE_CASE = new ActionCase() {
    @Nullable
    @Override
    public LinearGraphAnswer performAction(@Nonnull final ActionContext context) {
      if (isForDelegateGraph(context)) return null;

      GraphElement affectedGraphElement = context.getAffectedGraphElement();
      if (affectedGraphElement == null) return null;

      LinearFragmentGenerator compiledLinearFragmentGenerator = context.myCompiledFragmentGenerators.linearFragmentGenerator;
      FragmentGenerator compiledFragmentGenerator = context.myCompiledFragmentGenerators.fragmentGenerator;

      if (context.getActionType() == GraphAction.Type.MOUSE_OVER) {
        LinearFragmentGenerator.GraphFragment fragment = compiledLinearFragmentGenerator.getPartLongFragment(affectedGraphElement);
        if (fragment == null) return null;
        Set<Integer> middleCompiledNodes = compiledFragmentGenerator.getMiddleNodes(fragment.upNodeIndex, fragment.downNodeIndex, false);
        return LinearGraphUtils.createSelectedAnswer(context.getCompiledGraph(), middleCompiledNodes);
      }

      LinearFragmentGenerator.GraphFragment fragment = compiledLinearFragmentGenerator.getLongFragment(affectedGraphElement);
      if (fragment == null) return null;

      Set<Integer> middleCompiledNodes = compiledFragmentGenerator.getMiddleNodes(fragment.upNodeIndex, fragment.downNodeIndex, true);
      Set<GraphEdge> dottedCompiledEdges = new HashSet<>();
      for (Integer middleNodeIndex : middleCompiledNodes) {
        dottedCompiledEdges.addAll(ContainerUtil.filter(
          context.getCompiledGraph().getAdjacentEdges(middleNodeIndex, EdgeFilter.NORMAL_ALL),
          edge -> edge.getType() == GraphEdgeType.DOTTED
        ));
      }

      int upNodeIndex = context.convertToDelegateNodeIndex(fragment.upNodeIndex);
      int downNodeIndex = context.convertToDelegateNodeIndex(fragment.downNodeIndex);
      Set<Integer> middleNodes = context.convertToDelegateNodeIndex(middleCompiledNodes);
      Set<GraphEdge> dottedEdges = ContainerUtil.map2Set(dottedCompiledEdges, context::convertToDelegateEdge);

      CollapsedGraph.Modification modification = context.myCollapsedGraph.startModification();
      for (GraphEdge edge : dottedEdges) modification.removeEdge(edge);
      for (Integer middleNode : middleNodes) modification.hideNode(middleNode);
      modification.createEdge(new GraphEdge(upNodeIndex, downNodeIndex, null, GraphEdgeType.DOTTED));

      modification.apply();
      return new LinearGraphController.LinearGraphAnswer(GraphChangesUtil.SOME_CHANGES);
    }

    @Nonnull
    @Override
    public Set<GraphAction.Type> supportedActionTypes() {
      return Set.of(GraphAction.Type.MOUSE_CLICK, GraphAction.Type.MOUSE_OVER);
    }
  };

  private final static ActionCase EXPAND_ALL = new ActionCase() {
    @Nullable
    @Override
    public LinearGraphAnswer performAction(@Nonnull ActionContext context) {
      CollapsedGraph.Modification modification = context.myCollapsedGraph.startModification();
      modification.removeAdditionalEdges();
      modification.resetNodesVisibility();
      return new DeferredGraphAnswer(GraphChangesUtil.SOME_CHANGES, modification);
    }

    @Nonnull
    @Override
    public Set<GraphAction.Type> supportedActionTypes() {
      return Collections.singleton(GraphAction.Type.BUTTON_EXPAND);
    }
  };

  private final static ActionCase COLLAPSE_ALL = new ActionCase() {
    @Nonnull
    @Override
    public LinearGraphAnswer performAction(@Nonnull ActionContext context) {
      CollapsedGraph.Modification modification = context.myCollapsedGraph.startModification();
      modification.removeAdditionalEdges();
      modification.resetNodesVisibility();

      LinearGraph delegateGraph = context.getDelegatedGraph();
      for (int nodeIndex = 0; nodeIndex < delegateGraph.nodesCount(); nodeIndex++) {
        if (modification.isNodeHidden(nodeIndex)) continue;

        LinearFragmentGenerator.GraphFragment fragment = context.myDelegatedFragmentGenerators.linearFragmentGenerator.getLongDownFragment(nodeIndex);
        if (fragment != null) {
          Set<Integer> middleNodes =
            context.myDelegatedFragmentGenerators.fragmentGenerator.getMiddleNodes(fragment.upNodeIndex, fragment.downNodeIndex, true);

          for (Integer nodeIndexForHide : middleNodes) modification.hideNode(nodeIndexForHide);
          modification.createEdge(new GraphEdge(fragment.upNodeIndex, fragment.downNodeIndex, null, GraphEdgeType.DOTTED));
        }
      }

      return new DeferredGraphAnswer(GraphChangesUtil.SOME_CHANGES, modification);
    }

    @Nonnull
    @Override
    public Set<GraphAction.Type> supportedActionTypes() {
      return Collections.singleton(GraphAction.Type.BUTTON_COLLAPSE);
    }
  };

  private final static ActionCase LINEAR_EXPAND_CASE = new ActionCase() {
    @Nullable
    @Override
    public LinearGraphAnswer performAction(@Nonnull ActionContext context) {
      if (isForDelegateGraph(context)) return null;

      GraphEdge dottedEdge = getDottedEdge(context.getAffectedGraphElement(), context.getCompiledGraph());

      if (dottedEdge != null) {
        int upNodeIndex = context.convertToDelegateNodeIndex(assertInt(dottedEdge.getUpNodeIndex()));
        int downNodeIndex = context.convertToDelegateNodeIndex(assertInt(dottedEdge.getDownNodeIndex()));

        if (context.getActionType() == GraphAction.Type.MOUSE_OVER) {
          return LinearGraphUtils.createSelectedAnswer(context.getDelegatedGraph(), Set.of(upNodeIndex, downNodeIndex));
        }

        Set<Integer> middleNodes = context.myDelegatedFragmentGenerators.fragmentGenerator.getMiddleNodes(upNodeIndex, downNodeIndex, true);

        CollapsedGraph.Modification modification = context.myCollapsedGraph.startModification();
        for (Integer middleNode : middleNodes) {
          modification.showNode(middleNode);
        }
        modification.removeEdge(new GraphEdge(upNodeIndex, downNodeIndex, null, GraphEdgeType.DOTTED));

        modification.apply();
        return new LinearGraphController.LinearGraphAnswer(GraphChangesUtil.SOME_CHANGES);
      }

      return null;
    }

    @Nonnull
    @Override
    public Set<GraphAction.Type> supportedActionTypes() {
      return Set.of(GraphAction.Type.MOUSE_CLICK, GraphAction.Type.MOUSE_OVER);
    }
  };

  private final static List<ActionCase> FILTER_ACTION_CASES =
    ContainerUtil.list(COLLAPSE_ALL, EXPAND_ALL, LINEAR_EXPAND_CASE, LINEAR_COLLAPSE_CASE);

  private static boolean isForDelegateGraph(@Nonnull ActionContext context) {
    GraphElement affectedGraphElement = context.getAffectedGraphElement();
    if (affectedGraphElement == null) return false;

    GraphEdge dottedEdge = getDottedEdge(context.getAffectedGraphElement(), context.getCompiledGraph());
    if (dottedEdge != null) {
      int upNodeIndex = context.convertToDelegateNodeIndex(assertInt(dottedEdge.getUpNodeIndex()));
      int downNodeIndex = context.convertToDelegateNodeIndex(assertInt(dottedEdge.getDownNodeIndex()));

      if (!context.myCollapsedGraph.isMyCollapsedEdge(upNodeIndex, downNodeIndex)) return true;
    }
    return false;
  }

  private CollapsedActionManager() {
  }

  private static int assertInt(@Nullable Integer value) {
    assert value != null;
    return value;
  }

  @Nullable
  private static GraphEdge getDottedEdge(@Nullable GraphElement graphElement, @Nonnull LinearGraph graph) {
    if (graphElement == null) return null;

    if (graphElement instanceof GraphEdge edge && edge.getType() == GraphEdgeType.DOTTED) {
      return edge;
    }
    if (graphElement instanceof GraphNode node) {
      for (GraphEdge edge : graph.getAdjacentEdges(node.getNodeIndex(), EdgeFilter.NORMAL_ALL)) {
        if (edge.getType() == GraphEdgeType.DOTTED) return edge;
      }
    }

    return null;
  }

  private static class DeferredGraphAnswer extends LinearGraphController.LinearGraphAnswer {
    @Nonnull
    private final CollapsedGraph.Modification myModification;

    public DeferredGraphAnswer(@Nullable GraphChanges<Integer> graphChanges, @Nonnull CollapsedGraph.Modification modification) {
      super(graphChanges);
      myModification = modification;
    }

    @Nullable
    @Override
    public Runnable getGraphUpdater() {
      return myModification::apply;
    }
  }
}
