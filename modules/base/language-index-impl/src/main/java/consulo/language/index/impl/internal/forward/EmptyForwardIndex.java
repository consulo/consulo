// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.forward;

import consulo.index.io.forward.ForwardIndex;
import consulo.util.io.ByteArraySequence;

import org.jspecify.annotations.Nullable;

public class EmptyForwardIndex implements ForwardIndex {
  @Nullable
  @Override
  public ByteArraySequence get(Integer key) {
    return null;
  }

  @Override
  public void put(Integer key, @Nullable ByteArraySequence value) {
  }

  @Override
  public void clear() {
  }

  @Override
  public void close() {
  }

  @Override
  public void force() {
  }
}
