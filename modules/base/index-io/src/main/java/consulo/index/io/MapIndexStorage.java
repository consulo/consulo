/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.index.io;

import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.IOUtil;
import consulo.index.io.internal.ValueContainerImpl;
import consulo.util.collection.SLRUCache;
import consulo.util.lang.LoggerAssert;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

public abstract class MapIndexStorage<Key, Value> implements IndexStorage<Key, Value> {
  private static final Logger LOG = LoggerFactory.getLogger(MapIndexStorage.class);
  protected PersistentMap<Key, UpdatableValueContainer<Value>> myMap;
  protected SLRUCache<Key, ChangeTrackingValueContainer<Value>> myCache;
  protected final File myBaseStorageFile;
  protected final KeyDescriptor<Key> myKeyDescriptor;
  private final int myCacheSize;

  protected final Lock l = new ReentrantLock();
  private final DataExternalizer<Value> myDataExternalizer;
  private final boolean myKeyIsUniqueForIndexedFile;
  private final boolean myReadOnly;
  @Nonnull
  private final IntUnaryOperator myInputRemapping;

  protected MapIndexStorage(@Nonnull File storageFile,
                            @Nonnull KeyDescriptor<Key> keyDescriptor,
                            @Nonnull DataExternalizer<Value> valueExternalizer,
                            int cacheSize,
                            boolean keyIsUniqueForIndexedFile) throws IOException {
    this(storageFile, keyDescriptor, valueExternalizer, cacheSize, keyIsUniqueForIndexedFile, true, false, null);
  }

  protected MapIndexStorage(@Nonnull File storageFile,
                            @Nonnull KeyDescriptor<Key> keyDescriptor,
                            @Nonnull DataExternalizer<Value> valueExternalizer,
                            int cacheSize,
                            boolean keyIsUniqueForIndexedFile,
                            boolean initialize,
                            boolean readOnly,
                            @Nullable IntUnaryOperator inputRemapping) throws IOException {
    myBaseStorageFile = storageFile;
    myKeyDescriptor = keyDescriptor;
    myCacheSize = cacheSize;
    myDataExternalizer = valueExternalizer;
    myKeyIsUniqueForIndexedFile = keyIsUniqueForIndexedFile;
    myReadOnly = readOnly;
    if (inputRemapping != null) {
      LoggerAssert.assertTrue(LOG, myReadOnly, "input remapping allowed only for read-only storage");
    }
    else {
      inputRemapping = operand -> operand;
    }
    myInputRemapping = inputRemapping;
    if (initialize) initMapAndCache();
  }

  protected void initMapAndCache() throws IOException {
    final ValueContainerMap<Key, Value> map;
    PersistentHashMapValueStorage.CreationTimeOptions.EXCEPTIONAL_IO_CANCELLATION.set(() -> checkCanceled());
    PersistentHashMapValueStorage.CreationTimeOptions.COMPACT_CHUNKS_WITH_VALUE_DESERIALIZATION.set(Boolean.TRUE);
    if (myKeyIsUniqueForIndexedFile) {
      PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(Boolean.TRUE);
    }
    try {
      map = new ValueContainerMap<Key, Value>(getStorageFile(), myKeyDescriptor, myDataExternalizer, myKeyIsUniqueForIndexedFile, myInputRemapping) {
        @Override
        protected boolean isReadOnly() {
          return myReadOnly;
        }
      };
    }
    finally {
      PersistentHashMapValueStorage.CreationTimeOptions.EXCEPTIONAL_IO_CANCELLATION.set(null);
      PersistentHashMapValueStorage.CreationTimeOptions.COMPACT_CHUNKS_WITH_VALUE_DESERIALIZATION.set(null);
      if (myKeyIsUniqueForIndexedFile) {
        PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(Boolean.FALSE);
      }
    }
    myCache = new SLRUCache<Key, ChangeTrackingValueContainer<Value>>(myCacheSize, (int)(Math.ceil(myCacheSize * 0.25)) /* 25% from the main cache size*/, myKeyDescriptor) {
      @Override
      @Nonnull
      public ChangeTrackingValueContainer<Value> createValue(final Key key) {
        return new ChangeTrackingValueContainer<>(new ChangeTrackingValueContainer.Initializer<Value>() {
          @Nonnull
          @Override
          public Object getLock() {
            return map.getDataAccessLock();
          }

          @Nonnull
          @Override
          public ValueContainer<Value> compute() {
            ValueContainer<Value> value;
            try {
              value = map.get(key);
              if (value == null) {
                value = new ValueContainerImpl<>();
              }
            }
            catch (IOException e) {
              throw new RuntimeException(e);
            }
            return value;
          }
        });
      }

      @Override
      protected void onDropFromCache(Key key, @Nonnull ChangeTrackingValueContainer<Value> valueContainer) {
        if (!myReadOnly && valueContainer.isDirty()) {
          try {
            map.put(key, valueContainer);
          }
          catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    };

    myMap = map;
  }

  protected abstract void checkCanceled();

  @Nonnull
  private File getStorageFile() {
    return getIndexStorageFile(myBaseStorageFile);
  }

  @Override
  public void flush() {
    l.lock();
    try {
      if (!myMap.isClosed()) {
        myCache.clear();
        if (myMap.isDirty()) myMap.force();
      }
    }
    finally {
      l.unlock();
    }
  }

  @Override
  public void close() throws StorageException {
    try {
      flush();
      myMap.close();
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
    catch (RuntimeException e) {
      unwrapCauseAndRethrow(e);
    }
  }

  @Override
  public void clear() throws StorageException {
    try {
      myMap.close();
    }
    catch (IOException | RuntimeException e) {
      LOG.info(e.getMessage(), e);
    }
    try {
      IOUtil.deleteAllFilesStartingWith(getStorageFile());
      initMapAndCache();
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
    catch (RuntimeException e) {
      unwrapCauseAndRethrow(e);
    }
  }

  @Override
  @Nonnull
  public ChangeTrackingValueContainer<Value> read(Key key) throws StorageException {
    l.lock();
    try {
      return myCache.get(key);
    }
    catch (RuntimeException e) {
      return unwrapCauseAndRethrow(e);
    }
    finally {
      l.unlock();
    }
  }

  @Override
  public void addValue(Key key, int inputId, Value value) throws StorageException {
    if (myReadOnly) {
      throw new UnsupportedOperationException("Index storage is read-only");
    }
    try {
      myMap.markDirty();
      if (!myKeyIsUniqueForIndexedFile) {
        read(key).addValue(inputId, value);
        return;
      }

      ChangeTrackingValueContainer<Value> cached;
      try {
        l.lock();
        cached = myCache.getIfCached(key);
      }
      finally {
        l.unlock();
      }

      if (cached != null) {
        cached.addValue(inputId, value);
        return;
      }
      // do not pollute the cache with keys unique to indexed file
      ChangeTrackingValueContainer<Value> valueContainer = new ChangeTrackingValueContainer<>(null);
      valueContainer.addValue(inputId, value);
      myMap.put(key, valueContainer);
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void removeAllValues(@Nonnull Key key, int inputId) throws StorageException {
    try {
      myMap.markDirty();
      // important: assuming the key exists in the index
      read(key).removeAssociatedValue(inputId);
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public void clearCaches() {
    l.lock();
    try {
      for (Map.Entry<Key, ChangeTrackingValueContainer<Value>> entry : myCache.entrySet()) {
        entry.getValue().dropMergedData();
      }
    }
    finally {
      l.unlock();
    }
  }

  protected static <T> T unwrapCauseAndRethrow(RuntimeException e) throws StorageException {
    Throwable cause = e.getCause();
    if (cause instanceof IOException) {
      throw new StorageException(cause);
    }
    if (cause instanceof StorageException) {
      throw (StorageException)cause;
    }
    throw e;
  }

  @TestOnly
  public boolean processKeys(@Nonnull Predicate<? super Key> processor) throws StorageException {
    l.lock();
    try {
      myCache.clear(); // this will ensure that all new keys are made into the map
      return myMap.processKeys(processor);
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
    catch (RuntimeException e) {
      unwrapCauseAndRethrow(e);
      return false;
    }
    finally {
      l.unlock();
    }
  }

  @TestOnly
  public PersistentMap<Key, UpdatableValueContainer<Value>> getIndexMap() {
    return myMap;
  }

  @Nonnull
  public static File getIndexStorageFile(@Nonnull File baseFile) {
    return new File(baseFile.getPath() + ".storage");
  }
}
