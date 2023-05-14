// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing.impl.forward;

import consulo.index.io.KeyValueStore;
import consulo.util.io.ByteArraySequence;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;

/**
 * Represents key-value storage held by <a href="https://en.wikipedia.org/wiki/Search_engine_indexing#The_forward_index">forward index data structure</>.
 */
//@ApiStatus.Experimental
public interface ForwardIndex extends KeyValueStore<Integer, ByteArraySequence> {
  @Nullable
  @Override
  ByteArraySequence get(@Nonnull Integer key) throws IOException;

  @Override
  void put(@Nonnull Integer key, @Nullable ByteArraySequence value) throws IOException;

  void clear() throws IOException;
}