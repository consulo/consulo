// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.hash;

import consulo.content.scope.SearchScope;
import consulo.index.io.AbstractUpdateData;
import consulo.index.io.IndexExtension;
import consulo.index.io.StorageException;
import consulo.index.io.ValueContainer;
import consulo.language.index.impl.internal.FileBasedIndexImpl;
import consulo.language.index.impl.internal.UpdatableIndex;
import consulo.language.index.impl.internal.provided.ProvidedIndexExtension;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.IdFilter;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MergedInvertedIndex<Key, Value> implements UpdatableIndex<Key, Value, FileContent> {
  @Nonnull
  private final HashBasedMapReduceIndex<Key, Value> myProvidedIndex;
  @Nonnull
  private final FileContentHashIndex myHashIndex;
  @Nonnull
  private final UpdatableIndex<Key, Value, FileContent> myBaseIndex;

  @Nonnull
  public static <Key, Value> MergedInvertedIndex<Key, Value> create(@Nonnull ProvidedIndexExtension<Key, Value> providedExtension,
                                                                    @Nonnull FileBasedIndexExtension<Key, Value> originalExtension,
                                                                    @Nonnull UpdatableIndex<Key, Value, FileContent> baseIndex) throws IOException {
    File file = providedExtension.getIndexPath();
    HashBasedMapReduceIndex<Key, Value> index = HashBasedMapReduceIndex.create(providedExtension, originalExtension);
    return new MergedInvertedIndex<>(index, ((FileBasedIndexImpl)FileBasedIndex.getInstance()).getFileContentHashIndex(file), baseIndex);
  }

  public MergedInvertedIndex(@Nonnull HashBasedMapReduceIndex<Key, Value> index, @Nonnull FileContentHashIndex hashIndex, @Nonnull UpdatableIndex<Key, Value, FileContent> baseIndex) {
    myProvidedIndex = index;
    myHashIndex = hashIndex;
    myBaseIndex = baseIndex;
  }

  @Nonnull
  public ProvidedIndexExtension<Key, Value> getProvidedExtension() {
    return myProvidedIndex.getProvidedExtension();
  }

  @Nonnull
  @Override
  public Supplier<Boolean> update(int inputId, @Nullable FileContent content) {
    if (content != null) {
      //TODO if content == null
        Supplier<Boolean> update = myHashIndex.update(inputId, content);
      if (!((FileContentHashIndex.HashIndexUpdateComputable)update).isEmptyInput()) return update;
    }
    return myBaseIndex.update(inputId, content);
  }


  @Override
  public void updateWithMap(@Nonnull AbstractUpdateData<Key, Value> updateData) throws StorageException {
    int fileId = updateData.getInputId();
    if (myHashIndex.getHashId(fileId) != 0) {
      return;
    }
    myBaseIndex.updateWithMap(updateData);
  }

  @Override
  public void setBufferingEnabled(boolean enabled) {
    myBaseIndex.setBufferingEnabled(enabled);
  }

  @Override
  public void cleanupMemoryStorage() {
    myBaseIndex.cleanupMemoryStorage();
  }

  @TestOnly
  @Override
  public void cleanupForNextTest() {
    myBaseIndex.cleanupForNextTest();
  }

  @Nonnull
  @Override
  public ValueContainer<Value> getData(@Nonnull Key key) throws StorageException {
    return MergedValueContainer.merge(myBaseIndex.getData(key), myProvidedIndex.getData(key));
  }

  @Override
  public boolean processAllKeys(@Nonnull Predicate<? super Key> processor, @Nonnull SearchScope scope, @Nullable IdFilter idFilter) throws StorageException {
    return myBaseIndex.processAllKeys(processor, scope, idFilter) && myProvidedIndex.processAllKeys(processor, scope, idFilter);
  }

  @Nonnull
  @Override
  public Lock getReadLock() {
    return myBaseIndex.getReadLock();
  }

  @Nonnull
  @Override
  public Lock getWriteLock() {
    return myBaseIndex.getWriteLock();
  }

  @Nonnull
  @Override
  public ReadWriteLock getLock() {
    return myBaseIndex.getLock();
  }

  @Nonnull
  @Override
  public Map<Key, Value> getIndexedFileData(int fileId) throws StorageException {
    Map<Key, Value> data = myBaseIndex.getIndexedFileData(fileId);
    if (!data.isEmpty()) return data;
    int hashId = myHashIndex.getHashId(fileId);
    if (hashId == 0) return Collections.emptyMap();
    return myProvidedIndex.getIndexedFileData(hashId);
  }

  @Override
  public void setIndexedStateForFile(int fileId, @Nonnull VirtualFile file) {
    myBaseIndex.setIndexedStateForFile(fileId, file);
  }

  @Override
  public void resetIndexedStateForFile(int fileId) {
    myBaseIndex.resetIndexedStateForFile(fileId);
  }

  @Override
  public boolean isIndexedStateForFile(int fileId, @Nonnull VirtualFile file) {
    return myBaseIndex.isIndexedStateForFile(fileId, file);
  }

  @Override
  public long getModificationStamp() {
    return myBaseIndex.getModificationStamp();
  }

  @Override
  public void removeTransientDataForFile(int inputId) {
    myBaseIndex.removeTransientDataForFile(inputId);
  }

  @Override
  public void removeTransientDataForKeys(int inputId, @Nonnull Collection<? extends Key> keys) {
    myBaseIndex.removeTransientDataForKeys(inputId, keys);
  }

  @Nonnull
  @Override
  public IndexExtension<Key, Value, FileContent> getExtension() {
    return myBaseIndex.getExtension();
  }


  @Override
  public void clear() throws StorageException {
    myBaseIndex.clear();
  }

  @Override
  public void dispose() {
    myBaseIndex.dispose();
  }

  @Override
  public void flush() throws StorageException {
    myBaseIndex.flush();
  }
}
