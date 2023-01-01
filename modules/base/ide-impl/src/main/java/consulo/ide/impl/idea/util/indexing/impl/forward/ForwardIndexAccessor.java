// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing.impl.forward;

import consulo.ide.impl.idea.util.indexing.impl.InputData;
import consulo.ide.impl.idea.util.indexing.impl.InputDataDiffBuilder;
import consulo.util.io.ByteArraySequence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

//@ApiStatus.Experimental
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