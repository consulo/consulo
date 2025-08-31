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

package consulo.versionControlSystem.log.impl.internal.util;

import jakarta.annotation.Nonnull;

public class IntDeltaCompressor implements IntList {

  @Nonnull
  public static IntDeltaCompressor newInstance(@Nonnull IntList deltaList) {
    if (deltaList.size() < 0) throw new NegativeArraySizeException("size < 0: " + deltaList.size());

    int bytesAfterCompression = ByteArrayUtils.countBytesAfterCompression(deltaList);
    Flags startedDeltaIndex = new BitSetFlags(bytesAfterCompression);
    byte[] compressedDeltas = new byte[bytesAfterCompression];

    int currentStartIndex = 0;
    for (int i = 0; i < deltaList.size(); i++) {
      startedDeltaIndex.set(currentStartIndex, true);

      int value = deltaList.get(i);
      int sizeOf = ByteArrayUtils.sizeOf(value);
      ByteArrayUtils.writeDelta(currentStartIndex, value, sizeOf, compressedDeltas);

      currentStartIndex += sizeOf;
    }

    return new IntDeltaCompressor(compressedDeltas, startedDeltaIndex, deltaList.size());
  }

  @Nonnull
  private final byte[] myCompressedDeltas;
  @Nonnull
  private final Flags myStartedDeltaIndex;

  @Nonnull
  private final IntToIntMap myStartIndexMap;

  private IntDeltaCompressor(@Nonnull byte[] compressedDeltas, @Nonnull Flags startedDeltaIndex, int countDeltas) {
    myCompressedDeltas = compressedDeltas;
    myStartedDeltaIndex = startedDeltaIndex;
    myStartIndexMap = PermanentListIntToIntMap.newInstance(startedDeltaIndex, countDeltas);
  }

  // [left, right)
  public int getSumOfInterval(int left, int right) {
    if (left < 0 || left > right || right > size()) {
      throw new IllegalArgumentException("Size is: " + size() + ", but interval is: (" + left + ", " + right + ")");
    }
    if (left == size()) return 0;

    int startIndex = myStartIndexMap.getLongIndex(left);
    int sum = 0;
    for (int i = 0; i < right - left; i++) {
      int sizeOf = getNextStartIndex(startIndex) - startIndex;
      sum += ByteArrayUtils.readDelta(startIndex, sizeOf, myCompressedDeltas);
      startIndex += sizeOf;
    }
    return sum;
  }

  @Override
  public int get(int index) {
    if (index < 0 || index >= size()) throw new IllegalArgumentException("Size is: " + size() + ", but index is: " + index);

    int startIndex = myStartIndexMap.getLongIndex(index);
    int sizeOf = getNextStartIndex(startIndex) - startIndex;
    return ByteArrayUtils.readDelta(startIndex, sizeOf, myCompressedDeltas);
  }

  @Override
  public int size() {
    return myStartIndexMap.shortSize();
  }

  private int getNextStartIndex(int currentIndex) {
    for (int i = currentIndex + 1; i < myStartedDeltaIndex.size(); i++) {
      if (myStartedDeltaIndex.get(i)) return i;
    }
    return myStartedDeltaIndex.size();
  }
}
