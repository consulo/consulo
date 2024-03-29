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
package consulo.ide.impl.idea.util.graph.impl;

import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.util.collection.FList;
import consulo.util.collection.MultiMap;
import consulo.component.util.graph.Graph;
import consulo.logging.Logger;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * Algorithm to search k shortest paths between to vertices in unweighted directed graph.
 * Based on article "Finding the k shortest paths" by D. Eppstein, 1997.
 *
 * @author nik
 */
public class KShortestPathsFinder<Node> {
  private static final Logger LOG = Logger.getInstance(KShortestPathsFinder.class);
  private final Graph<Node> myGraph;
  private final Node myStart;
  private final Node myFinish;
  private final ProgressIndicator myProgressIndicator;
  private MultiMap<Node, GraphEdge<Node>> myNonTreeEdges;
  private List<Node> mySortedNodes;
  private Map<Node, Node> myNextNodes;
  private Map<Node, HeapNode<Node>> myOutRoots;
  private Map<Node,Heap<Node>> myHeaps;

  public KShortestPathsFinder(@Nonnull Graph<Node> graph, @Nonnull Node start, @Nonnull Node finish, @Nonnull ProgressIndicator progressIndicator) {
    myGraph = graph;
    myStart = start;
    myFinish = finish;
    myProgressIndicator = progressIndicator;
  }

  private void computeDistancesToTarget() {
    myNonTreeEdges = new MultiMap<Node, GraphEdge<Node>>();
    mySortedNodes = new ArrayList<Node>();
    myNextNodes = new HashMap<Node, Node>();
    ObjectIntMap<Node> distances = ObjectMaps.newObjectIntHashMap();
    Deque<Node> nodes = new ArrayDeque<Node>();
    nodes.addLast(myFinish);
    distances.putInt(myFinish, 0);
    while (!nodes.isEmpty()) {
      myProgressIndicator.checkCanceled();
      Node node = nodes.removeFirst();
      mySortedNodes.add(node);
      int d = distances.getInt(node) + 1;
      Iterator<Node> iterator = myGraph.getIn(node);
      while (iterator.hasNext()) {
        Node prev = iterator.next();
        if (distances.containsKey(prev)) {
          int dPrev = distances.getInt(prev);
          myNonTreeEdges.putValue(prev, new GraphEdge<Node>(prev, node, d - dPrev));
          continue;
        }
        distances.putInt(prev, d);
        myNextNodes.put(prev, node);
        nodes.addLast(prev);
      }
    }
  }

  private void buildOutHeaps() {
    myOutRoots = new HashMap<Node, HeapNode<Node>>();
    for (Node node : mySortedNodes) {
      myProgressIndicator.checkCanceled();
      List<HeapNode<Node>> heapNodes = new ArrayList<HeapNode<Node>>();
      Collection<GraphEdge<Node>> edges = myNonTreeEdges.get(node);
      if (edges.isEmpty()) continue;

      HeapNode<Node> root = null;
      for (GraphEdge<Node> edge : edges) {
        HeapNode<Node> heapNode = new HeapNode<Node>(edge);
        heapNodes.add(heapNode);
        if (root == null || root.myEdge.getDelta() > heapNode.myEdge.getDelta()) {
          root = heapNode;
        }
      }
      LOG.assertTrue(root != null);
      heapNodes.remove(root);
      myOutRoots.put(node, root);
      if (!heapNodes.isEmpty()) {
        for (int j = 1; j < heapNodes.size(); j++) {
          HeapNode<Node> heapNode = heapNodes.get(j);
          HeapNode<Node> parent = heapNodes.get((j+1)/2 - 1);
          heapNode.myParent = parent;
          parent.myChildren[(j+1) % 2] = heapNode;
        }
        for (int j = heapNodes.size() / 2 - 1; j >= 0; j--) {
          heapify(heapNodes.get(j));
        }
        root.myChildren[2] = heapNodes.get(0);
        root.myChildren[2].myParent = root;
      }
    }
  }

  private void buildMainHeaps() {
    myHeaps = new HashMap<Node, Heap<Node>>();
    for (Node node : mySortedNodes) {
      myProgressIndicator.checkCanceled();
      HeapNode<Node> outRoot = myOutRoots.get(node);
      Node next = myNextNodes.get(node);
      if (outRoot == null) {
        if (next != null) {
          myHeaps.put(node, myHeaps.get(next));
        }
        continue;
      }

      final Heap<Node> nextHeap = myHeaps.get(next);
      if (nextHeap == null) {
        myHeaps.put(node, new Heap<Node>(outRoot));
        continue;
      }

      final Heap<Node> tHeap = nextHeap.insert(outRoot);
      myHeaps.put(node, tHeap);
    }
  }

  private void heapify(HeapNode<Node> node) {
    while (true) {
      HeapNode<Node> min = node;
      for (int i = 0; i < 2; i++) {
        HeapNode<Node> child = node.myChildren[i];
        if (child != null && child.myEdge.getDelta() < min.myEdge.getDelta()) {
          min = child;
        }
      }
      if (min != node) {
        GraphEdge<Node> t = min.myEdge;
        min.myEdge = node.myEdge;
        node.myEdge = t;
        node = min;
      }
      else {
        break;
      }
    }
  }

  public List<List<Node>> findShortestPaths(int k) {
    try {
      if (myStart.equals(myFinish)) {
        return Collections.singletonList(Collections.singletonList(myStart));
      }
      computeDistancesToTarget();
      if (!myNextNodes.containsKey(myStart)) {
        return Collections.emptyList();
      }
      buildOutHeaps();
      buildMainHeaps();

      PriorityQueue<Sidetracks<Node>> queue = new PriorityQueue<Sidetracks<Node>>();
      List<FList<HeapNode<Node>>> sidetracks = new ArrayList<FList<HeapNode<Node>>>();
      sidetracks.add(FList.<HeapNode<Node>>emptyList());

      final Heap<Node> heap = myHeaps.get(myStart);
      if (heap != null) {
        queue.add(new Sidetracks<Node>(0, FList.<HeapNode<Node>>emptyList().prepend(heap.getRoot())));
        for (int i = 2; i <= k; i++) {
          if (queue.isEmpty()) break;
          myProgressIndicator.checkCanceled();
          final Sidetracks<Node> current = queue.remove();
          sidetracks.add(current.myEdges);
          final HeapNode<Node> e = current.myEdges.getHead();
          final Heap<Node> next = myHeaps.get(e.myEdge.getFinish());
          if (next != null) {
            final HeapNode<Node> f = next.getRoot();
            queue.add(new Sidetracks<Node>(current.myLength + f.myEdge.getDelta(), current.myEdges.prepend(f)));
          }
          for (HeapNode<Node> child : e.myChildren) {
            if (child != null) {
              queue.add(new Sidetracks<Node>(current.myLength - e.myEdge.getDelta() + child.myEdge.getDelta(),
                                             current.myEdges.getTail().prepend(child)));
            }
          }
        }
      }

      return computePathsBySidetracks(sidetracks);
    }
    catch (ProcessCanceledException e) {
      return Collections.emptyList();
    }
  }

  private List<List<Node>> computePathsBySidetracks(List<FList<HeapNode<Node>>> sidetracks) {
    final List<List<Node>> result = new ArrayList<List<Node>>();
    for (FList<HeapNode<Node>> sidetrack : sidetracks) {
      myProgressIndicator.checkCanceled();
      List<GraphEdge<Node>> edges = new ArrayList<GraphEdge<Node>>();
      while (!sidetrack.isEmpty()) {
        edges.add(sidetrack.getHead().myEdge);
        sidetrack = sidetrack.getTail();
      }
      Node current = myStart;
      final List<Node> path = new ArrayList<Node>();
      path.add(current);
      int i = edges.size() - 1;
      while (!current.equals(myFinish) || i >= 0) {
        if (i >= 0 && edges.get(i).getStart().equals(current)) {
          current = edges.get(i).getFinish();
          i--;
        }
        else {
          current = myNextNodes.get(current);
          LOG.assertTrue(current != null);
        }
        path.add(current);
      }
      result.add(path);
    }

    return result;
  }

  private static class Sidetracks<Node> implements Comparable<Sidetracks> {
    private int myLength;
    private final FList<HeapNode<Node>> myEdges;

    private Sidetracks(int length, FList<HeapNode<Node>> edges) {
      myLength = length;
      myEdges = edges;
    }

    @Override
    public int compareTo(Sidetracks o) {
      return myLength - o.myLength;
    }
  }

  private static class Heap<Node> {
    private final int mySize;
    private HeapNode<Node> myRoot;

    public Heap(HeapNode<Node> root) {
      myRoot = root;
      mySize = 1;
    }

    private Heap(int size, HeapNode<Node> root) {
      mySize = size;
      myRoot = root;
    }

    public HeapNode<Node> getRoot() {
      return myRoot;
    }

    public Heap<Node> insert(HeapNode<Node> node) {
      int pos = mySize + 1;
      int pow = 1;
      while (pos > pow << 2) {
        pow <<= 1;
      }
      HeapNode<Node> place = myRoot;
      while (true) {
        final int ind = (pos & pow) != 0 ? 1 : 0;
        if (pow == 1) {
          HeapNode<Node> placeCopy = place.copy();
          placeCopy.myChildren[ind] = node;
          node.myParent = placeCopy;
          break;
        }
        place = place.myChildren[ind];
        pow >>= 1;
      }
      while (true) {
        final HeapNode<Node> parent = node.myParent;
        if (parent == null || parent.myEdge.getDelta() < node.myEdge.getDelta()) {
          break;
        }
        final HeapNode<Node> parentCopy = parent.copy();
        final GraphEdge<Node> t = parentCopy.myEdge;
        parentCopy.myEdge = node.myEdge;
        node.myEdge = t;
        final HeapNode<Node> t2 = parentCopy.myChildren[2];
        parentCopy.myChildren[2] = node.myChildren[2];
        node.myChildren[2] = t2;
        node = parentCopy;
      }
      HeapNode<Node> newRoot = node;
      while (newRoot.myParent != null) {
        newRoot = newRoot.myParent;
      }
      return new Heap<Node>(mySize + 1, newRoot);
    }
  }

  private static class HeapNode<Node> {
    public HeapNode<Node>[] myChildren;
    public HeapNode<Node> myParent;
    public GraphEdge<Node> myEdge;

    private HeapNode(GraphEdge<Node> edge) {
      myEdge = edge;
      myChildren = new HeapNode[3];
    }

    public HeapNode(HeapNode<Node> node) {
      myEdge = node.myEdge;
      myChildren = node.myChildren.clone();
      myParent = node.myParent;
    }

    public HeapNode<Node> copy() {
      return new HeapNode<Node>(this);
    }
  }
}
