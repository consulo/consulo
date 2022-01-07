// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class StubForwardIndexExternalizer<StubKeySerializationState> implements DataExternalizer<Map<StubIndexKey, Map<Object, StubIdList>>> {
  private volatile boolean myEnsuredStubElementTypesLoaded;

  protected abstract StubKeySerializationState createStubIndexKeySerializationState(@Nonnull DataOutput out, @Nonnull Set<StubIndexKey> set) throws IOException;

  protected abstract void writeStubIndexKey(@Nonnull DataOutput out, @Nonnull StubIndexKey key, StubKeySerializationState state) throws IOException;

  protected abstract StubKeySerializationState createStubIndexKeySerializationState(@Nonnull DataInput input, int stubIndexKeyCount) throws IOException;

  protected abstract ID<?, ?> readStubIndexKey(@Nonnull DataInput input, StubKeySerializationState stubKeySerializationState) throws IOException;

  @Override
  public void save(@Nonnull DataOutput out, Map<StubIndexKey, Map<Object, StubIdList>> indexedStubs) throws IOException {

    DataInputOutputUtil.writeINT(out, indexedStubs.size());
    if (!indexedStubs.isEmpty()) {
      StubKeySerializationState stubKeySerializationState = createStubIndexKeySerializationState(out, indexedStubs.keySet());

      StubIndexImpl stubIndex = (StubIndexImpl)StubIndex.getInstance();
      for (StubIndexKey stubIndexKey : indexedStubs.keySet()) {
        writeStubIndexKey(out, stubIndexKey, stubKeySerializationState);
        Map<Object, StubIdList> map = indexedStubs.get(stubIndexKey);
        stubIndex.serializeIndexValue(out, stubIndexKey, map);
      }
    }
  }

  @Override
  public Map<StubIndexKey, Map<Object, StubIdList>> read(@Nonnull DataInput in) throws IOException {
    return doRead(in, null, null);
  }

  @Nullable
  <K> Map<StubIndexKey, Map<Object, StubIdList>> doRead(@Nonnull DataInput in, @Nullable StubIndexKey<K, ?> requestedIndex, @Nullable K requestedKey) throws IOException {
    if (!myEnsuredStubElementTypesLoaded) {
      ProgressManager.getInstance().executeNonCancelableSection(() -> {
        SerializationManager.getInstance().initSerializers();
        StubIndexImpl.initExtensions();
      });
      myEnsuredStubElementTypesLoaded = true;
    }
    int stubIndicesValueMapSize = DataInputOutputUtil.readINT(in);
    if (stubIndicesValueMapSize > 0) {
      Map<StubIndexKey, Map<Object, StubIdList>> stubIndicesValueMap = requestedIndex != null ? null : new HashMap<>(stubIndicesValueMapSize);
      StubIndexImpl stubIndex = (StubIndexImpl)StubIndex.getInstance();
      StubKeySerializationState stubKeySerializationState = createStubIndexKeySerializationState(in, stubIndicesValueMapSize);
      for (int i = 0; i < stubIndicesValueMapSize; ++i) {
        ID<Object, ?> indexKey = (ID<Object, ?>)readStubIndexKey(in, stubKeySerializationState);
        if (indexKey instanceof StubIndexKey) { // indexKey can be ID in case of removed index
          StubIndexKey<Object, ?> stubIndexKey = (StubIndexKey<Object, ?>)indexKey;
          boolean deserialize = requestedIndex == null || requestedIndex.equals(stubIndexKey);
          if (deserialize) {
            Map<Object, StubIdList> value = stubIndex.deserializeIndexValue(in, stubIndexKey, requestedKey);
            if (requestedIndex != null) {
              return Collections.singletonMap(requestedIndex, value);
            }
            stubIndicesValueMap.put(stubIndexKey, value);
          }
          else {
            stubIndex.skipIndexValue(in);
          }
        }
      }
      return stubIndicesValueMap;
    }
    return Collections.emptyMap();
  }

  static class IdeStubForwardIndexesExternalizer extends StubForwardIndexExternalizer<Void> {
    static final IdeStubForwardIndexesExternalizer INSTANCE = new IdeStubForwardIndexesExternalizer();

    @Override
    protected void writeStubIndexKey(@Nonnull DataOutput out, @Nonnull StubIndexKey key, Void aVoid) throws IOException {
      DataInputOutputUtil.writeINT(out, key.getUniqueId());
    }

    @Override
    protected Void createStubIndexKeySerializationState(@Nonnull DataOutput out, @Nonnull Set<StubIndexKey> set) {
      return null;
    }

    @Override
    protected ID<?, ?> readStubIndexKey(@Nonnull DataInput input, Void aVoid) throws IOException {
      return ID.findById(DataInputOutputUtil.readINT(input));
    }

    @Override
    protected Void createStubIndexKeySerializationState(@Nonnull DataInput input, int stubIndexKeyCount) {
      return null;
    }
  }
}
