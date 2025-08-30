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
package consulo.versionControlSystem.log.impl.internal.util;

import jakarta.annotation.Nonnull;

public class SmartDeltaCompressor implements IntList {

  @Nonnull
  public static SmartDeltaCompressor newInstance(@Nonnull IntList deltaList) {
    if (deltaList.size() < 0) throw new NegativeArraySizeException("size < 0: " + deltaList.size());

    int bytesAfterCompression = ByteArrayUtils.countBytesAfterCompression(deltaList);
    byte[] deltas = new byte[bytesAfterCompression];

    int size = deltaList.size();
    int groundPointCount = ByteArrayUtils.groundPointCount(size, 64);

    int[] groundIndexes = new int[groundPointCount];
    long[] majorBits = new long[groundPointCount];
    long[] minorBits = new long[groundPointCount];

    int offset = 0;
    for (int main = 0; main < groundPointCount; main++) {
      groundIndexes[main] = offset;

      for (int rem = 0; rem < 64; rem++) {
        int index = main * 64 + rem;
        if (index >= size) break;
        int sizeOf = ByteArrayUtils.sizeOf(deltaList.get(index));
        ByteArrayUtils.writeDelta(offset, deltaList.get(index), sizeOf, deltas);

        long mask = 1L << rem;
        /*
          4 -> 11
          3 -> 10
          2 -> 01
          1 -> 00
         */
        if (sizeOf == 3 || sizeOf == 4) majorBits[main] |= mask;
        if (sizeOf == 2 || sizeOf == 4) minorBits[main] |= mask;

        offset += sizeOf;
      }
    }

    return new SmartDeltaCompressor(size, deltas, majorBits, minorBits, groundIndexes);
  }

  private final int mySize;
  private final byte[] myDeltas;

  private final long[] myMajorBits;
  private final long[] myMinorBits;

  private final int[] myStrongIndexes;

  private SmartDeltaCompressor(int size, byte[] deltas, long[] majorBits, long[] minorBits, int[] strongIndexes) {
    mySize = size;
    myDeltas = deltas;
    myMajorBits = majorBits;
    myMinorBits = minorBits;
    myStrongIndexes = strongIndexes;
  }

  @Override
  public int size() {
    return mySize;
  }

  @Override
  public int get(int index) {
    int main = index >>> 6; // div 64
    int rem = index & 0x3f; // mod 64

    int shift = 63 - rem;
    long major = myMajorBits[main] << shift;
    long minor = myMinorBits[main] << shift;

    int sizeOf = (int)(2 * (major >>> 63) + (minor >>> 63) + 1);

    int endIndex = myStrongIndexes[main] + 2 * Long.bitCount(major) + Long.bitCount(minor) + rem + 1;

    return ByteArrayUtils.readDelta(endIndex - sizeOf, sizeOf, myDeltas);
  }
}
