// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.index.io.forward;

import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.util.io.ByteArraySequence;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public interface IntForwardIndex extends ForwardIndex {

  int getInt(Integer key) throws IOException;

  void putInt(Integer key, int value) throws IOException;

  @Nullable
  @Override
  default ByteArraySequence get(Integer key) throws IOException {
    int intValue = getInt(key);
    return AbstractForwardIndexAccessor.serializeToByteSeq(intValue, EnumeratorIntegerDescriptor.INSTANCE, 4);
  }

  @Override
  default void put(Integer key, @Nullable ByteArraySequence value) throws IOException {
    int valueAsInt = value == null ? 0 : AbstractForwardIndexAccessor.deserializeFromByteSeq(value, EnumeratorIntegerDescriptor.INSTANCE);
    putInt(key, valueAsInt);
  }
}
