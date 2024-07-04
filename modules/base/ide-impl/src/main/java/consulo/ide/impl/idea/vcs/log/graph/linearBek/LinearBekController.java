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
package consulo.ide.impl.idea.vcs.log.graph.linearBek;

import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcs.log.graph.api.EdgeFilter;
import consulo.ide.impl.idea.vcs.log.graph.api.GraphLayout;
import consulo.ide.impl.idea.vcs.log.graph.api.LinearGraph;
import consulo.ide.impl.idea.vcs.log.graph.api.elements.GraphEdge;
import consulo.ide.impl.idea.vcs.log.graph.api.elements.GraphEdgeType;
import consulo.ide.impl.idea.vcs.log.graph.api.elements.GraphElement;
import consulo.ide.impl.idea.vcs.log.graph.api.elements.GraphNode;
import consulo.ide.impl.idea.vcs.log.graph.api.permanent.PermanentGraphInfo;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.BekBaseController;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.CascadeController;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.GraphChangesUtil;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.bek.BekIntMap;
import consulo.ide.impl.idea.vcs.log.graph.utils.LinearGraphUtils;
import consulo.logging.Logger;
import consulo.versionControlSystem.log.graph.action.GraphAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class LinearBekController extends CascadeController {
  private static final Logger LOG = Logger.getInstance(LinearBekController.class);
  @Nonnull
  private final LinearBekGraph myCompiledGraph;
  private final LinearBekGraphBuilder myLinearBekGraphBuilder;
  private final BekGraphLayout myBekGraphLayout;

  public LinearBekController(@Nonnull BekBaseController controller, @Nonnull PermanentGraphInfo permanentGraphInfo) {
    super(controller, permanentGraphInfo);
    myCompiledGraph = new LinearBekGraph(getDelegateGraph());
    myBekGraphLayout = new BekGraphLayout(permanentGraphInfo.getPermanentGraphLayout(), controller.getBekIntMap());
    myLinearBekGraphBuilder = new LinearBekGraphBuilder(myCompiledGraph, myBekGraphLayout);

    long start = System.currentTimeMillis();
    myLinearBekGraphBuilder.collapseAll();
    LOG.info("Linear bek took " + (System.currentTimeMillis() - start) / 1000.0 + " sec");
  }

  @Nonnull
  @Override
  protected LinearGraphAnswer delegateGraphChanged(@Nonnull LinearGraphAnswer delegateAnswer) {
    return delegateAnswer;
  }

  @Nullable
  @Override
  protected LinearGraphAnswer performAction(@Nonnull LinearGraphAction action) {
    if (action.getAffectedElement() != null) {
      if (action.getType() == GraphAction.Type.MOUSE_CLICK) {
        GraphElement graphElement = action.getAffectedElement().getGraphElement();
        if (graphElement instanceof GraphNode node) {
          LinearGraphAnswer answer = collapseNode(node);
          if (answer != null) return answer;
          for (GraphEdge dottedEdge : getAllAdjacentDottedEdges(node)) {
            LinearGraphAnswer expandedAnswer = expandEdge(dottedEdge);
            if (expandedAnswer != null) return expandedAnswer;
          }
        }
        else if (graphElement instanceof GraphEdge edge) {
          return expandEdge(edge);
        }
      }
      else if (action.getType() == GraphAction.Type.MOUSE_OVER) {
        GraphElement graphElement = action.getAffectedElement().getGraphElement();
        if (graphElement instanceof GraphNode node) {
          LinearGraphAnswer answer = highlightNode(node);
          if (answer != null) return answer;
          for (GraphEdge dottedEdge : getAllAdjacentDottedEdges(node)) {
            LinearGraphAnswer highlightAnswer = highlightEdge(dottedEdge);
            if (highlightAnswer != null) return highlightAnswer;
          }
        }
        else if (graphElement instanceof GraphEdge edge) {
          return highlightEdge(edge);
        }
      }
    }
    else if (action.getType() == GraphAction.Type.BUTTON_COLLAPSE) {
      return collapseAll();
    }
    else if (action.getType() == GraphAction.Type.BUTTON_EXPAND) {
      return expandAll();
    }
    return null;
  }

  @Nonnull
  private List<GraphEdge> getAllAdjacentDottedEdges(GraphNode graphElement) {
    return ContainerUtil.filter(
      myCompiledGraph.getAdjacentEdges(graphElement.getNodeIndex(), EdgeFilter.ALL),
      graphEdge -> graphEdge.getType() == GraphEdgeType.DOTTED
    );
  }

  @Nonnull
  private LinearGraphAnswer expandAll() {
    return new LinearGraphAnswer(GraphChangesUtil.SOME_CHANGES) {
      @Nullable
      @Override
      public Runnable getGraphUpdater() {
        return () -> {
          myCompiledGraph.myDottedEdges.removeAll();
          myCompiledGraph.myHiddenEdges.removeAll();
        };
      }
    };
  }

  @Nonnull
  private LinearGraphAnswer collapseAll() {
    final LinearBekGraph.WorkingLinearBekGraph workingGraph = new LinearBekGraph.WorkingLinearBekGraph(myCompiledGraph);
    new LinearBekGraphBuilder(workingGraph, myBekGraphLayout).collapseAll();
    return new LinearGraphAnswer(
      GraphChangesUtil.edgesReplaced(workingGraph.getRemovedEdges(), workingGraph.getAddedEdges(), getDelegateGraph())) {
      @Nullable
      @Override
      public Runnable getGraphUpdater() {
        return workingGraph::applyChanges;
      }
    };
  }

  @Nullable
  private LinearGraphAnswer highlightNode(GraphNode node) {
    Set<LinearBekGraphBuilder.MergeFragment> toCollapse = collectFragmentsToCollapse(node);
    if (toCollapse.isEmpty()) return null;

    Set<Integer> toHighlight = new HashSet<>();
    for (LinearBekGraphBuilder.MergeFragment fragment : toCollapse) {
      toHighlight.addAll(fragment.getAllNodes());
    }

    return LinearGraphUtils.createSelectedAnswer(myCompiledGraph, toHighlight);
  }

  @Nullable
  private LinearGraphAnswer highlightEdge(GraphEdge edge) {
    if (edge.getType() == GraphEdgeType.DOTTED) {
      return LinearGraphUtils.createSelectedAnswer(myCompiledGraph, ContainerUtil.set(edge.getUpNodeIndex(), edge.getDownNodeIndex()));
    }
    return null;
  }

  @Nullable
  private LinearGraphAnswer collapseNode(GraphNode node) {
    SortedSet<Integer> toCollapse = collectNodesToCollapse(node);

    if (toCollapse.isEmpty()) return null;

    for (Integer i : toCollapse) {
      myLinearBekGraphBuilder.collapseFragment(i);
    }
    return new LinearGraphAnswer(GraphChangesUtil.SOME_CHANGES);
  }

  private SortedSet<Integer> collectNodesToCollapse(GraphNode node) {
    SortedSet<Integer> toCollapse = new TreeSet<>((Comparator<Integer>)(o1, o2) -> o2.compareTo(o1));
    for (LinearBekGraphBuilder.MergeFragment f : collectFragmentsToCollapse(node)) {
      toCollapse.add(f.getParent());
      toCollapse.addAll(f.getTailsAndBody());
    }
    return toCollapse;
  }

  @Nonnull
  private Set<LinearBekGraphBuilder.MergeFragment> collectFragmentsToCollapse(GraphNode node) {
    Set<LinearBekGraphBuilder.MergeFragment> result = new HashSet<>();

    int mergesCount = 0;

    LinkedHashSet<Integer> toProcess = new LinkedHashSet<>();
    toProcess.add(node.getNodeIndex());
    while (!toProcess.isEmpty()) {
      Integer i = ContainerUtil.getFirstItem(toProcess);
      toProcess.remove(i);

      LinearBekGraphBuilder.MergeFragment fragment = myLinearBekGraphBuilder.getFragment(i);
      if (fragment == null) continue;

      result.add(fragment);
      toProcess.addAll(fragment.getTailsAndBody());

      mergesCount++;
      if (mergesCount > 10) break;
    }
    return result;
  }

  @Nullable
  private LinearGraphAnswer expandEdge(GraphEdge edge) {
    if (edge.getType() == GraphEdgeType.DOTTED) {
      return new LinearGraphAnswer(
        GraphChangesUtil.edgesReplaced(Collections.singleton(edge), myCompiledGraph.expandEdge(edge), getDelegateGraph()));
    }
    return null;
  }

  @Nonnull
  private LinearGraph getDelegateGraph() {
    return getDelegateController().getCompiledGraph();
  }

  @Nonnull
  @Override
  public LinearGraph getCompiledGraph() {
    return myCompiledGraph;
  }

  private static class BekGraphLayout implements GraphLayout {
    private final GraphLayout myGraphLayout;
    private final BekIntMap myBekIntMap;

    public BekGraphLayout(GraphLayout graphLayout, BekIntMap bekIntMap) {
      myGraphLayout = graphLayout;
      myBekIntMap = bekIntMap;
    }

    @Override
    public int getLayoutIndex(int nodeIndex) {
      return myGraphLayout.getLayoutIndex(myBekIntMap.getUsualIndex(nodeIndex));
    }

    @Override
    public int getOneOfHeadNodeIndex(int nodeIndex) {
      int usualIndex = myGraphLayout.getOneOfHeadNodeIndex(myBekIntMap.getUsualIndex(nodeIndex));
      return myBekIntMap.getBekIndex(usualIndex);
    }

    @Nonnull
    @Override
    public List<Integer> getHeadNodeIndex() {
      List<Integer> bekIndexes = new ArrayList<>();
      for (int head : myGraphLayout.getHeadNodeIndex()) {
        bekIndexes.add(myBekIntMap.getBekIndex(head));
      }
      return bekIndexes;
    }
  }
}
