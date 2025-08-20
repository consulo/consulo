// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.forward;

import consulo.index.io.IndexExtension;
import consulo.index.io.IndexId;
import consulo.index.io.InputData;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.forward.AbstractForwardIndexAccessor;
import consulo.index.io.forward.InputDataDiffBuilder;
import consulo.language.index.impl.internal.CollectionInputDataDiffBuilder;
import consulo.language.index.impl.internal.InputIndexDataExternalizer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Set;

public class KeyCollectionForwardIndexAccessor<Key, Value> extends AbstractForwardIndexAccessor<Key, Value, Collection<Key>> {
  public KeyCollectionForwardIndexAccessor(@Nonnull DataExternalizer<Collection<Key>> externalizer) {
    super(externalizer);
  }

  public KeyCollectionForwardIndexAccessor(@Nonnull IndexExtension<Key, Value, ?> extension) {
    this(extension.getKeyDescriptor(), extension.getName());
  }

  public KeyCollectionForwardIndexAccessor(@Nonnull KeyDescriptor<Key> externalizer, @Nonnull IndexId<Key, Value> indexId) {
    super(new InputIndexDataExternalizer<>(externalizer, indexId));
  }

  @Override
  protected InputDataDiffBuilder<Key, Value> createDiffBuilder(int inputId, @Nullable Collection<Key> keys) {
    return new CollectionInputDataDiffBuilder<>(inputId, keys);
  }

  @Nullable
  @Override
  public Collection<Key> convertToDataType(@Nonnull InputData<Key, Value> data) {
    Set<Key> keys = data.getKeyValues().keySet();
    return keys.isEmpty() ? null : keys;
  }

  @Override
  protected int getBufferInitialSize(@Nonnull Collection<Key> keys) {
    return 4 * keys.size();
  }
}
