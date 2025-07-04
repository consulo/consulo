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
package consulo.util.collection;

/**
 * Packs the specified number of bits (1..64) into a chunk which stored in array and allows to get and set these bits atomically.
 * Useful for storing related flags together.
 * Guarantees are similar to {@link ConcurrentBitSet}, only for bit chunk instead of bit.
 */
public class ConcurrentPackedBitsArray {
  private final int bitsPerChunk;
  private final ConcurrentBitSet bits = new ConcurrentBitSet();
  private final long mask;
  private final int chunksPerWord;

  public ConcurrentPackedBitsArray(int bitsPerChunk) {
    if (bitsPerChunk <= 0 || bitsPerChunk > 64) {
      throw new IllegalArgumentException("Bits-to-pack number must be between 1 and 64, but got: "+bitsPerChunk);
    }
    this.bitsPerChunk = bitsPerChunk;
    mask = (1 << bitsPerChunk) - 1;
    chunksPerWord = 64 / bitsPerChunk;
  }

  // returns chunk bits
  public long get(int id) {
    int bitIndex = id/chunksPerWord * 64 + (id%chunksPerWord)*bitsPerChunk;
    long word = bits.getWord(bitIndex) >> bitIndex;
    return word & mask;
  }

  // stores chunk atomically, returns previous chunk
  public long set(int id, final long flags) {
    if ((flags & ~mask) != 0) {
      throw new IllegalArgumentException("Flags must be between 0 and "+ mask +" but got:"+flags);
    }
    final int bitIndex = id/chunksPerWord * 64 + (id%chunksPerWord)*bitsPerChunk;

    long prevChunk = bits.changeWord(bitIndex, word -> word & ~(mask << bitIndex) | (flags << bitIndex)) >> bitIndex;

    return prevChunk & mask;
  }

  public void clear() {
    bits.clear();
  }
}
