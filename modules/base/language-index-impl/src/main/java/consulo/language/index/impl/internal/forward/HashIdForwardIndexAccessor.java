// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.forward;

import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.index.io.forward.InputDataDiffBuilder;
import consulo.index.io.forward.IntForwardIndexAccessor;
import consulo.language.index.impl.internal.HashedInputData;
import consulo.index.io.InputData;
import consulo.language.index.impl.internal.MapInputDataDiffBuilder;
import consulo.language.index.impl.internal.UpdatableSnapshotInputMappingIndex;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Map;

public class HashIdForwardIndexAccessor<Key, Value, Input> extends AbstractMapForwardIndexAccessor<Key, Value, Integer> implements IntForwardIndexAccessor<Key, Value> {
    private final UpdatableSnapshotInputMappingIndex<Key, Value, Input> mySnapshotInputMappingIndex;

    public HashIdForwardIndexAccessor(@Nonnull UpdatableSnapshotInputMappingIndex<Key, Value, Input> snapshotInputMappingIndex) {
        super(EnumeratorIntegerDescriptor.INSTANCE);
        mySnapshotInputMappingIndex = snapshotInputMappingIndex;
    }

    @Nullable
    @Override
    protected Map<Key, Value> convertToMap(@Nullable Integer hashId) throws IOException {
        return hashId == null ? null : mySnapshotInputMappingIndex.readData(hashId);
    }

    @Nonnull
    @Override
    public InputDataDiffBuilder<Key, Value> getDiffBuilderFromInt(int inputId, int hashId) throws IOException {
        return new MapInputDataDiffBuilder<>(inputId, convertToMap(hashId));
    }

    @Override
    public int serializeIndexedDataToInt(@Nonnull InputData<Key, Value> data) {
        return data == InputData.empty() ? 0 : ((HashedInputData<Key, Value>) data).getHashId();
    }

    @Nullable
    @Override
    public Integer convertToDataType(@Nonnull InputData<Key, Value> data) {
        return serializeIndexedDataToInt(data);
    }
}
