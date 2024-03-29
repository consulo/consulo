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
package consulo.language.codeStyle.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

class ProbablyIncreasingLowerboundAlgorithm<T extends AbstractBlockWrapper> {
  private List<T> myBlocks;

  private int myLastCalculatedOffset = -1;
  private int myLastCalculatedAnswerIndex = -1;

  public ProbablyIncreasingLowerboundAlgorithm(@Nonnull List<T> blocks) {
    myBlocks = blocks;
  }

  public void reset() {
    myLastCalculatedAnswerIndex = -1;
    myLastCalculatedOffset = -1;
  }

  public List<T> getLeftSubList(@Nullable AbstractBlockWrapper block) {
    return myBlocks.subList(0, getLeftRespNeighborIndex(block) + 1);
  }

  public void setBlocksList(List<T> blocks) {
    myBlocks = blocks;
    reset();
  }

  @Nullable
  public AbstractBlockWrapper getLeftRespNeighbor(@Nonnull AbstractBlockWrapper block) {
    int index = getLeftRespNeighborIndex(block);
    if (index == -1) {
      return null;
    }
    else {
      return myBlocks.get(index);
    }
  }

  private int getLeftRespNeighborIndex(@Nullable AbstractBlockWrapper block) {
    if (block == null) {
      return myBlocks.size() - 1;
    }

    final int offset = block.getStartOffset();
    if (myLastCalculatedOffset != -1) {
      if (offset >= myLastCalculatedOffset) {
        myLastCalculatedAnswerIndex = calcLeftRespNeighborIndexLinear(myLastCalculatedAnswerIndex, offset);
        myLastCalculatedOffset = offset;
        return myLastCalculatedAnswerIndex;
      }

      //Logger.getInstance(ProbablyIncreasingLowerboundAlgorithm.class).error("not very good!");
      myLastCalculatedOffset = -1;
    }
    myLastCalculatedOffset = offset;
    return myLastCalculatedAnswerIndex = calcLeftRespNeighborIndex(offset);
  }

  private int calcLeftRespNeighborIndexLinear(int lastAnswerIndex, int blockOffset) {
    int index = lastAnswerIndex;
    while (index + 1 < myBlocks.size()) {
      if (blockOffset <= myBlocks.get(index + 1).getStartOffset()) {
        break;
      }
      index++;
    }
    return index;
  }

  private int calcLeftRespNeighborIndex(final int blockOffset) {
    int l = -1, r = myBlocks.size();
    while (r - l > 1) {
      int m = (l + r) / 2;
      if (myBlocks.get(m).getStartOffset() >= blockOffset) {
        r = m;
      }
      else {
        l = m;
      }
    }

    return l;
  }

}
