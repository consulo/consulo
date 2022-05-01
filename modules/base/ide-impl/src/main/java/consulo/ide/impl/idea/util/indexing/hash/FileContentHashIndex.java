// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing.hash;

import consulo.application.util.function.Computable;
import consulo.ide.impl.idea.util.indexing.*;
import consulo.ide.impl.idea.util.indexing.impl.AbstractUpdateData;
import consulo.ide.impl.idea.util.indexing.impl.IndexStorage;
import consulo.ide.impl.idea.util.indexing.impl.forward.MapForwardIndexAccessor;
import consulo.ide.impl.idea.util.indexing.impl.forward.PersistentMapBasedForwardIndex;
import consulo.language.psi.stub.FileContent;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.function.IntUnaryOperator;

public class FileContentHashIndex extends VfsAwareMapReduceIndex<Integer, Void, FileContent> {
  FileContentHashIndex(@Nonnull FileContentHashIndexExtension extension, IndexStorage<Integer, Void> storage) throws IOException {
    super(extension, storage, new PersistentMapBasedForwardIndex(IndexInfrastructure.getInputIndexStorageFile(extension.getName())),
          new MapForwardIndexAccessor<>(new InputMapExternalizer<>(extension)), null, null);
  }

  @Nonnull
  @Override
  protected Computable<Boolean> createIndexUpdateComputation(@Nonnull AbstractUpdateData<Integer, Void> updateData) {
    return new HashIndexUpdateComputable(super.createIndexUpdateComputation(updateData), updateData.newDataIsEmpty());
  }

  public int getHashId(int fileId) throws StorageException {
    Map<Integer, Void> data = getIndexedFileData(fileId);
    if (data.isEmpty()) return 0;
    return data.keySet().iterator().next();
  }

  @Nonnull
  IntUnaryOperator toHashIdToFileIdFunction() {
    return hash -> {
      try {
        ValueContainer<Void> data = getData(hash);
        assert data.size() == 1;
        return data.getValueIterator().getInputIdsIterator().next();
      }
      catch (StorageException e) {
        throw new RuntimeException(e);
      }
    };
  }

  final static class HashIndexUpdateComputable implements Computable<Boolean> {
    @Nonnull
    private final Computable<Boolean> myUnderlying;
    private final boolean myEmptyInput;

    HashIndexUpdateComputable(@Nonnull Computable<Boolean> underlying, boolean isEmptyInput) {
      myUnderlying = underlying;
      myEmptyInput = isEmptyInput;
    }

    boolean isEmptyInput() {
      return myEmptyInput;
    }

    @Override
    public Boolean compute() {
      return myUnderlying.compute();
    }
  }
}
