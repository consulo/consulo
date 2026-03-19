/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.application.util.graph;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.component.util.graph.Graph;
import consulo.util.collection.Chunk;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class GraphAlgorithms {
  public static GraphAlgorithms getInstance() {
    return Application.get().getInstance(GraphAlgorithms.class);
  }

  public abstract @Nullable <Node> List<Node> findShortestPath(Graph<Node> graph, Node start, Node finish);

  
  public abstract <Node> List<List<Node>> findKShortestPaths(Graph<Node> graph, Node start, Node finish, int k, ProgressIndicator progressIndicator);

  
  public abstract <Node> Set<List<Node>> findCycles(Graph<Node> graph, Node node);

  
  public abstract <Node> List<List<Node>> removePathsWithCycles(List<List<Node>> paths);

  
  public abstract <Node> Graph<Node> invertEdgeDirections(Graph<Node> graph);

  
  public abstract <Node> Collection<Chunk<Node>> computeStronglyConnectedComponents(Graph<Node> graph);

  
  public abstract <Node> Graph<Chunk<Node>> computeSCCGraph(Graph<Node> graph);

  /**
   * Adds start node and all its outs to given set recursively.
   * Nodes which are already in set aren't processed.
   *
   * @param start node to start from
   * @param set   set to be populated
   */
  public abstract <Node> void collectOutsRecursively(Graph<Node> graph, Node start, Set<Node> set);
}
