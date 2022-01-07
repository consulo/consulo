// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import consulo.logging.Logger;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.hash.MergedInvertedIndex;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;

public interface ProvidedIndexExtension<K, V> {
  Logger LOG = Logger.getInstance(ProvidedIndexExtension.class);

  @Nonnull
  File getIndexPath();

  @Nonnull
  ID<K, V> getIndexId();

  @Nonnull
  KeyDescriptor<K> createKeyDescriptor();

  @Nonnull
  DataExternalizer<V> createValueExternalizer();

  @Nonnull
  static <K, V> UpdatableIndex<K, V, FileContent> wrapWithProvidedIndex(@Nonnull ProvidedIndexExtension<K, V> providedIndexExtension,
                                                                        @Nonnull FileBasedIndexExtension<K, V> originalExtension,
                                                                        @Nonnull UpdatableIndex<K, V, FileContent> index) {
    try {
      return MergedInvertedIndex.create(providedIndexExtension, originalExtension, index);
    }
    catch (IOException e) {
      LOG.error(e);
      return index;
    }
  }
}
