// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing.impl.forward;

import consulo.ide.impl.idea.util.indexing.impl.InputData;
import consulo.ide.impl.idea.util.indexing.impl.InputDataDiffBuilder;
import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.util.io.ByteArraySequence;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;

public interface IntForwardIndexAccessor<Key, Value> extends ForwardIndexAccessor<Key, Value> {
  @Nonnull
  @Override
  default InputDataDiffBuilder<Key, Value> getDiffBuilder(int inputId, @Nullable ByteArraySequence sequence) throws IOException {
    return getDiffBuilderFromInt(inputId, sequence == null ? 0 : AbstractForwardIndexAccessor.deserializeFromByteSeq(sequence, EnumeratorIntegerDescriptor.INSTANCE));
  }

  @Nullable
  @Override
  default ByteArraySequence serializeIndexedData(@Nonnull InputData<Key, Value> data) throws IOException {
    return AbstractForwardIndexAccessor.serializeToByteSeq(serializeIndexedDataToInt(data), EnumeratorIntegerDescriptor.INSTANCE, 8);
  }

  /**
   * creates a diff builder for given inputId.
   */
  @Nonnull
  InputDataDiffBuilder<Key, Value> getDiffBuilderFromInt(int inputId, int value) throws IOException;

  int serializeIndexedDataToInt(@Nonnull InputData<Key, Value> data);
}
