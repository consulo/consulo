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
package consulo.ide.impl.idea.vcs.log.graph.impl.facade;

import consulo.ide.impl.idea.vcs.log.graph.api.LinearGraph;
import consulo.ide.impl.idea.vcs.log.graph.api.elements.GraphElement;
import consulo.ide.impl.idea.vcs.log.graph.api.permanent.PermanentGraphInfo;
import consulo.ide.impl.idea.vcs.log.graph.collapsing.CollapsedGraph;
import consulo.ide.impl.idea.vcs.log.graph.collapsing.DottedFilterEdgesGenerator;
import consulo.ide.impl.idea.vcs.log.graph.utils.LinearGraphUtils;
import consulo.ide.impl.idea.vcs.log.graph.utils.UnsignedBitSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

public class FilteredController extends CascadeController {
  @Nonnull
  private final CollapsedGraph myCollapsedGraph;

  protected FilteredController(@Nonnull CascadeController delegateLinearGraphController,
                               @Nonnull PermanentGraphInfo permanentGraphInfo,
                               @Nonnull Set<Integer> matchedIds) {
    super(delegateLinearGraphController, permanentGraphInfo);
    UnsignedBitSet initVisibility = new UnsignedBitSet();
    for (Integer matchedId : matchedIds) initVisibility.set(matchedId, true);

    myCollapsedGraph = CollapsedGraph.newInstance(delegateLinearGraphController.getCompiledGraph(), initVisibility);
    DottedFilterEdgesGenerator.update(myCollapsedGraph, 0, myCollapsedGraph.getDelegatedGraph().nodesCount() - 1);
  }

  @Nonnull
  @Override
  public LinearGraphAnswer performLinearGraphAction(@Nonnull LinearGraphAction action) {
    // filter prohibits any actions on delegate graph for now
    LinearGraphAnswer answer = performAction(action);
    if (answer != null) return answer;
    return LinearGraphUtils.DEFAULT_GRAPH_ANSWER;
  }

  @jakarta.annotation.Nullable
  @Override
  protected GraphElement convertToDelegate(@Nonnull GraphElement graphElement) {
    // filter prohibits any actions on delegate graph for now
    return null;
  }

  @Nonnull
  @Override
  protected LinearGraphAnswer delegateGraphChanged(@Nonnull LinearGraphAnswer delegateAnswer) {
    if (delegateAnswer == LinearGraphUtils.DEFAULT_GRAPH_ANSWER) return delegateAnswer;
    throw new UnsupportedOperationException(); // todo fix later
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
}
