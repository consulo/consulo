// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.hash;

import consulo.application.progress.ProgressManager;
import consulo.index.io.IndexStorage;
import consulo.index.io.MapIndexStorage;
import consulo.language.index.impl.internal.FileBasedIndexImpl;
import consulo.language.index.impl.internal.VfsAwareMapReduceIndex;
import consulo.language.index.impl.internal.provided.ProvidedIndexExtension;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;

import java.io.File;
import java.io.IOException;
import java.util.function.IntUnaryOperator;

class HashBasedMapReduceIndex<Key, Value> extends VfsAwareMapReduceIndex<Key, Value, FileContent> {
  
  private final ProvidedIndexExtension<Key, Value> myProvidedExtension;

  
  static <Key, Value> HashBasedMapReduceIndex<Key, Value> create(ProvidedIndexExtension<Key, Value> providedExtension, FileBasedIndexExtension<Key, Value> originalExtension)
          throws IOException {
    File file = providedExtension.getIndexPath();
    return new HashBasedMapReduceIndex<>(file, originalExtension, providedExtension, ((FileBasedIndexImpl)FileBasedIndex.getInstance()).getFileContentHashIndex(file));
  }

  private HashBasedMapReduceIndex(File baseFile,
                                  FileBasedIndexExtension<Key, Value> originalExtension,
                                  ProvidedIndexExtension<Key, Value> providedExtension,
                                  FileContentHashIndex hashIndex) throws IOException {
    super(originalExtension, createStorage(baseFile, originalExtension, providedExtension, hashIndex.toHashIdToFileIdFunction()), null, null, null, null);
    myProvidedExtension = providedExtension;
  }

  
  public ProvidedIndexExtension<Key, Value> getProvidedExtension() {
    return myProvidedExtension;
  }

  private static <Key, Value> IndexStorage<Key, Value> createStorage(File baseFile,
                                                                     FileBasedIndexExtension<Key, Value> originalExtension,
                                                                     ProvidedIndexExtension<Key, Value> providedExtension,
                                                                     IntUnaryOperator hashToFileId) throws IOException {
    return new MapIndexStorage<Key, Value>(new File(baseFile, originalExtension.getName().getName()), providedExtension.createKeyDescriptor(), providedExtension.createValueExternalizer(),
                                           originalExtension.getCacheSize(), originalExtension.keyIsUniqueForIndexedFile(), true, true, hashToFileId) {
      @Override
      protected void checkCanceled() {
        ProgressManager.checkCanceled();
      }
    };
  }

}
