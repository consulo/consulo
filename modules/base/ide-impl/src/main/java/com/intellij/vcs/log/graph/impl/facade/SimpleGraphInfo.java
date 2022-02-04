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
package com.intellij.vcs.log.graph.impl.facade;

import com.intellij.util.Function;
import com.intellij.util.NotNullFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.graph.GraphCommit;
import com.intellij.vcs.log.graph.GraphCommitImpl;
import com.intellij.vcs.log.graph.api.GraphLayout;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.LiteLinearGraph;
import com.intellij.vcs.log.graph.api.permanent.PermanentCommitsInfo;
import com.intellij.vcs.log.graph.api.permanent.PermanentGraphInfo;
import com.intellij.vcs.log.graph.impl.permanent.GraphLayoutImpl;
import com.intellij.vcs.log.graph.impl.permanent.PermanentCommitsInfoImpl;
import com.intellij.vcs.log.graph.impl.permanent.PermanentLinearGraphBuilder;
import com.intellij.vcs.log.graph.impl.permanent.PermanentLinearGraphImpl;
import com.intellij.vcs.log.graph.utils.IntList;
import com.intellij.vcs.log.graph.utils.LinearGraphUtils;
import com.intellij.vcs.log.graph.utils.TimestampGetter;
import com.intellij.vcs.log.graph.utils.impl.CompressedIntList;
import com.intellij.vcs.log.graph.utils.impl.IntTimestampGetter;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.intellij.vcs.log.graph.utils.LinearGraphUtils.asLiteLinearGraph;

public class SimpleGraphInfo<CommitId> implements PermanentGraphInfo<CommitId> {

  @Nonnull
  private final LinearGraph myLinearGraph;
  @Nonnull
  private final GraphLayout myGraphLayout;
  @Nonnull
  private final NotNullFunction<Integer, CommitId> myFunction;
  @Nonnull
  private final TimestampGetter myTimestampGetter;
  @Nonnull
  private final Set<Integer> myBranchNodeIds;

  private SimpleGraphInfo(@Nonnull LinearGraph linearGraph,
                          @Nonnull GraphLayout graphLayout,
                          @Nonnull NotNullFunction<Integer, CommitId> function,
                          @Nonnull TimestampGetter timestampGetter,
                          @Nonnull Set<Integer> branchNodeIds) {
    myLinearGraph = linearGraph;
    myGraphLayout = graphLayout;
    myFunction = function;
    myTimestampGetter = timestampGetter;
    myBranchNodeIds = branchNodeIds;
  }

  public static <CommitId> SimpleGraphInfo<CommitId> build(@Nonnull final LinearGraph linearGraph,
                                                           @Nonnull GraphLayout oldLayout,
                                                           @Nonnull final PermanentCommitsInfo<CommitId> permanentCommitsInfo,
                                                           int permanentGraphSize,
                                                           @Nonnull Set<Integer> branchNodeIds) {
    int firstVisibleRow = 1000; // todo get first visible row from table somehow
    int delta = 1000;
    final int start = Math.max(0, firstVisibleRow - delta);
    final int end = Math.min(linearGraph.nodesCount(), start + 2 * delta); // no more than 2*1000 commits;

    final List<GraphCommit<CommitId>> graphCommits = ContainerUtil.newArrayListWithCapacity(end - start);
    final List<CommitId> commitsIdMap = ContainerUtil.newArrayListWithCapacity(end - start);

    for (int row = start; row < end; row++) {
      int nodeId = linearGraph.getNodeId(row);
      CommitId commit = permanentCommitsInfo.getCommitId(nodeId);
      List<CommitId> parents = ContainerUtil.newSmartList();
      parents.addAll(ContainerUtil.mapNotNull(asLiteLinearGraph(linearGraph).getNodes(row, LiteLinearGraph.NodeFilter.DOWN),
                                              new Function<Integer, CommitId>() {
                                                @Override
                                                public CommitId fun(Integer row) {
                                                  if (row < start || row >= end) return null;
                                                  return permanentCommitsInfo.getCommitId(linearGraph.getNodeId(row));
                                                }
                                              }));
      graphCommits.add(new GraphCommitImpl<>(commit, parents, permanentCommitsInfo.getTimestamp(nodeId)));
      commitsIdMap.add(commit);
    }
    IntTimestampGetter timestampGetter = PermanentCommitsInfoImpl.createTimestampGetter(graphCommits);

    NotNullFunction<Integer, CommitId> function = createCommitIdMapFunction(commitsIdMap);
    PermanentLinearGraphImpl newLinearGraph = PermanentLinearGraphBuilder.newInstance(graphCommits).build();

    final int[] layoutIndexes = new int[end - start];
    List<Integer> headNodeIndexes = ContainerUtil.newArrayList();

    ObjectIntMap<CommitId> commitIdToInteger = reverseCommitIdMap(permanentCommitsInfo, permanentGraphSize);
    for (int row = start; row < end; row++) {
      CommitId commitId = commitsIdMap.get(row - start);
      int layoutIndex = oldLayout.getLayoutIndex(commitIdToInteger.getInt(commitId));
      layoutIndexes[row - start] = layoutIndex;
      if (asLiteLinearGraph(newLinearGraph).getNodes(row - start, LiteLinearGraph.NodeFilter.UP).isEmpty()) {
        headNodeIndexes.add(row - start);
      }
    }

    ContainerUtil.sort(headNodeIndexes, new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return layoutIndexes[o1] - layoutIndexes[o2];
      }
    });
    int[] starts = new int[headNodeIndexes.size()];
    for (int i = 0; i < starts.length; i++) {
      starts[i] = layoutIndexes[headNodeIndexes.get(i)];
    }

    GraphLayoutImpl newLayout = new GraphLayoutImpl(layoutIndexes, headNodeIndexes, starts);

    return new SimpleGraphInfo<>(newLinearGraph, newLayout, function, timestampGetter,
                                 LinearGraphUtils.convertIdsToNodeIndexes(linearGraph, branchNodeIds));
  }

  @Nonnull
  private static <CommitId> NotNullFunction<Integer, CommitId> createCommitIdMapFunction(List<CommitId> commitsIdMap) {
    NotNullFunction<Integer, CommitId> function;
    if (!commitsIdMap.isEmpty() && commitsIdMap.get(0) instanceof Integer) {
      int[] ints = new int[commitsIdMap.size()];
      for (int row = 0; row < commitsIdMap.size(); row++) {
        ints[row] = (Integer)commitsIdMap.get(row);
      }
      function = (NotNullFunction<Integer, CommitId>)new IntegerCommitIdMapFunction(CompressedIntList.newInstance(ints));
    }
    else {
      function = new CommitIdMapFunction<>(commitsIdMap);
    }
    return function;
  }

  @Nonnull
  private static <CommitId> ObjectIntMap<CommitId> reverseCommitIdMap(PermanentCommitsInfo<CommitId> permanentCommitsInfo, int size) {
    ObjectIntMap<CommitId> result = ObjectMaps.newObjectIntHashMap();
    for (int i = 0; i < size; i++) {
      result.putInt(permanentCommitsInfo.getCommitId(i), i);
    }
    return result;
  }

  @Nonnull
  @Override
  public PermanentCommitsInfo<CommitId> getPermanentCommitsInfo() {
    return new PermanentCommitsInfo<CommitId>() {
      @Nonnull
      @Override
      public CommitId getCommitId(int nodeId) {
        return myFunction.fun(nodeId);
      }

      @Override
      public long getTimestamp(int nodeId) {
        return myTimestampGetter.getTimestamp(nodeId);
      }

      @Override
      public int getNodeId(@Nonnull CommitId commitId) {
        for (int id = 0; id < myLinearGraph.nodesCount(); id++) {
          if (myFunction.fun(id).equals(commitId)) {
            return id;
          }
        }
        return -1;
      }

      @Nonnull
      @Override
      public Set<Integer> convertToNodeIds(@Nonnull Collection<CommitId> heads) {
        Set<Integer> result = ContainerUtil.newHashSet();
        for (int id = 0; id < myLinearGraph.nodesCount(); id++) {
          if (heads.contains(myFunction.fun(id))) {
            result.add(id);
          }
        }
        return result;
      }
    };
  }

  @Nonnull
  @Override
  public LinearGraph getLinearGraph() {
    return myLinearGraph;
  }

  @Nonnull
  @Override
  public GraphLayout getPermanentGraphLayout() {
    return myGraphLayout;
  }

  @Nonnull
  @Override
  public Set<Integer> getBranchNodeIds() {
    return myBranchNodeIds;
  }

  private static class CommitIdMapFunction<CommitId> implements NotNullFunction<Integer, CommitId> {
    private final List<CommitId> myCommitsIdMap;

    public CommitIdMapFunction(List<CommitId> commitsIdMap) {
      myCommitsIdMap = commitsIdMap;
    }

    @Nonnull
    @Override
    public CommitId fun(Integer dom) {
      return myCommitsIdMap.get(dom);
    }
  }

  private static class IntegerCommitIdMapFunction implements NotNullFunction<Integer, Integer> {
    private final IntList myCommitsIdMap;

    public IntegerCommitIdMapFunction(IntList commitsIdMap) {
      myCommitsIdMap = commitsIdMap;
    }

    @Nonnull
    @Override
    public Integer fun(Integer dom) {
      return myCommitsIdMap.get(dom);
    }
  }
}
