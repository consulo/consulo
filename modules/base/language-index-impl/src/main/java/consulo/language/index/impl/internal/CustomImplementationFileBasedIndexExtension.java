// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.index.io.IndexStorage;
import consulo.index.io.StorageException;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;

import java.io.IOException;

/**
 * @author max
 */
public interface CustomImplementationFileBasedIndexExtension<K, V> {
  
  UpdatableIndex<K, V, FileContent> createIndexImplementation(FileBasedIndexExtension<K, V> extension, IndexStorage<K, V> storage) throws StorageException, IOException;
}