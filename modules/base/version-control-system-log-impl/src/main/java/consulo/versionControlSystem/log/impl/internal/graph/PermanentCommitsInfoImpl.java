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

import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.log.graph.GraphCommit;
import consulo.versionControlSystem.log.graph.PermanentCommitsInfo;
import consulo.versionControlSystem.log.graph.TimestampGetter;
import consulo.versionControlSystem.log.impl.internal.util.CompressedIntList;
import consulo.versionControlSystem.log.impl.internal.util.IntList;

import java.util.*;

public class PermanentCommitsInfoImpl<CommitId> implements PermanentCommitsInfo<CommitId> {
  private static final Logger LOG = Logger.getInstance(PermanentCommitsInfoImpl.class);

  
  public static <CommitId> PermanentCommitsInfoImpl<CommitId> newInstance(
    List<? extends GraphCommit<CommitId>> graphCommits,
    Map<Integer, CommitId> notLoadedCommits
  ) {
    TimestampGetter timestampGetter = createTimestampGetter(graphCommits);

    boolean isIntegerCase = !graphCommits.isEmpty() && graphCommits.get(0).getId().getClass() == Integer.class;

    List<CommitId> commitIdIndex;
    if (isIntegerCase) {
      commitIdIndex = (List<CommitId>)createCompressedIntList((List<? extends GraphCommit<Integer>>)graphCommits);
    }
    else {
      commitIdIndex = ContainerUtil.map(graphCommits, GraphCommit::getId);
    }
    return new PermanentCommitsInfoImpl<>(timestampGetter, commitIdIndex, notLoadedCommits);
  }

  
  public static <CommitId> IntTimestampGetter createTimestampGetter(final List<? extends GraphCommit<CommitId>> graphCommits) {
    return IntTimestampGetter.newInstance(new TimestampGetter() {
      @Override
      public int size() {
        return graphCommits.size();
      }

      @Override
      public long getTimestamp(int index) {
        return graphCommits.get(index).getTimestamp();
      }
    });
  }

  
  private static List<Integer> createCompressedIntList(final List<? extends GraphCommit<Integer>> graphCommits) {
    final IntList compressedIntList = CompressedIntList.newInstance(new IntList() {
      @Override
      public int size() {
        return graphCommits.size();
      }

      @Override
      public int get(int index) {
        return graphCommits.get(index).getId();
      }
    }, 30);
    return new AbstractList<>() {
      
      @Override
      public Integer get(int index) {
        return compressedIntList.get(index);
      }

      @Override
      public int size() {
        return compressedIntList.size();
      }
    };
  }

  
  private final TimestampGetter myTimestampGetter;

  
  private final List<CommitId> myCommitIdIndexes;

  
  private final Map<Integer, CommitId> myNotLoadCommits;

  public PermanentCommitsInfoImpl(TimestampGetter timestampGetter,
                                  List<CommitId> commitIdIndex,
                                  Map<Integer, CommitId> notLoadCommits) {
    myTimestampGetter = timestampGetter;
    myCommitIdIndexes = commitIdIndex;
    myNotLoadCommits = notLoadCommits;
  }

  @Override
  
  public CommitId getCommitId(int nodeId) {
    if (nodeId < 0) return myNotLoadCommits.get(nodeId);
    return myCommitIdIndexes.get(nodeId);
  }

  @Override
  public long getTimestamp(int nodeId) {
    if (nodeId < 0) return 0;
    return myTimestampGetter.getTimestamp(nodeId);
  }

  
  public TimestampGetter getTimestampGetter() {
    return myTimestampGetter;
  }

  // todo optimize with special map
  @Override
  public int getNodeId(CommitId commitId) {
    int indexOf = myCommitIdIndexes.indexOf(commitId);
    if (indexOf != -1) return indexOf;

    return getNotLoadNodeId(commitId);
  }

  private int getNotLoadNodeId(CommitId commitId) {
    for (Map.Entry<Integer, CommitId> entry : myNotLoadCommits.entrySet()) {
      if (entry.getValue().equals(commitId)) return entry.getKey();
    }
    return -1;
  }

  
  public List<CommitId> convertToCommitIdList(Collection<Integer> commitIndexes) {
    return ContainerUtil.map(commitIndexes, this::getCommitId);
  }

  
  public Set<CommitId> convertToCommitIdSet(Collection<Integer> commitIndexes) {
    return ContainerUtil.map2Set(commitIndexes, this::getCommitId);
  }

  @Override
  
  public Set<Integer> convertToNodeIds(Collection<CommitId> commitIds) {
    return convertToNodeIds(commitIds, false);
  }                                                                                                   

  
  public Set<Integer> convertToNodeIds(Collection<CommitId> commitIds, boolean reportNotFound) {
    Set<Integer> result = new HashSet<>();
    Set<CommitId> matchedIds = new HashSet<>();
    for (int i = 0; i < myCommitIdIndexes.size(); i++) {
      CommitId commitId = myCommitIdIndexes.get(i);
      if (commitIds.contains(commitId)) {
        result.add(i);
        matchedIds.add(commitId);
      }
    }
    if (reportNotFound) {
      Collection<CommitId> unmatchedIds = ContainerUtil.subtract(commitIds, matchedIds);
      if (!unmatchedIds.isEmpty()) {
        LOG.warn("Unmatched commit ids " + unmatchedIds);
      }
    }
    for (Map.Entry<Integer, CommitId> entry : myNotLoadCommits.entrySet()) {
      if (commitIds.contains(entry.getValue())) result.add(entry.getKey());
    }
    return result;
  }
}
