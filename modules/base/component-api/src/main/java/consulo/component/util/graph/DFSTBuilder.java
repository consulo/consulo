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
package consulo.component.util.graph;

import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Stack;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.collection.primitive.ints.IntStack;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;
import consulo.util.lang.Couple;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author dsl, ven
 */
public class DFSTBuilder<Node> {
  private final OutboundSemiGraph<Node> myGraph;
  private final ObjectIntMap<Node> myNodeToNNumber; // node -> node number in topological order [0..size). Independent nodes are in reversed loading order (loading order is the graph.getNodes() order)
  private final Node[] myInvN; // node number in topological order [0..size) -> node
  private Couple<Node> myBackEdge;

  private Comparator<Node> myComparator;
  private final IntList mySCCs = IntLists.newArrayList(); // strongly connected component sizes
  private final ObjectIntMap<Node> myNodeToTNumber = ObjectMaps.newObjectIntHashMap(); // node -> number in scc topological order. Independent scc are in reversed loading order

  private final Node[] myInvT; // number in (enumerate all nodes scc by scc) order -> node
  private final Node[] myAllNodes;

  public DFSTBuilder(@Nonnull Graph<Node> graph) {
    this((OutboundSemiGraph<Node>)graph);
  }

  @SuppressWarnings("unchecked")
  public DFSTBuilder(@Nonnull OutboundSemiGraph<Node> graph) {
    myAllNodes = (Node[])graph.getNodes().toArray();
    myGraph = graph;
    int size = graph.getNodes().size();
    myNodeToNNumber = ObjectMaps.newObjectIntHashMap(size * 2);
    myInvN = (Node[])new Object[size];
    myInvT = (Node[])new Object[size];
    new Tarjan().build();
  }

  /**
   * Tarjan's strongly connected components search algorithm
   * (<a href="https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm">Wikipedia article</a>).<br>
   * This implementation differs from the canonical one above by:<br>
   * <ul>
   * <li>being non-recursive</li>
   * <li>also computing a topological order during the same single pass</li>
   * </ul>
   */
  private class Tarjan {
    private final int[] lowLink = new int[myInvN.length];
    private final int[] index = new int[myInvN.length];

    private final IntStack nodesOnStack = new IntStack();
    private final boolean[] isOnStack = new boolean[index.length];

    private class Frame {
      public Frame(int nodeI) {
        this.nodeI = nodeI;
        Iterator<Node> outNodes = myGraph.getOut(myAllNodes[nodeI]);
        IntList list = IntLists.newArrayList();

        while (outNodes.hasNext()) {
          Node node = outNodes.next();
          list.add(nodeIndex.getInt(node));
        }
        out = list.toArray();
      }

      private final int nodeI;
      private final int[] out;
      private int nextUnexploredIndex;

      @Override
      public String toString() {
        StringBuilder o = new StringBuilder();
        o.append(myAllNodes[nodeI]).append(" -> [");
        for (int id : out) o.append(myAllNodes[id]).append(", ");
        return o.append(']').toString();
      }
    }

    private final Stack<Frame> frames = new Stack<Frame>(); // recursion stack
    private final ObjectIntMap<Node> nodeIndex = ObjectMaps.newObjectIntHashMap();
    private int dfsIndex;
    private int sccsSizeCombined;
    private final IntList topo = IntLists.newArrayList(index.length); // nodes in reverse topological order

    private void build() {
      Arrays.fill(index, -1);
      for (int i = 0; i < myAllNodes.length; i++) {
        Node node = myAllNodes[i];
        nodeIndex.putInt(node, i);
      }
      for (int i = 0; i < index.length; i++) {
        if (index[i] == -1) {
          frames.push(new Frame(i));
          List<List<Node>> sccs = new ArrayList<List<Node>>();

          strongConnect(sccs);

          for (List<Node> scc : sccs) {
            int sccSize = scc.size();

            mySCCs.add(sccSize);
            int sccBase = index.length - sccsSizeCombined - sccSize;

            // root node should be first in scc for some reason
            Node rootNode = myAllNodes[i];
            int rIndex = scc.indexOf(rootNode);
            if (rIndex != -1) {
              ContainerUtil.swapElements(scc, rIndex, 0);
            }

            for (int j = 0; j < scc.size(); j++) {
              Node sccNode = scc.get(j);
              int tIndex = sccBase + j;
              myInvT[tIndex] = sccNode;
              myNodeToTNumber.putInt(sccNode, tIndex);
            }
            sccsSizeCombined += sccSize;
          }
        }
      }

      for (int i = 0; i < topo.size(); i++) {
        int nodeI = topo.get(i);
        Node node = myAllNodes[nodeI];

        myNodeToNNumber.putInt(node, index.length - 1 - i);
        myInvN[index.length - 1 - i] = node;
      }
      IntLists.reverse(mySCCs);  // have to place SCCs in topological order too
    }

    private void strongConnect(@Nonnull List<List<Node>> sccs) {
      int successor = -1;
      nextNode:
      while (!frames.isEmpty()) {
        Frame pair = frames.peek();
        int i = pair.nodeI;

        // we have returned to the node
        if (index[i] == -1) {
          // actually we visit node first time, prepare
          index[i] = dfsIndex;
          lowLink[i] = dfsIndex;
          dfsIndex++;
          nodesOnStack.push(i);
          isOnStack[i] = true;
        }
        if (ArrayUtil.indexOf(pair.out, successor) != -1) {
          lowLink[i] = Math.min(lowLink[i], lowLink[successor]);
        }
        successor = i;

        // if unexplored children left, dfs there
        while (pair.nextUnexploredIndex < pair.out.length) {
          int nextI = pair.out[pair.nextUnexploredIndex++];
          if (index[nextI] == -1) {
            frames.push(new Frame(nextI));
            continue nextNode;
          }
          if (isOnStack[nextI]) {
            lowLink[i] = Math.min(lowLink[i], index[nextI]);

            if (myBackEdge == null) {
              myBackEdge = Couple.of(myAllNodes[nextI], myAllNodes[i]);
            }
          }
        }
        frames.pop();
        topo.add(i);
        // we are really back, pop a scc
        if (lowLink[i] == index[i]) {
          // found yer
          List<Node> scc = new ArrayList<Node>();
          int pushedI;
          do {
            pushedI = nodesOnStack.pop();
            Node pushed = myAllNodes[pushedI];
            isOnStack[pushedI] = false;
            scc.add(pushed);
          }
          while (pushedI != i);
          sccs.add(scc);
        }
      }
    }
  }

  @Nonnull
  public Comparator<Node> comparator() {
    if (myComparator == null) {
      ObjectIntMap<Node> map = isAcyclic() ? myNodeToNNumber : myNodeToTNumber;
      myComparator = (t, t1) -> map.getInt(t) - map.getInt(t1);
    }
    return myComparator;
  }

  public Couple<Node> getCircularDependency() {
    return myBackEdge;
  }

  public boolean isAcyclic() {
    return getCircularDependency() == null;
  }

  @Nonnull
  public Node getNodeByNNumber(int n) {
    return myInvN[n];
  }

  @Nonnull
  public Node getNodeByTNumber(int n) {
    return myInvT[n];
  }

  /**
   * @return the list containing the number of nodes in strongly connected components.
   * Respective nodes could be obtained via {@link #getNodeByTNumber(int)}.
   */
  @Nonnull
  public IntList getSCCs() {
    return mySCCs;
  }

  @Nonnull
  public Collection<Collection<Node>> getComponents() {
    final IntList componentSizes = getSCCs();
    if (componentSizes.isEmpty()) return List.of();

    return new MyCollection<Collection<Node>>(componentSizes.size()) {
      @Nonnull
      @Override
      public Iterator<Collection<Node>> iterator() {
        return new MyIterator<Collection<Node>>(componentSizes.size()) {
          private int offset;

          @Override
          protected Collection<Node> get(int i) {
            final int cSize = componentSizes.get(i);
            final int cOffset = offset;
            if (cSize == 0) return List.of();
            offset += cSize;
            return new MyCollection<Node>(cSize) {
              @Nonnull
              @Override
              public Iterator<Node> iterator() {
                return new MyIterator<Node>(cSize) {
                  @Override
                  public Node get(int i) {
                    return getNodeByTNumber(cOffset + i);
                  }
                };
              }
            };
          }
        };
      }
    };
  }

  private abstract static class MyCollection<T> extends AbstractCollection<T> {
    private final int size;

    protected MyCollection(int size) {
      this.size = size;
    }

    @Override
    public int size() {
      return size;
    }
  }

  private abstract static class MyIterator<T> implements Iterator<T> {
    private final int size;
    private int i;

    protected MyIterator(int size) {
      this.size = size;
    }

    @Override
    public boolean hasNext() {
      return i < size;
    }

    @Override
    public T next() {
      if (i == size) throw new NoSuchElementException();
      return get(i++);
    }

    protected abstract T get(int i);

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  @Nonnull
  public List<Node> getSortedNodes() {
    List<Node> result = new ArrayList<Node>(myGraph.getNodes());
    Collections.sort(result, comparator());
    return result;
  }
}