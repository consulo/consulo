// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.util.io.DataInputOutputUtil;
import consulo.util.collection.primitive.ints.IntIntMap;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.collection.primitive.ints.IntMaps;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.PrimitiveIterator;
import java.util.function.IntUnaryOperator;

class IntEnumerator {
  private final IntIntMap myEnumerates;
  private final IntList myIds;
  private int myNext;

  IntEnumerator() {
    this(true);
  }

  private IntEnumerator(boolean forSavingStub) {
    myEnumerates = forSavingStub ? IntMaps.newIntIntHashMap(1) : null;
    myIds = IntLists.newArrayList();
  }

  int enumerate(int number) {
    assert myEnumerates != null;
    int i = myEnumerates.getInt(number);
    if (i == 0) {
      i = myNext;
      myEnumerates.putInt(number, myNext++);
      myIds.add(number);
    }
    return i;
  }

  int valueOf(int id) {
    return myIds.get(id);
  }

  void dump(DataOutputStream stream) throws IOException {
    dump(stream, IntUnaryOperator.identity());
  }

  void dump(DataOutputStream stream, IntUnaryOperator idRemapping) throws IOException {
    DataInputOutputUtil.writeINT(stream, myIds.size());
    PrimitiveIterator.OfInt iterator = myIds.iterator();
    while (iterator.hasNext()) {
      int id = iterator.nextInt();
      int remapped = idRemapping.applyAsInt(id);
      if (remapped == 0) {
        throw new IOException("remapping is not found for " + id);
      }
      DataInputOutputUtil.writeINT(stream, remapped);
    }
  }

  static IntEnumerator read(DataInputStream stream) throws IOException {
    int size = DataInputOutputUtil.readINT(stream);
    IntEnumerator enumerator = new IntEnumerator(false);
    for (int i = 1; i < size + 1; i++) {
      enumerator.myIds.add(DataInputOutputUtil.readINT(stream));
    }
    return enumerator;
  }
}
