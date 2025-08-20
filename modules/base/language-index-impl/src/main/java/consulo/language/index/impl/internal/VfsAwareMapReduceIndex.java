// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.index.impl.internal;

import consulo.application.ApplicationProperties;
import consulo.application.progress.ProgressManager;
import consulo.content.scope.SearchScope;
import consulo.index.io.*;
import consulo.index.io.forward.ForwardIndex;
import consulo.index.io.forward.ForwardIndexAccessor;
import consulo.index.io.forward.InputDataDiffBuilder;
import consulo.index.io.internal.DebugAssertions;
import consulo.language.index.impl.internal.forward.*;
import consulo.language.internal.psi.stub.IdIndex;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.IdFilter;
import consulo.language.psi.stub.SingleEntryFileBasedIndexExtension;
import consulo.logging.Logger;
import consulo.util.collection.Maps;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.util.io.ByteArraySequence;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;

/**
 * @author Eugene Zhuravlev
 */
public class VfsAwareMapReduceIndex<Key, Value, Input> extends MapReduceIndex<Key, Value, Input> implements UpdatableIndex<Key, Value, Input> {
  private static final Logger LOG = Logger.getInstance(VfsAwareMapReduceIndex.class);

  static {
    if (!DebugAssertions.DEBUG) {
      DebugAssertions.DEBUG = ApplicationProperties.isInSandbox();
    }
  }

  private final AtomicBoolean myInMemoryMode = new AtomicBoolean();
  private final IntObjectMap<Map<Key, Value>> myInMemoryKeysAndValues = IntMaps.newIntObjectHashMap();

  private final SnapshotInputMappingIndex<Key, Value, Input> mySnapshotInputMappings;
  private final boolean myUpdateMappings;
  private final boolean mySingleEntryIndex;

  public VfsAwareMapReduceIndex(@Nonnull IndexExtension<Key, Value, Input> extension,
                                @Nonnull IndexStorage<Key, Value> storage) throws IOException {
    this(extension, storage, hasSnapshotMapping(extension) ? new SnapshotInputMappings<>(extension) : null);
    if (!(myIndexId instanceof ID<?, ?>)) {
      throw new IllegalArgumentException("myIndexId should be instance of consulo.ide.impl.idea.util.indexing.ID");
    }
  }

  public VfsAwareMapReduceIndex(@Nonnull IndexExtension<Key, Value, Input> extension,
                                @Nonnull IndexStorage<Key, Value> storage,
                                @Nullable SnapshotInputMappings<Key, Value, Input> snapshotInputMappings) throws IOException {
    this(extension,
         storage,
         snapshotInputMappings != null ? new SharedIntMapForwardIndex(extension,
                                                                      snapshotInputMappings.getInputIndexStorageFile(),
                                                                      true) : getForwardIndexMap(extension),
         snapshotInputMappings != null ? snapshotInputMappings.getForwardIndexAccessor() : getForwardIndexAccessor(extension),
         snapshotInputMappings,
         null);
  }

  public VfsAwareMapReduceIndex(@Nonnull IndexExtension<Key, Value, Input> extension,
                                @Nonnull IndexStorage<Key, Value> storage,
                                @Nullable ForwardIndex forwardIndexMap,
                                @Nullable ForwardIndexAccessor<Key, Value> forwardIndexAccessor,
                                @Nullable SnapshotInputMappingIndex<Key, Value, Input> snapshotInputMappings,
                                @Nullable ReadWriteLock lock) {
    super(extension, storage, forwardIndexMap, forwardIndexAccessor, lock);
    if (myIndexId instanceof ID) {
      SharedIndicesData.registerIndex((ID<Key, Value>)myIndexId, extension);
    }
    mySnapshotInputMappings = IndexImporterMappingIndex.wrap(snapshotInputMappings, extension);
    myUpdateMappings = snapshotInputMappings instanceof UpdatableSnapshotInputMappingIndex;
    mySingleEntryIndex = extension instanceof SingleEntryFileBasedIndexExtension;
    installMemoryModeListener();
  }

  private static <Key, Value> boolean hasSnapshotMapping(@Nonnull IndexExtension<Key, Value, ?> indexExtension) {
    return indexExtension instanceof FileBasedIndexExtension && ((FileBasedIndexExtension<Key, Value>)indexExtension).hasSnapshotMapping() && IdIndex.ourSnapshotMappingsEnabled;
  }

  @Nonnull
  @Override
  protected InputData<Key, Value> mapInput(@Nullable Input content) {
    InputData<Key, Value> data;
    boolean containsSnapshotData = true;
    if (mySnapshotInputMappings != null && content != null) {
      try {
        data = mySnapshotInputMappings.readData(content);
        if (data != null) {
          return data;
        }
        else {
          containsSnapshotData = !myUpdateMappings;
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    data = super.mapInput(content);
    if (!containsSnapshotData && !UpdatableSnapshotInputMappingIndex.ignoreMappingIndexUpdate(content)) {
      try {
        return ((UpdatableSnapshotInputMappingIndex<Key, Value, Input>)mySnapshotInputMappings).putData(content, data);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return data;
  }

  @Nonnull
  @Override
  protected InputDataDiffBuilder<Key, Value> getKeysDiffBuilder(int inputId) throws IOException {
    if (mySnapshotInputMappings != null && !myInMemoryMode.get()) {
      return super.getKeysDiffBuilder(inputId);
    }
    if (myInMemoryMode.get()) {
      synchronized (myInMemoryKeysAndValues) {
        Map<Key, Value> keysAndValues = myInMemoryKeysAndValues.get(inputId);
        if (keysAndValues != null) {
          return getKeysDiffBuilderInMemoryMode(inputId, keysAndValues);
        }
      }
    }
    return super.getKeysDiffBuilder(inputId);
  }

  @Nonnull
  protected InputDataDiffBuilder<Key, Value> getKeysDiffBuilderInMemoryMode(int inputId, @Nonnull Map<Key, Value> keysAndValues) {
    return mySingleEntryIndex ? new SingleEntryIndexForwardIndexAccessor.SingleValueDiffBuilder(inputId,
                                                                                                keysAndValues) : new MapInputDataDiffBuilder<>(
      inputId,
      keysAndValues);
  }

  @Override
  protected void updateForwardIndex(int inputId, @Nonnull InputData<Key, Value> data) throws IOException {
    if (myInMemoryMode.get()) {
      synchronized (myInMemoryKeysAndValues) {
        myInMemoryKeysAndValues.put(inputId, data.getKeyValues());
      }
    }
    else {
      super.updateForwardIndex(inputId, data);
    }
  }

  @Override
  public void setIndexedStateForFile(int fileId, @Nonnull VirtualFile file) {
    IndexingStamp.setFileIndexedStateCurrent(fileId, (ID<?, ?>)myIndexId);
  }

  @Override
  public void resetIndexedStateForFile(int fileId) {
    IndexingStamp.setFileIndexedStateOutdated(fileId, (ID<?, ?>)myIndexId);
  }

  @Override
  public boolean isIndexedStateForFile(int fileId, @Nonnull VirtualFile file) {
    return IndexingStamp.isFileIndexedStateCurrent(fileId, (ID<?, ?>)myIndexId);
  }

  @Override
  public void removeTransientDataForFile(int inputId) {
    Lock lock = getWriteLock();
    lock.lock();
    try {
      Map<Key, Value> keyValueMap;
      synchronized (myInMemoryKeysAndValues) {
        keyValueMap = myInMemoryKeysAndValues.remove(inputId);
      }

      if (keyValueMap == null) return;

      try {
        removeTransientDataForInMemoryKeys(inputId, keyValueMap);

        InputDataDiffBuilder<Key, Value> builder = getKeysDiffBuilder(inputId);
        if (builder instanceof CollectionInputDataDiffBuilder<?, ?>) {
          Collection<Key> keyCollectionFromDisk = ((CollectionInputDataDiffBuilder<Key, Value>)builder).getSeq();
          if (keyCollectionFromDisk != null) {
            removeTransientDataForKeys(inputId, keyCollectionFromDisk);
          }
        }
        else {
          Set<Key> diskKeySet = new HashSet<>();

          builder.differentiate(Collections.emptyMap(), (key, value, inputId1) -> {
          }, (key, value, inputId1) -> {
          }, (key, inputId1) -> diskKeySet.add(key));
          removeTransientDataForKeys(inputId, diskKeySet);
        }
      }
      catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
    finally {
      lock.unlock();
    }
  }

  protected void removeTransientDataForInMemoryKeys(int inputId, @Nonnull Map<? extends Key, ? extends Value> map) {
    removeTransientDataForKeys(inputId, map.keySet());
  }

  @Override
  public void removeTransientDataForKeys(int inputId, @Nonnull Collection<? extends Key> keys) {
    MemoryIndexStorage<Key, Value> memoryIndexStorage = (MemoryIndexStorage<Key, Value>)getStorage();
    boolean modified = false;
    for (Key key : keys) {
      if (memoryIndexStorage.clearMemoryMapForId(key, inputId) && !modified) {
        modified = true;
      }
    }
    if (modified) {
      myModificationStamp.incrementAndGet();
    }
  }


  @Override
  public void setBufferingEnabled(boolean enabled) {
    ((MemoryIndexStorage<Key, Value>)getStorage()).setBufferingEnabled(enabled);
  }

  @Override
  public void cleanupMemoryStorage() {
    MemoryIndexStorage<Key, Value> memStorage = (MemoryIndexStorage<Key, Value>)getStorage();
    ConcurrencyUtil.withLock(getWriteLock(), () -> memStorage.clearMemoryMap());
    memStorage.fireMemoryStorageCleared();
  }

  @TestOnly
  @Override
  public void cleanupForNextTest() {
    MemoryIndexStorage<Key, Value> memStorage = (MemoryIndexStorage<Key, Value>)getStorage();
    ConcurrencyUtil.withLock(getReadLock(), () -> memStorage.clearCaches());
  }

  @Override
  public boolean processAllKeys(@Nonnull Predicate<? super Key> processor,
                                @Nonnull SearchScope scope,
                                @Nullable IdFilter idFilter) throws StorageException {
    final Lock lock = getReadLock();
    lock.lock();
    try {
      return ((VfsAwareIndexStorage<Key, Value>)myStorage).processKeys(processor, scope, idFilter);
    }
    finally {
      lock.unlock();
    }
  }

  @Nonnull
  @Override
  public Map<Key, Value> getIndexedFileData(int fileId) throws StorageException {
    try {
      return Collections.unmodifiableMap(Maps.notNullize(getNullableIndexedData(fileId)));
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Nullable
  private Map<Key, Value> getNullableIndexedData(int fileId) throws IOException, StorageException {
    if (myInMemoryMode.get()) {
      Map<Key, Value> map = myInMemoryKeysAndValues.get(fileId);
      if (map != null) return map;
    }
    if (getForwardIndexAccessor() instanceof AbstractMapForwardIndexAccessor) {
      ByteArraySequence serializedInputData = getForwardIndex().get(fileId);
      AbstractMapForwardIndexAccessor<Key, Value, ?> forwardIndexAccessor =
        (AbstractMapForwardIndexAccessor<Key, Value, ?>)getForwardIndexAccessor();
      return forwardIndexAccessor.convertToInputDataMap(serializedInputData);
    }
    // in future we will get rid of forward index for SingleEntryFileBasedIndexExtension
    if (myExtension instanceof SingleEntryFileBasedIndexExtension) {
      Key key = (Key)(Object)fileId;
      final Map<Key, Value>[] result = new Map[]{Collections.emptyMap()};
      ValueContainer<Value> container = getData(key);
      container.forEach((id, value) -> {
        result[0] = Collections.singletonMap(key, value);
        return false;
      });
      return result[0];
    }
    LOG.error("Can't fetch indexed data for index " + myIndexId.getName());
    return null;
  }

  @Override
  public void checkCanceled() {
    ProgressManager.checkCanceled();
  }

  @Override
  protected void requestRebuild(@Nonnull Throwable ex) {
    FileBasedIndex.getInstance().requestRebuild((ID<?, ?>)myIndexId, ex);
  }

  @Override
  protected void doClear() throws StorageException, IOException {
    super.doClear();
    if (mySnapshotInputMappings != null && myUpdateMappings) {
      try {
        ((UpdatableSnapshotInputMappingIndex<Key, Value, Input>)mySnapshotInputMappings).clear();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  @Override
  protected void doFlush() throws IOException, StorageException {
    super.doFlush();
    if (mySnapshotInputMappings != null && myUpdateMappings) {
      ((UpdatableSnapshotInputMappingIndex<Key, Value, Input>)mySnapshotInputMappings).flush();
    }
  }

  @Override
  protected void doDispose() throws StorageException {
    super.doDispose();

    if (mySnapshotInputMappings != null) {
      try {
        mySnapshotInputMappings.close();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  @Nullable
  private static <Key, Value> ForwardIndexAccessor<Key, Value> getForwardIndexAccessor(@Nonnull IndexExtension<Key, Value, ?> indexExtension) {
    if (!shouldCreateForwardIndex(indexExtension)) return null;
    if (indexExtension instanceof SingleEntryFileBasedIndexExtension) return new SingleEntryIndexForwardIndexAccessor(indexExtension);
    return new MapForwardIndexAccessor<>(new InputMapExternalizer<>(indexExtension));
  }

  @Nullable
  private static ForwardIndex getForwardIndexMap(@Nonnull IndexExtension<?, ?, ?> indexExtension) throws IOException {
    if (!shouldCreateForwardIndex(indexExtension)) return null;
    if (indexExtension instanceof SingleEntryFileBasedIndexExtension<?>)
      return new EmptyForwardIndex(); // indexStorage and forwardIndex are same here
    File indexStorageFile = IndexInfrastructure.getInputIndexStorageFile((ID<?, ?>)indexExtension.getName());
    return new PersistentMapBasedForwardIndex(indexStorageFile, false);
  }

  private static boolean shouldCreateForwardIndex(@Nonnull IndexExtension<?, ?, ?> indexExtension) {
    if (hasSnapshotMapping(indexExtension)) return false;
    if (indexExtension instanceof CustomInputsIndexFileBasedIndexExtension) {
      LOG.error("Index `" + indexExtension.getName() + "` will be created without forward index");
      return false;
    }
    return true;
  }

  private void installMemoryModeListener() {
    IndexStorage<Key, Value> storage = getStorage();
    if (storage instanceof MemoryIndexStorage) {
      ((MemoryIndexStorage<Key, Value>)storage).addBufferingStateListener(new MemoryIndexStorage.BufferingStateListener() {
        @Override
        public void bufferingStateChanged(boolean newState) {
          myInMemoryMode.set(newState);
        }

        @Override
        public void memoryStorageCleared() {
          synchronized (myInMemoryKeysAndValues) {
            myInMemoryKeysAndValues.clear();
          }
        }
      });
    }
  }
}
