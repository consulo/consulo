// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.impl;

import com.intellij.util.indexing.StorageException;
import javax.annotation.Nonnull;

import java.io.IOException;

public abstract class AbstractUpdateData<Key, Value> {
  private final int myInputId;

  protected AbstractUpdateData(int id) {
    myInputId = id;
  }

  protected abstract boolean iterateKeys(@Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> addProcessor,
                                         @Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> updateProcessor,
                                         @Nonnull RemovedKeyProcessor<? super Key> removeProcessor) throws StorageException;

  public abstract boolean newDataIsEmpty();

  public int getInputId() {
    return myInputId;
  }

  protected void updateForwardIndex() throws IOException {
  }

}
