// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;

import javax.annotation.Nonnull;
import java.util.Arrays;

class ByteArrayInterner {
  private static final HashingStrategy<byte[]> BYTE_ARRAY_STRATEGY = new HashingStrategy<byte[]>() {
    @Override
    public int hashCode(byte[] object) {
      return Arrays.hashCode(object);
    }

    @Override
    public boolean equals(byte[] o1, byte[] o2) {
      return Arrays.equals(o1, o2);
    }
  };
  private final ObjectIntMap<byte[]> arrayToStart = ObjectMaps.newObjectIntHashMap(BYTE_ARRAY_STRATEGY);
  final BufferExposingByteArrayOutputStream joinedBuffer = new BufferExposingByteArrayOutputStream();

  int internBytes(@Nonnull byte[] bytes) {
    if (bytes.length == 0) return 0;

    int start = arrayToStart.getInt(bytes);
    if (start == 0) {
      start = joinedBuffer.size() + 1; // should be positive
      arrayToStart.putInt(bytes, start);
      joinedBuffer.write(bytes, 0, bytes.length);
    }
    return start;
  }
}
