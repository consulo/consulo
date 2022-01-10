// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.util.io.AbstractStringEnumerator;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.IOUtil;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.UnaryOperator;

class FileLocalStringEnumerator implements AbstractStringEnumerator {
  private final ObjectIntMap<String> myEnumerates;
  private final ArrayList<String> myStrings = new ArrayList<>();

  FileLocalStringEnumerator(boolean forSavingStub) {
    myEnumerates = forSavingStub ? ObjectMaps.newObjectIntHashMap() : null;
  }

  @Override
  public int enumerate(@Nullable String value) {
    if (value == null) return 0;
    assert myEnumerates != null; // enumerate possible only when writing stub
    int i = myEnumerates.getInt(value);
    if (i == 0) {
      myEnumerates.putInt(value, i = myStrings.size() + 1);
      myStrings.add(value);
    }
    return i;
  }

  @Override
  public String valueOf(int idx) {
    if (idx == 0) return null;
    return myStrings.get(idx - 1);
  }

  void write(@Nonnull DataOutput stream) throws IOException {
    assert myEnumerates != null;
    DataInputOutputUtil.writeINT(stream, myStrings.size());
    byte[] buffer = IOUtil.allocReadWriteUTFBuffer();
    for (String s : myStrings) {
      IOUtil.writeUTFFast(buffer, stream, s);
    }
  }

  @Override
  public void markCorrupted() {
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public void force() {
  }

  static void readEnumeratedStrings(@Nonnull FileLocalStringEnumerator enumerator, @Nonnull DataInput stream, @Nonnull UnaryOperator<String> interner) throws IOException {
    final int numberOfStrings = DataInputOutputUtil.readINT(stream);
    byte[] buffer = IOUtil.allocReadWriteUTFBuffer();
    enumerator.myStrings.ensureCapacity(numberOfStrings);

    int i = 0;
    while (i < numberOfStrings) {
      String s = interner.apply(IOUtil.readUTFFast(buffer, stream));
      enumerator.myStrings.add(s);
      ++i;
    }
  }
}
