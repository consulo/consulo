// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.util.indexing;

import com.intellij.util.indexing.impl.IndexStorage;
import javax.annotation.Nonnull;

import java.io.IOException;

//@ApiStatus.OverrideOnly
public interface CustomImplementationFileBasedIndexExtension<K, V> {
  @Nonnull
  UpdatableIndex<K, V, FileContent> createIndexImplementation(@Nonnull FileBasedIndexExtension<K, V> extension, @Nonnull IndexStorage<K, V> storage) throws StorageException, IOException;
}