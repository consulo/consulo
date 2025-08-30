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

import consulo.versionControlSystem.log.graph.GraphElement;
import consulo.versionControlSystem.log.graph.LinearGraph;
import consulo.versionControlSystem.log.graph.PermanentGraphInfo;
import consulo.versionControlSystem.log.impl.internal.util.UnsignedBitSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

public class BranchFilterController extends CascadeController {
  @Nonnull
  private CollapsedGraph myCollapsedGraph;
  private final Set<Integer> myIdsOfVisibleBranches;

  public BranchFilterController(@Nonnull CascadeController delegateLinearGraphController,
                                @Nonnull PermanentGraphInfo<?> permanentGraphInfo,
                                @Nullable Set<Integer> idsOfVisibleBranches) {
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
