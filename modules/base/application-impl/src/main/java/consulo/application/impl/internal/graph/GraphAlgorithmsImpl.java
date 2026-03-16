/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.application.impl.internal.graph;

import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.component.util.graph.*;
import consulo.application.util.graph.GraphAlgorithms;
import consulo.util.collection.Chunk;
import jakarta.inject.Singleton;

import java.util.*;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class GraphAlgorithmsImpl extends GraphAlgorithms {
  @Override
  public <Node> List<Node> findShortestPath(Graph<Node> graph, Node start, Node finish) {
    return new ShortestPathFinder<>(graph).findPath(start, finish);
  }

  
  @Override
  public <Node> List<List<Node>> findKShortestPaths(Graph<Node> graph, Node start, Node finish, int k,
                                                    ProgressIndicator progressIndicator) {
    return new KShortestPathsFinder<>(graph, start, finish, progressIndicator).findShortestPaths(k);
  }

  
  @Override
  public <Node> Set<List<Node>> findCycles(Graph<Node> graph, Node node) {
    return new CycleFinder<>(graph).getNodeCycles(node);
  }

  
  @Override
  public <Node> Graph<Node> invertEdgeDirections(final Graph<Node> graph) {
    return new Graph<Node>() {
      public Collection<Node> getNodes() {
        return graph.getNodes();
      }

      public Iterator<Node> getIn(Node n) {
        return graph.getOut(n);
      }

      public Iterator<Node> getOut(Node n) {
        return graph.getIn(n);
      }

    };
  }

  
  @Override
  public <Node> Graph<Chunk<Node>> computeSCCGraph(final Graph<Node> graph) {
    DFSTBuilder<Node> builder = new DFSTBuilder<>(graph);

    Collection<Collection<Node>> components = builder.getComponents();
    final List<Chunk<Node>> chunks = new ArrayList<>(components.size());
    final Map<Node, Chunk<Node>> nodeToChunkMap = new LinkedHashMap<>();
    for (Collection<Node> component : components) {
      Set<Node> chunkNodes = new LinkedHashSet<>();
      Chunk<Node> chunk = new Chunk<>(chunkNodes);
      chunks.add(chunk);
      for (Node node : component) {
        chunkNodes.add(node);
        nodeToChunkMap.put(node, chunk);
      }
    }

    return GraphGenerator.generate(CachingSemiGraph.cache(new InboundSemiGraph<Chunk<Node>>() {
      @Override
      public Collection<Chunk<Node>> getNodes() {
        return chunks;
      }

      @Override
      public Iterator<Chunk<Node>> getIn(Chunk<Node> chunk) {
        Set<Node> chunkNodes = chunk.getNodes();
        Set<Chunk<Node>> ins = new LinkedHashSet<>();
        for (Node node : chunkNodes) {
          for (Iterator<Node> nodeIns = graph.getIn(node); nodeIns.hasNext(); ) {
            Node in = nodeIns.next();
            if (!chunk.containsNode(in)) {
              ins.add(nodeToChunkMap.get(in));
            }
          }
        }
        return ins.iterator();
      }
    }));
  }

  @Override
  public <Node> void collectOutsRecursively(Graph<Node> graph, Node start, Set<Node> set) {
    if (!set.add(start)) {
      return;
    }
    Iterator<Node> iterator = graph.getOut(start);
    while (iterator.hasNext()) {
      Node node = iterator.next();
      collectOutsRecursively(graph, node, set);
    }
  }

  
  @Override
  public <Node> Collection<Chunk<Node>> computeStronglyConnectedComponents(Graph<Node> graph) {
    return computeSCCGraph(graph).getNodes();
  }

  
  @Override
  public <Node> List<List<Node>> removePathsWithCycles(List<List<Node>> paths) {
    List<List<Node>> result = new ArrayList<>();
    for (List<Node> path : paths) {
      if (!containsCycle(path)) {
        result.add(path);
      }
    }
    return result;
  }

  private static boolean containsCycle(List<?> path) {
    return new HashSet<Object>(path).size() != path.size();
  }
}