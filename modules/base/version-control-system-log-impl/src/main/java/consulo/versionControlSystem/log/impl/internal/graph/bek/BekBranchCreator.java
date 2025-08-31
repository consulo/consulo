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
package consulo.versionControlSystem.log.impl.internal.graph.bek;

import consulo.util.lang.Pair;
import consulo.versionControlSystem.log.graph.LinearGraph;
import consulo.versionControlSystem.log.impl.internal.graph.GraphLayoutImpl;
import consulo.versionControlSystem.log.impl.internal.util.BitSetFlags;
import consulo.versionControlSystem.log.impl.internal.util.DfsUtil;
import consulo.versionControlSystem.log.impl.internal.util.Flags;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import static consulo.versionControlSystem.log.graph.LinearGraphUtils.getDownNodes;
import static consulo.versionControlSystem.log.graph.LinearGraphUtils.getUpNodes;

class BekBranchCreator {
  @Nonnull
  private final LinearGraph myPermanentGraph;
  @Nonnull
  private final GraphLayoutImpl myGraphLayout;
  @Nonnull
  private final Flags myDoneNodes;

  @Nonnull
  private final DfsUtil myDfsUtil = new DfsUtil();
  @Nonnull
  private final BekEdgeRestrictions myEdgeRestrictions = new BekEdgeRestrictions();

  public BekBranchCreator(@Nonnull LinearGraph permanentGraph, @Nonnull GraphLayoutImpl graphLayout) {
    myPermanentGraph = permanentGraph;
    myGraphLayout = graphLayout;
    myDoneNodes = new BitSetFlags(permanentGraph.nodesCount(), false);
  }

  @Nonnull
  public Pair<List<BekBranch>, BekEdgeRestrictions> getResult() {
    List<BekBranch> bekBranches = new ArrayList<>();

    for (int headNode : myGraphLayout.getHeadNodeIndex()) {
      List<Integer> nextBranch = createNextBranch(headNode);
      bekBranches.add(new BekBranch(myPermanentGraph, nextBranch));
    }
    return Pair.create(bekBranches, myEdgeRestrictions);
  }

  public List<Integer> createNextBranch(int headNode) {
    List<Integer> nodeIndexes = new ArrayList<>();

    assert !myDoneNodes.get(headNode);
    myDoneNodes.set(headNode, true);
    nodeIndexes.add(headNode);

    int startLayout = myGraphLayout.getLayoutIndex(headNode);

    myDfsUtil.nodeDfsIterator(headNode, currentNode -> {
      int currentLayout = myGraphLayout.getLayoutIndex(currentNode);
      List<Integer> downNodes = getDownNodes(myPermanentGraph, currentNode);
      for (int i = downNodes.size() - 1; i >= 0; i--) {
        int downNode = downNodes.get(i);

        if (myDoneNodes.get(downNode)) {
          if (myGraphLayout.getLayoutIndex(downNode) < startLayout) myEdgeRestrictions.addRestriction(currentNode, downNode);
        }
        else if (currentLayout <= myGraphLayout.getLayoutIndex(downNode)) {

          // almost ok node, except (may be) up nodes
          boolean hasUndoneUpNodes = false;
          for (int upNode : getUpNodes(myPermanentGraph, downNode)) {
            if (!myDoneNodes.get(upNode) && myGraphLayout.getLayoutIndex(upNode) <= myGraphLayout.getLayoutIndex(downNode)) {
              hasUndoneUpNodes = true;
              break;
            }
          }

          if (!hasUndoneUpNodes) {
            myDoneNodes.set(downNode, true);
            nodeIndexes.add(downNode);
            return downNode;
          }
        }
      }
      return DfsUtil.NextNode.NODE_NOT_FOUND;
    });

    return nodeIndexes;
  }
}
