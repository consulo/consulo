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

package consulo.versionControlSystem.log.impl.internal.graph;

import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.versionControlSystem.log.graph.GraphCommit;
import consulo.versionControlSystem.log.impl.internal.util.BitSetFlags;
import consulo.versionControlSystem.log.impl.internal.util.Flags;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Function;

public class PermanentLinearGraphBuilder<CommitId> {

  @Nonnull
  private final List<? extends GraphCommit<CommitId>> myCommits;
  @Nonnull
  private final Flags mySimpleNodes;

  private final int myNodesCount;

  @Nonnull
  private final int[] myNodeToEdgeIndex;
  @Nonnull
  private final int[] myLongEdges;
  // downCommitId -> List of upNodeIndex
  @Nonnull
  private final Map<CommitId, List<Integer>> upAdjacentNodes = new HashMap<>();

  private PermanentLinearGraphBuilder(@Nonnull List<? extends GraphCommit<CommitId>> commits,
                                      @Nonnull Flags simpleNodes,
                                      int longEdgesCount) {
    myCommits = commits;
    mySimpleNodes = simpleNodes;

    myNodesCount = simpleNodes.size();

    myNodeToEdgeIndex = new int[myNodesCount + 1];
    myLongEdges = new int[2 * longEdgesCount];
  }

  @Nonnull
  public static <CommitId> PermanentLinearGraphBuilder<CommitId> newInstance(@Nonnull List<? extends GraphCommit<CommitId>> graphCommits) {
    graphCommits = DuplicateParentFixer.fixDuplicateParentCommits(graphCommits);
    Flags simpleNodes = new BitSetFlags(graphCommits.size());

    int longEdgesCount = 0;

    for (int nodeIndex = 0; nodeIndex < graphCommits.size(); nodeIndex++) {
      GraphCommit<CommitId> commit = graphCommits.get(nodeIndex);

      CommitId nextCommitHashIndex = nextCommitHashIndex(graphCommits, nodeIndex);

      List parents = commit.getParents();
      if (parents.size() == 1 && parents.get(0).equals(nextCommitHashIndex)) {
        simpleNodes.set(nodeIndex, true);
      }
      else {
        longEdgesCount += parents.size();
      }
    }

    return new PermanentLinearGraphBuilder<>(graphCommits, simpleNodes, longEdgesCount);
  }

  @Nullable
  private static <CommitId> CommitId nextCommitHashIndex(List<? extends GraphCommit<CommitId>> commits, int nodeIndex) {
    if (nodeIndex < commits.size() - 1) return commits.get(nodeIndex + 1).getId();
    return null;
  }

  private void addUnderdoneEdge(int upNodeIndex, CommitId downCommitId) {
    List<Integer> upNodes = upAdjacentNodes.get(downCommitId);
    if (upNodes == null) {
      upNodes = new SmartList<>();
      upAdjacentNodes.put(downCommitId, upNodes);
    }
    upNodes.add(upNodeIndex);
  }

  private void fixUnderdoneEdge(int upNodeIndex, int downNodeIndex, CommitId downCommitId) {
    int end = myNodeToEdgeIndex[upNodeIndex + 1];

    GraphCommit<CommitId> upCommit = myCommits.get(upNodeIndex);
    List<CommitId> parentHashIndices = upCommit.getParents();

    for (int i = 0; i < parentHashIndices.size(); i++) {
      if (parentHashIndices.get(i).equals(downCommitId)) {
        int offset = parentHashIndices.size() - i;
        int edgeIndex = end - offset;

        if (myLongEdges[edgeIndex] == -1) {
          myLongEdges[edgeIndex] = downNodeIndex;
          return;
        }
        else {
          throw new IllegalStateException("Edge was set early!. Up node: " + upNodeIndex + ", down node: " + downNodeIndex);
        }
      }
    }
    throw new IllegalStateException("Not found underdone edges for node: " + upNodeIndex + ". Adjacent down node: " + downNodeIndex);
  }

  private void doStep(int nodeIndex) {
    GraphCommit<CommitId> commit = myCommits.get(nodeIndex);

    List<Integer> upNodes = upAdjacentNodes.remove(commit.getId());
    if (upNodes == null) upNodes = Collections.emptyList();

    int edgeIndex = myNodeToEdgeIndex[nodeIndex];
    for (Integer upNodeIndex : upNodes) {
      fixUnderdoneEdge(upNodeIndex, nodeIndex, commit.getId());
      myLongEdges[edgeIndex] = upNodeIndex;
      edgeIndex++;
    }

    // down nodes
    if (!mySimpleNodes.get(nodeIndex)) {
      for (CommitId downCommitId : commit.getParents()) {
        addUnderdoneEdge(nodeIndex, downCommitId);
        myLongEdges[edgeIndex] = -1;
        edgeIndex++;
      }
    }

    myNodeToEdgeIndex[nodeIndex + 1] = edgeIndex;
  }

  private void fixUnderdoneEdgeForNotLoadCommit(int upNodeIndex, int notLoadId) {
    for (int edgeIndex = myNodeToEdgeIndex[upNodeIndex]; edgeIndex < myNodeToEdgeIndex[upNodeIndex + 1]; edgeIndex++) {
      if (myLongEdges[edgeIndex] == -1) {
        myLongEdges[edgeIndex] = notLoadId;
        return;
      }
    }
    throw new IllegalStateException("Not found underdone edge to not load commit for node: " + upNodeIndex);
  }

  private void fixUnderdoneEdges(@Nonnull Function<CommitId, Integer> notLoadedCommitToId) {
    List<CommitId> commitIds = new ArrayList<CommitId>(upAdjacentNodes.keySet());
    ContainerUtil.sort(commitIds, (o1, o2) -> Collections.min(upAdjacentNodes.get(o1)) - Collections.min(upAdjacentNodes.get(o2)));
    for (CommitId notLoadCommit : commitIds) {
      int notLoadId = notLoadedCommitToId.apply(notLoadCommit);
      for (int upNodeIndex : upAdjacentNodes.get(notLoadCommit)) {
        fixUnderdoneEdgeForNotLoadCommit(upNodeIndex, notLoadId);
      }
    }
  }

  // id's must be less that -2
  @Nonnull
  public PermanentLinearGraphImpl build(@Nonnull Function<CommitId, Integer> notLoadedCommitToId) {
    for (int nodeIndex = 0; nodeIndex < myNodesCount; nodeIndex++) {
      doStep(nodeIndex);
    }

    fixUnderdoneEdges(notLoadedCommitToId);

    return new PermanentLinearGraphImpl(mySimpleNodes, myNodeToEdgeIndex, myLongEdges);
  }

  @Nonnull
  public PermanentLinearGraphImpl build() {
    return build(dom -> Integer.MIN_VALUE);
  }
}
