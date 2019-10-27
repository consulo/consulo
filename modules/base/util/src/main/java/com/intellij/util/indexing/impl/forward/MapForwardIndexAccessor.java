// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.impl.forward;

import com.intellij.util.indexing.impl.InputData;
import com.intellij.util.io.DataExternalizer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;

public class MapForwardIndexAccessor<Key, Value> extends AbstractMapForwardIndexAccessor<Key, Value, Map<Key, Value>> {
  public MapForwardIndexAccessor(@Nonnull DataExternalizer<Map<Key, Value>> externalizer) {
    super(externalizer);
  }

  @Nullable
  @Override
  protected Map<Key, Value> convertToMap(@Nullable Map<Key, Value> inputData) {
    return inputData;
  }

  @Override
  protected int getBufferInitialSize(@Nonnull Map<Key, Value> map) {
    return 4 * map.size();
  }

  @Nullable
  @Override
  public Map<Key, Value> convertToDataType(@Nonnull InputData<Key, Value> data) {
    return data.getKeyValues();
  }
}
