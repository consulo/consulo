// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.provided;

import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.language.index.impl.internal.UpdatableIndex;
import consulo.language.index.impl.internal.hash.MergedInvertedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.logging.Logger;

import java.io.File;
import java.io.IOException;

public interface ProvidedIndexExtension<K, V> {
  Logger LOG = Logger.getInstance(ProvidedIndexExtension.class);

  
  File getIndexPath();

  
  ID<K, V> getIndexId();

  
  KeyDescriptor<K> createKeyDescriptor();

  
  DataExternalizer<V> createValueExternalizer();

  
  static <K, V> UpdatableIndex<K, V, FileContent> wrapWithProvidedIndex(ProvidedIndexExtension<K, V> providedIndexExtension,
                                                                        FileBasedIndexExtension<K, V> originalExtension,
                                                                        UpdatableIndex<K, V, FileContent> index) {
    try {
      return MergedInvertedIndex.create(providedIndexExtension, originalExtension, index);
    }
    catch (IOException e) {
      LOG.error(e);
      return index;
    }
  }
}
