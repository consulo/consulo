// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.forward;

import consulo.index.io.data.DataExternalizer;
import consulo.index.io.forward.AbstractForwardIndexAccessor;
import consulo.index.io.forward.InputDataDiffBuilder;
import consulo.language.index.impl.internal.MapInputDataDiffBuilder;
import consulo.util.io.ByteArraySequence;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Map;

public abstract class AbstractMapForwardIndexAccessor<Key, Value, DataType> extends AbstractForwardIndexAccessor<Key, Value, DataType> {
  public AbstractMapForwardIndexAccessor(@Nonnull DataExternalizer<DataType> externalizer) {
    super(externalizer);
  }

  @Override
  protected final InputDataDiffBuilder<Key, Value> createDiffBuilder(int inputId, @Nullable DataType inputData) throws IOException {
    return new MapInputDataDiffBuilder<>(inputId, convertToMap(inputData));
  }

  @Nullable
  protected abstract Map<Key, Value> convertToMap(@Nullable DataType inputData) throws IOException;

  @Nullable
  public Map<Key, Value> convertToInputDataMap(@Nullable ByteArraySequence sequence) throws IOException {
    return convertToMap(deserializeData(sequence));
  }
}
