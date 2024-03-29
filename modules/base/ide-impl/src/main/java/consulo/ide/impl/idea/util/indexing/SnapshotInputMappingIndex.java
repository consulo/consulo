// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing;

import consulo.ide.impl.idea.util.indexing.impl.InputData;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;

//@ApiStatus.Experimental
public interface SnapshotInputMappingIndex<Key, Value, Input> extends Closeable {
  @Nullable
  InputData<Key, Value> readData(@Nonnull Input content) throws IOException;
}
