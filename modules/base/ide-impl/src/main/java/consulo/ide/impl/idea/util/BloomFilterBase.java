// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util;

import consulo.ide.impl.idea.openapi.util.io.DataInputOutputUtilRt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class BloomFilterBase {
  private final int myHashFunctionCount;
  private final int myBitsCount;
  private final long[] myElementsSet;
  private static final int BITS_PER_ELEMENT = 6;

  protected BloomFilterBase(int _maxElementCount, double probability) {
    int bitsPerElementFactor = (int)Math.ceil(-Math.log(probability) / (Math.log(2) * Math.log(2)));
    myHashFunctionCount = (int)Math.ceil(bitsPerElementFactor * Math.log(2));

    int bitsCount = _maxElementCount * bitsPerElementFactor;

    if ((bitsCount & 1) == 0) ++bitsCount;
    while (!isPrime(bitsCount)) bitsCount += 2;
    myBitsCount = bitsCount;
    myElementsSet = new long[(bitsCount >> BITS_PER_ELEMENT) + 1];
  }

  private static boolean isPrime(int bits) {
    if ((bits & 1) == 0 || bits % 3 == 0) return false;
    int sqrt = (int)Math.sqrt(bits);
    for (int i = 6; i <= sqrt; i += 6) {
      if (bits % (i - 1) == 0 || bits % (i + 1) == 0) return false;
    }
    return true;
  }

  protected final void addIt(int prime, int prime2) {
    for (int i = 0; i < myHashFunctionCount; ++i) {
      int abs = Math.abs((i * prime + prime2 * (myHashFunctionCount - i)) % myBitsCount);
      myElementsSet[abs >> BITS_PER_ELEMENT] |= (1L << abs);
    }
  }

  protected final boolean maybeContains(int prime, int prime2) {
    for (int i = 0; i < myHashFunctionCount; ++i) {
      int abs = Math.abs((i * prime + prime2 * (myHashFunctionCount - i)) % myBitsCount);
      if ((myElementsSet[abs >> BITS_PER_ELEMENT] & (1L << abs)) == 0) return false;
    }

    return true;
  }

  protected BloomFilterBase(DataInput input) throws IOException {
    myHashFunctionCount = DataInputOutputUtilRt.readINT(input);
    myBitsCount = DataInputOutputUtilRt.readINT(input);
    myElementsSet = new long[(myBitsCount >> BITS_PER_ELEMENT) + 1];

    for (int i = 0; i < myElementsSet.length; ++i) myElementsSet[i] = input.readLong();
  }

  protected void save(DataOutput output) throws IOException {
    DataInputOutputUtilRt.writeINT(output, myHashFunctionCount);
    DataInputOutputUtilRt.writeINT(output, myBitsCount);
    for (long l : myElementsSet) output.writeLong(l);
  }
}
