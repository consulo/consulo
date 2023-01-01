// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.stub;

import consulo.index.io.AbstractStringEnumerator;
import consulo.util.collection.ArrayUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.BitSet;

class LazyStubData {
  private final AbstractStringEnumerator myStorage;
  private final MostlyUShortIntList myParentsAndStarts;
  private final byte[] mySerializedStubs;
  private final BitSet myAllStarts;

  LazyStubData(AbstractStringEnumerator storage, MostlyUShortIntList parentsAndStarts, byte[] serializedStubs, BitSet allStarts) {
    myStorage = storage;
    myParentsAndStarts = parentsAndStarts;
    mySerializedStubs = serializedStubs;
    myAllStarts = allStarts;
  }

  int getParentIndex(int index) {
    return myParentsAndStarts.get(index * 2);
  }

  private int getDataStart(int index) {
    return myParentsAndStarts.get(index * 2 + 1);
  }

  StubBase<?> deserializeStub(int index, StubBase<?> parent, IStubElementType<?, ?> type) throws IOException {
    StubInputStream stream = new StubInputStream(stubBytes(index), myStorage);
    StubBase<?> stub = (StubBase<?>)type.deserialize(stream, parent);
    int available = stream.available();
    if (available > 0) {
      assert available == 1 : "Stub serializer/deserializer mismatch in " + type;
      stub.markDangling();
    }
    return stub;
  }

  private ByteArrayInputStream stubBytes(int index) {
    int start = getDataStart(index);
    if (start == 0) return new ByteArrayInputStream(ArrayUtil.EMPTY_BYTE_ARRAY);

    int end = myAllStarts.nextSetBit(start + 1);
    if (end < 0) end = mySerializedStubs.length + 1;
    return new ByteArrayInputStream(mySerializedStubs, start - 1, end - start);
  }
}