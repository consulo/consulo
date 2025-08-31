/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.util.graph.impl;

import consulo.component.util.graph.Graph;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author anna
 * @since 2005-02-13
 */
public class CycleFinder<Node> {
  private final Graph<Node> myGraph;

  public CycleFinder(Graph<Node> graph) {
    myGraph = graph;
  }

  @Nonnull
  public Set<List<Node>> getNodeCycles(final Node node){
    Set<List<Node>> result = new HashSet<List<Node>>();


    Graph<Node> graphWithoutNode = new Graph<Node>() {
      public Collection<Node> getNodes() {
        Collection<Node> nodes = myGraph.getNodes();
        nodes.remove(node);
        return nodes;
      }

      public Iterator<Node> getIn(Node n) {
        HashSet<Node> nodes = new HashSet<Node>();
        Iterator<Node> in = myGraph.getIn(n);
        while (in.hasNext()) {
          nodes.add(in.next());
        }
        nodes.remove(node);
        return nodes.iterator();
      }

      public Iterator<Node> getOut(Node n) {
        HashSet<Node> nodes = new HashSet<Node>();
        Iterator<Node> out = myGraph.getOut(n);
        while (out.hasNext()) {
          nodes.add(out.next());
        }
        nodes.remove(node);
        return nodes.iterator();
      }

    };

    HashSet<Node> inNodes = new HashSet<Node>();
    Iterator<Node> in = myGraph.getIn(node);
    while (in.hasNext()) {
      inNodes.add(in.next());
    }
    HashSet<Node> outNodes = new HashSet<Node>();
    Iterator<Node> out = myGraph.getOut(node);
    while (out.hasNext()) {
      outNodes.add(out.next());
    }

    HashSet<Node> retainNodes = new HashSet<Node>(inNodes);
    retainNodes.retainAll(outNodes);
    for (Node node1 : retainNodes) {
      ArrayList<Node> oneNodeCycle = new ArrayList<Node>();
      oneNodeCycle.add(node1);
      oneNodeCycle.add(node);
      result.add(oneNodeCycle);
    }

    inNodes.removeAll(retainNodes);
    outNodes.removeAll(retainNodes);

    ShortestPathFinder<Node> finder = new ShortestPathFinder<Node>(graphWithoutNode);
    for (Node fromNode : outNodes) {
      for (Node toNode : inNodes) {
        List<Node> shortestPath = finder.findPath(fromNode, toNode);
        if (shortestPath != null) {
          ArrayList<Node> path = new ArrayList<Node>();
          path.addAll(shortestPath);
          path.add(node);
          result.add(path);
        }
      }
    }
    return result;
  }
}
