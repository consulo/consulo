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
package com.intellij.vcs.log.graph.impl.facade;

import com.intellij.vcs.log.graph.*;
import com.intellij.vcs.log.graph.actions.ActionController;
import com.intellij.vcs.log.graph.actions.GraphAction;
import com.intellij.vcs.log.graph.actions.GraphAnswer;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.elements.GraphNodeType;
import com.intellij.vcs.log.graph.api.permanent.PermanentGraphInfo;
import com.intellij.vcs.log.graph.impl.facade.LinearGraphController.LinearGraphAction;
import com.intellij.vcs.log.graph.impl.print.PrintElementGeneratorImpl;
import com.intellij.vcs.log.graph.impl.print.elements.PrintElementWithGraphElement;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;

import static com.intellij.vcs.log.graph.utils.LinearGraphUtils.getCursor;

public class VisibleGraphImpl<CommitId> implements VisibleGraph<CommitId> {
  @Nonnull
  private final LinearGraphController myGraphController;
  @Nonnull
  private final PermanentGraphInfo<CommitId> myPermanentGraph;
  @Nonnull
  private final GraphColorManager<CommitId> myColorManager;

  private PrintElementManagerImpl myPrintElementManager;
  private PrintElementGeneratorImpl myPrintElementGenerator;
  private boolean myShowLongEdges = false;

  public VisibleGraphImpl(@Nonnull LinearGraphController graphController,
                          @Nonnull PermanentGraphInfo<CommitId> permanentGraph,
                          @Nonnull GraphColorManager<CommitId> colorManager) {
    myGraphController = graphController;
    myPermanentGraph = permanentGraph;
    myColorManager = colorManager;
    updatePrintElementGenerator();
  }

  @Override
  public int getVisibleCommitCount() {
    return myGraphController.getCompiledGraph().nodesCount();
  }

  @Nonnull
  @Override
  public RowInfo<CommitId> getRowInfo(final int visibleRow) {
    final int nodeId = myGraphController.getCompiledGraph().getNodeId(visibleRow);
    assert nodeId >= 0; // todo remake for all id
    return new MyRowInfo(nodeId, visibleRow);
  }

  @Override
  @javax.annotation.Nullable
  public Integer getVisibleRowIndex(@Nonnull CommitId commitId) {
    int nodeId = myPermanentGraph.getPermanentCommitsInfo().getNodeId(commitId);
    return myGraphController.getCompiledGraph().getNodeIndex(nodeId);
  }

  @Nonnull
  @Override
  public ActionController<CommitId> getActionController() {
    return new ActionControllerImpl();
  }

  private void updatePrintElementGenerator() {
    myPrintElementManager = new PrintElementManagerImpl(myGraphController.getCompiledGraph(), myPermanentGraph, myColorManager);
    myPrintElementGenerator = new PrintElementGeneratorImpl(myGraphController.getCompiledGraph(), myPrintElementManager, myShowLongEdges);
  }

  @Nonnull
  public SimpleGraphInfo<CommitId> buildSimpleGraphInfo() {
    return SimpleGraphInfo
      .build(myGraphController.getCompiledGraph(), myPermanentGraph.getPermanentGraphLayout(), myPermanentGraph.getPermanentCommitsInfo(),
             myPermanentGraph.getLinearGraph().nodesCount(),
             myPermanentGraph.getBranchNodeIds());
  }

  public int getRecommendedWidth() {
    return myPrintElementGenerator.getRecommendedWidth();
  }

  private class ActionControllerImpl implements ActionController<CommitId> {

    @javax.annotation.Nullable
    private Integer convertToNodeId(@Nullable Integer nodeIndex) {
      if (nodeIndex == null) return null;
      return myGraphController.getCompiledGraph().getNodeId(nodeIndex);
    }

    @Nullable
    private GraphAnswer<CommitId> performArrowAction(@Nonnull LinearGraphAction action) {
      PrintElementWithGraphElement affectedElement = action.getAffectedElement();
      if (!(affectedElement instanceof EdgePrintElement)) return null;
      EdgePrintElement edgePrintElement = (EdgePrintElement)affectedElement;
      if (!edgePrintElement.hasArrow()) return null;

      GraphElement graphElement = affectedElement.getGraphElement();
      if (!(graphElement instanceof GraphEdge)) return null;
      GraphEdge edge = (GraphEdge)graphElement;

      Integer targetId = null;
      if (edge.getType() == GraphEdgeType.NOT_LOAD_COMMIT) {
        assert edgePrintElement.getType().equals(EdgePrintElement.Type.DOWN);
        targetId = edge.getTargetId();
      }
      if (edge.getType().isNormalEdge()) {
        if (edgePrintElement.getType().equals(EdgePrintElement.Type.DOWN)) {
          targetId = convertToNodeId(edge.getDownNodeIndex());
        }
        else {
          targetId = convertToNodeId(edge.getUpNodeIndex());
        }
      }
      if (targetId == null) return null;

      if (action.getType() == GraphAction.Type.MOUSE_OVER) {
        myPrintElementManager.setSelectedElement(affectedElement);
        return new GraphAnswerImpl<>(getCursor(true), myPermanentGraph.getPermanentCommitsInfo().getCommitId(targetId), null,
                                     false);
      }

      if (action.getType() == GraphAction.Type.MOUSE_CLICK) {
        return new GraphAnswerImpl<>(getCursor(false), myPermanentGraph.getPermanentCommitsInfo().getCommitId(targetId), null,
                                     true);
      }

      return null;
    }

    @Nonnull
    @Override
    public GraphAnswer<CommitId> performAction(@Nonnull GraphAction graphAction) {
      myPrintElementManager.setSelectedElements(Collections.<Integer>emptySet());

      LinearGraphAction action = convert(graphAction);
      GraphAnswer<CommitId> graphAnswer = performArrowAction(action);
      if (graphAnswer != null) return graphAnswer;

      LinearGraphController.LinearGraphAnswer answer = myGraphController.performLinearGraphAction(action);
      if (answer.getSelectedNodeIds() != null) myPrintElementManager.setSelectedElements(answer.getSelectedNodeIds());

      if (answer.getGraphChanges() != null) updatePrintElementGenerator();
      return convert(answer);
    }

    @Override
    public boolean areLongEdgesHidden() {
      return !myShowLongEdges;
    }

    @Override
    public void setLongEdgesHidden(boolean longEdgesHidden) {
      myShowLongEdges = !longEdgesHidden;
      updatePrintElementGenerator();
    }

    private LinearGraphAction convert(@Nonnull GraphAction graphAction) {
      PrintElementWithGraphElement printElement = null;
      if (graphAction.getAffectedElement() != null) {
        printElement = myPrintElementGenerator.withGraphElement(graphAction.getAffectedElement());
      }
      return new LinearGraphActionImpl(printElement, graphAction.getType());
    }

    private GraphAnswer<CommitId> convert(@Nonnull final LinearGraphController.LinearGraphAnswer answer) {
      final Runnable graphUpdater = answer.getGraphUpdater();
      return new GraphAnswerImpl<>(answer.getCursorToSet(), null, graphUpdater == null ? null : new Runnable() {
        @Override
        public void run() {
          graphUpdater.run();
          updatePrintElementGenerator();
        }
      }, false);
    }
  }

  private static class GraphAnswerImpl<CommitId> implements GraphAnswer<CommitId> {
    @javax.annotation.Nullable
    private final Cursor myCursor;
    @Nullable private final CommitId myCommitToJump;
    @javax.annotation.Nullable
    private final Runnable myUpdater;
    private final boolean myDoJump;

    private GraphAnswerImpl(@Nullable Cursor cursor, @javax.annotation.Nullable CommitId commitToJump, @javax.annotation.Nullable Runnable updater, boolean doJump) {
      myCursor = cursor;
      myCommitToJump = commitToJump;
      myUpdater = updater;
      myDoJump = doJump;
    }

    @javax.annotation.Nullable
    @Override
    public Cursor getCursorToSet() {
      return myCursor;
    }

    @Nullable
    @Override
    public CommitId getCommitToJump() {
      return myCommitToJump;
    }

    @Nullable
    @Override
    public Runnable getGraphUpdater() {
      return myUpdater;
    }

    @Override
    public boolean doJump() {
      return myDoJump;
    }
  }

  public static class LinearGraphActionImpl implements LinearGraphAction {
    @javax.annotation.Nullable
    private final PrintElementWithGraphElement myAffectedElement;
    @Nonnull
    private final Type myType;

    public LinearGraphActionImpl(@Nullable PrintElementWithGraphElement affectedElement, @Nonnull Type type) {
      myAffectedElement = affectedElement;
      myType = type;
    }

    @Nullable
    @Override
    public PrintElementWithGraphElement getAffectedElement() {
      return myAffectedElement;
    }

    @Nonnull
    @Override
    public Type getType() {
      return myType;
    }
  }

  private class MyRowInfo implements RowInfo<CommitId> {
    private final int myNodeId;
    private final int myVisibleRow;

    public MyRowInfo(int nodeId, int visibleRow) {
      myNodeId = nodeId;
      myVisibleRow = visibleRow;
    }

    @Nonnull
    @Override
    public CommitId getCommit() {
      return myPermanentGraph.getPermanentCommitsInfo().getCommitId(myNodeId);
    }

    @Nonnull
    @Override
    public CommitId getOneOfHeads() {
      int headNodeId = myPermanentGraph.getPermanentGraphLayout().getOneOfHeadNodeIndex(myNodeId);
      return myPermanentGraph.getPermanentCommitsInfo().getCommitId(headNodeId);
    }

    @Nonnull
    @Override
    public Collection<? extends PrintElement> getPrintElements() {
      return myPrintElementGenerator.getPrintElements(myVisibleRow);
    }

    @Nonnull
    @Override
    public RowType getRowType() {
      GraphNodeType nodeType = myGraphController.getCompiledGraph().getGraphNode(myVisibleRow).getType();
      switch (nodeType) {
        case USUAL:
          return RowType.NORMAL;
        case UNMATCHED:
          return RowType.UNMATCHED;
        default:
          throw new UnsupportedOperationException("Unsupported node type: " + nodeType);
      }
    }
  }
}
