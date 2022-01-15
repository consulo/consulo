// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import consulo.util.dataholder.UserDataHolder;
import com.intellij.util.indexing.impl.InputData;
import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.Map;

//@ApiStatus.Experimental
public interface UpdatableSnapshotInputMappingIndex<Key, Value, Input> extends SnapshotInputMappingIndex<Key, Value, Input> {
  consulo.util.dataholder.Key<Boolean> FORCE_IGNORE_MAPPING_INDEX_UPDATE = consulo.util.dataholder.Key.create("unphysical.content.flag");

  @Nonnull
  Map<Key, Value> readData(int hashId) throws IOException;

  InputData<Key, Value> putData(@Nonnull Input content, @Nonnull InputData<Key, Value> data) throws IOException;

  void flush() throws IOException;

  void clear() throws IOException;

  static <Input> boolean ignoreMappingIndexUpdate(@Nonnull Input content) {
    return content instanceof UserDataHolder && ((UserDataHolder)content).getUserData(FORCE_IGNORE_MAPPING_INDEX_UPDATE) != null;
  }
}
