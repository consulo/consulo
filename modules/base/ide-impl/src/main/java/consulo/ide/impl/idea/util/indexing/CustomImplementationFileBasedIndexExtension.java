// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package consulo.ide.impl.idea.util.indexing;

import consulo.index.io.IndexStorage;
import consulo.index.io.StorageException;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;

import jakarta.annotation.Nonnull;

import java.io.IOException;

//@ApiStatus.OverrideOnly
public interface CustomImplementationFileBasedIndexExtension<K, V> {
  @Nonnull
  UpdatableIndex<K, V, FileContent> createIndexImplementation(@Nonnull FileBasedIndexExtension<K, V> extension, @Nonnull IndexStorage<K, V> storage) throws StorageException, IOException;
}