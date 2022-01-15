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
package com.intellij.vcs.log.graph.collapsing;

import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.permanent.PermanentGraphInfo;
import com.intellij.vcs.log.graph.impl.facade.CascadeController;
import com.intellij.vcs.log.graph.impl.facade.ReachableNodes;
import com.intellij.vcs.log.graph.utils.UnsignedBitSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Set;

public class BranchFilterController extends CascadeController {
  @Nonnull
  private CollapsedGraph myCollapsedGraph;
  private final Set<Integer> myIdsOfVisibleBranches;

  public BranchFilterController(@Nonnull CascadeController delegateLinearGraphController,
                                @Nonnull final PermanentGraphInfo<?> permanentGraphInfo,
                                @javax.annotation.Nullable Set<Integer> idsOfVisibleBranches) {
    super(delegateLinearGraphController, permanentGraphInfo);
    myIdsOfVisibleBranches = idsOfVisibleBranches;
    updateCollapsedGraph();
  }

  private void updateCollapsedGraph() {
    UnsignedBitSet initVisibility =
      ReachableNodes.getReachableNodes(myPermanentGraphInfo.getLinearGraph(), myIdsOfVisibleBranches);
    myCollapsedGraph = CollapsedGraph.newInstance(getDelegateController().getCompiledGraph(), initVisibility);
  }

  @Nonnull
  @Override
  protected LinearGraphAnswer delegateGraphChanged(@Nonnull LinearGraphAnswer delegateAnswer) {
    if (delegateAnswer.getGraphChanges() != null) updateCollapsedGraph();
    return delegateAnswer;
  }

  @Nullable
  @Override
  protected LinearGraphAnswer performAction(@Nonnull LinearGraphAction action) {
    return null;
  }

  @Nonnull
  @Override
  public LinearGraph getCompiledGraph() {
    return myCollapsedGraph.getCompiledGraph();
  }

  @Nullable
  @Override
  protected GraphElement convertToDelegate(@Nonnull GraphElement graphElement) {
    return CollapsedController.convertToDelegate(graphElement, myCollapsedGraph);
  }
}
