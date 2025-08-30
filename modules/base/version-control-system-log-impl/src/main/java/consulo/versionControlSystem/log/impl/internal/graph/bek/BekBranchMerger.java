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

import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.log.graph.TimestampGetter;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

class BekBranchMerger {
  @Nonnull
  private final List<BekBranch> myBekBranches;
  @Nonnull
  private final BekEdgeRestrictions myEdgeRestrictions;
  @Nonnull
  private final TimestampGetter myTimestampGetter;

  @Nonnull
  private final List<Integer> myInverseResultList = new ArrayList<>();

  public BekBranchMerger(
    @Nonnull List<BekBranch> bekBranches,
    @Nonnull BekEdgeRestrictions edgeRestrictions,
    @Nonnull TimestampGetter timestampGetter
  ) {
    myBekBranches = bekBranches;
    myEdgeRestrictions = edgeRestrictions;
    myTimestampGetter = timestampGetter;
  }

  // return true, if exist some undone branch
  private boolean prepareLastPartsForBranches() {
    boolean hasUndoneBranches = false;
    for (BekBranch bekBranch : myBekBranches) {
      if (!bekBranch.isDone()) {
        hasUndoneBranches = true;
        if (bekBranch.getPrepareForInsertPart() == null) {
          bekBranch.updatePrepareForInsertPart(myTimestampGetter, myEdgeRestrictions);
        }
      }
    }
    return hasUndoneBranches;
  }

  private long getBranchLastPartTimestamp(BekBranch bekBranch) {
    List<Integer> prepareForInsertPart = bekBranch.getPrepareForInsertPart();
    if (prepareForInsertPart == null) return Long.MAX_VALUE;

    assert !prepareForInsertPart.isEmpty();
    int nodeIndex = prepareForInsertPart.get(0);
    return myTimestampGetter.getTimestamp(nodeIndex);
  }

  private void step() {
    BekBranch selectBranch = myBekBranches.get(0);
    for (BekBranch bekBranch : myBekBranches) {
      if (getBranchLastPartTimestamp(selectBranch) > getBranchLastPartTimestamp(bekBranch)) {
        selectBranch = bekBranch;
      }
    }

    List<Integer> prepareForInsertPart = selectBranch.getPrepareForInsertPart();
    assert prepareForInsertPart != null;
    for (int insertedNode : prepareForInsertPart) {
      myEdgeRestrictions.removeRestriction(insertedNode);
    }

    myInverseResultList.addAll(ContainerUtil.reverse(prepareForInsertPart));
    selectBranch.doneInsertPreparedPart();
  }

  @Nonnull
  public List<Integer> getResult() {
    while (prepareLastPartsForBranches()) {
      step();
    }

    return ContainerUtil.reverse(myInverseResultList);
  }
}
