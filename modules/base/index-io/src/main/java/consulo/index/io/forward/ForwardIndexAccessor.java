// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.index.io.forward;

import consulo.index.io.InputData;
import consulo.util.io.ByteArraySequence;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;

public interface ForwardIndexAccessor<Key, Value> {
  /**
   * creates a diff builder for given inputId.
   */
  @Nonnull
  InputDataDiffBuilder<Key, Value> getDiffBuilder(int inputId, @Nullable ByteArraySequence sequence) throws IOException;

  /**
   * serialize indexed data to forward index format.
   */
  @Nullable
  ByteArraySequence serializeIndexedData(@Nonnull InputData<Key, Value> data) throws IOException;
}