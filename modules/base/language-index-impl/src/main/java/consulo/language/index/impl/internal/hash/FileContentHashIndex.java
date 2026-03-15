// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.hash;

import consulo.index.io.AbstractUpdateData;
import consulo.index.io.IndexStorage;
import consulo.index.io.StorageException;
import consulo.index.io.ValueContainer;
import consulo.language.index.impl.internal.IndexInfrastructure;
import consulo.language.index.impl.internal.InputMapExternalizer;
import consulo.language.index.impl.internal.VfsAwareMapReduceIndex;
import consulo.language.index.impl.internal.forward.MapForwardIndexAccessor;
import consulo.language.index.impl.internal.forward.PersistentMapBasedForwardIndex;
import consulo.language.psi.stub.FileContent;

import java.io.IOException;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

public class FileContentHashIndex extends VfsAwareMapReduceIndex<Integer, Void, FileContent> {
    FileContentHashIndex(FileContentHashIndexExtension extension, IndexStorage<Integer, Void> storage) throws IOException {
        super(extension, storage, new PersistentMapBasedForwardIndex(IndexInfrastructure.getInputIndexStorageFile(extension.getName())),
            new MapForwardIndexAccessor<>(new InputMapExternalizer<>(extension)), null, null);
    }

    
    @Override
    protected Supplier<Boolean> createIndexUpdateComputation(AbstractUpdateData<Integer, Void> updateData) {
        return new HashIndexUpdateComputable(super.createIndexUpdateComputation(updateData), updateData.newDataIsEmpty());
    }

    public int getHashId(int fileId) throws StorageException {
        Map<Integer, Void> data = getIndexedFileData(fileId);
        if (data.isEmpty()) {
            return 0;
        }
        return data.keySet().iterator().next();
    }

    
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

    final static class HashIndexUpdateComputable implements Supplier<Boolean> {
        
        private final Supplier<Boolean> myUnderlying;
        private final boolean myEmptyInput;

        HashIndexUpdateComputable(Supplier<Boolean> underlying, boolean isEmptyInput) {
            myUnderlying = underlying;
            myEmptyInput = isEmptyInput;
        }

        boolean isEmptyInput() {
            return myEmptyInput;
        }

        @Override
        public Boolean get() {
            return myUnderlying.get();
        }
    }
}
