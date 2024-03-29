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
package consulo.ide.impl.idea.vcs.log.graph.api;

import consulo.ide.impl.idea.vcs.log.graph.api.elements.GraphEdge;
import consulo.ide.impl.idea.vcs.log.graph.api.elements.GraphNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public interface LinearGraph {
  int nodesCount();

  @Nonnull
  List<GraphEdge> getAdjacentEdges(int nodeIndex, @Nonnull EdgeFilter filter);

  @Nonnull
  GraphNode getGraphNode(int nodeIndex);

  int getNodeId(int nodeIndex);

  // return null, if node doesn't exist
  @Nullable
  Integer getNodeIndex(int nodeId);
}
