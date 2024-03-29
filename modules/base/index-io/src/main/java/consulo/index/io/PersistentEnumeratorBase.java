/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.index.io.data.DataEnumeratorEx;
import consulo.util.collection.SLRUMap;
import consulo.util.collection.ShareableKey;
import consulo.util.io.FileUtil;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author max
 * @author jeka
 */
public abstract class PersistentEnumeratorBase<Data> implements DataEnumeratorEx<Data>, Forceable, Closeable {
  protected static final Logger LOG = LoggerFactory.getLogger(PersistentEnumeratorBase.class);
  protected static final int NULL_ID = 0;

  private static final int META_DATA_OFFSET = 4;
  static final int DATA_START = META_DATA_OFFSET + 16;
  private static final CacheKey ourFlyweight = new FlyweightKey();

  protected final ResizeableMappedFile myStorage;
  private final boolean myAssumeDifferentSerializedBytesMeansObjectsInequality;
  private final AppendableStorageBackedByResizableMappedFile myKeyStorage;
  final KeyDescriptor<Data> myDataDescriptor;
  protected final File myFile;
  private final Version myVersion;
  private final boolean myDoCaching;

  private volatile boolean myDirtyStatusUpdateInProgress;

  private boolean myClosed;
  private boolean myDirty;
  private boolean myCorrupted;
  private RecordBufferHandler<PersistentEnumeratorBase> myRecordHandler;
  private Flushable myMarkCleanCallback;

  public static class Version {
    private static final int DIRTY_MAGIC = 0xbabe1977;
    private static final int CORRECTLY_CLOSED_MAGIC = 0xebabafd;

    private final int correctlyClosedMagic;
    private final int dirtyMagic;

    public Version(int version) {
      this(CORRECTLY_CLOSED_MAGIC + version, DIRTY_MAGIC);
    }

    private Version(int _correctlyClosedMagic, int _dirtyMagic) {
      correctlyClosedMagic = _correctlyClosedMagic;
      dirtyMagic = _dirtyMagic;
      assert correctlyClosedMagic != dirtyMagic;
    }
  }

  abstract static class RecordBufferHandler<T extends PersistentEnumeratorBase> {
    abstract int recordWriteOffset(T enumerator, byte[] buf);

    @Nonnull
    abstract byte[] getRecordBuffer(T enumerator);

    abstract void setupRecord(T enumerator, int hashCode, final int dataOffset, final byte[] buf);
  }

  private static class CacheKey implements ShareableKey {
    public PersistentEnumeratorBase owner;
    public Object key;

    private CacheKey(Object key, PersistentEnumeratorBase owner) {
      this.key = key;
      this.owner = owner;
    }

    @Override
    public ShareableKey getStableCopy() {
      return this;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof CacheKey)) return false;

      final CacheKey cacheKey = (CacheKey)o;

      if (!key.equals(cacheKey.key)) return false;
      if (!owner.equals(cacheKey.owner)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }
  }

  private static CacheKey sharedKey(Object key, PersistentEnumeratorBase owner) {
    ourFlyweight.key = key;
    ourFlyweight.owner = owner;
    return ourFlyweight;
  }

  private static final int ENUMERATION_CACHE_SIZE;

  static {
    String property = System.getProperty("idea.enumerationCacheSize");
    ENUMERATION_CACHE_SIZE = property == null ? 8192 : Integer.valueOf(property);
  }

  private static final SLRUMap<Object, Integer> ourEnumerationCache = new SLRUMap<>(ENUMERATION_CACHE_SIZE, ENUMERATION_CACHE_SIZE);

  @TestOnly
  public static void clearCacheForTests() {
    ourEnumerationCache.clear();
  }

  public static class CorruptedException extends IOException {
    public CorruptedException(File file) {
      this("PersistentEnumerator storage corrupted " + file.getPath());
    }

    protected CorruptedException(String message) {
      super(message);
    }
  }

  public static class VersionUpdatedException extends CorruptedException {
    VersionUpdatedException(@Nonnull File file) {
      super("PersistentEnumerator storage corrupted " + file.getPath());
    }
  }

  public PersistentEnumeratorBase(@Nonnull File file,
                                  @Nonnull ResizeableMappedFile storage,
                                  @Nonnull KeyDescriptor<Data> dataDescriptor,
                                  int initialSize,
                                  @Nonnull Version version,
                                  @Nonnull RecordBufferHandler<? extends PersistentEnumeratorBase> recordBufferHandler,
                                  boolean doCaching) throws IOException {
    myDataDescriptor = dataDescriptor;
    myFile = file;
    myVersion = version;
    myRecordHandler = (RecordBufferHandler<PersistentEnumeratorBase>)recordBufferHandler;
    myDoCaching = doCaching;

    if (!file.exists()) {
      FileUtil.delete(keyStreamFile());
      if (!FileUtil.createIfDoesntExist(file)) {
        throw new IOException("Cannot create empty file: " + file);
      }
    }

    myStorage = storage;

    lockStorage();
    try {
      if (myStorage.length() == 0) {
        try {
          markDirty(true);
          putMetaData(0);
          putMetaData2(0);
          setupEmptyFile();
        }
        catch (RuntimeException e) {
          LOG.info(e.getMessage(), e);
          myStorage.close();
          if (e.getCause() instanceof IOException) {
            throw (IOException)e.getCause();
          }
          throw e;
        }
        catch (IOException e) {
          LOG.info(e.getMessage(), e);
          myStorage.close();
          throw e;
        }
        catch (Exception e) {
          LOG.info(e.getMessage(), e);
          myStorage.close();
          throw new CorruptedException(file);
        }
      }
      else {
        int sign;
        try {
          sign = myStorage.getInt(0);
        }
        catch (Exception e) {
          LOG.info(e.getMessage(), e);
          sign = myVersion.dirtyMagic;
        }
        if (sign != myVersion.correctlyClosedMagic) {
          myStorage.close();
          if (sign != myVersion.dirtyMagic) throw new VersionUpdatedException(file);
          throw new CorruptedException(file);
        }
      }
    }
    finally {
      unlockStorage();
    }

    if (myDataDescriptor instanceof InlineKeyDescriptor) {
      myKeyStorage = null;
    }
    else {
      try {
        myKeyStorage = new AppendableStorageBackedByResizableMappedFile(keyStreamFile(), initialSize, myStorage.getPagedFileStorage().getStorageLockContext(), PagedFileStorage.MB, false);
      }
      catch (IOException e) {
        myStorage.close();
        throw e;
      }
      catch (Throwable e) {
        LOG.info(e.getMessage(), e);
        myStorage.close();
        throw new CorruptedException(file);
      }
    }
    myAssumeDifferentSerializedBytesMeansObjectsInequality = myDataDescriptor instanceof DifferentSerializableBytesImplyNonEqualityPolicy;
  }

  void lockStorage() {
    myStorage.getPagedFileStorage().lock();
  }

  void unlockStorage() {
    myStorage.getPagedFileStorage().unlock();
  }

  protected abstract void setupEmptyFile() throws IOException;

  @Nonnull
  final RecordBufferHandler<PersistentEnumeratorBase> getRecordHandler() {
    return myRecordHandler;
  }

  public void setRecordHandler(@Nonnull RecordBufferHandler<PersistentEnumeratorBase> recordHandler) {
    myRecordHandler = recordHandler;
  }

  void setMarkCleanCallback(Flushable markCleanCallback) {
    myMarkCleanCallback = markCleanCallback;
  }

  public Data getValue(int keyId, int processingKey) throws IOException {
    return valueOf(keyId);
  }

  @Override
  public int tryEnumerate(Data value) throws IOException {
    return doEnumerate(value, true, false);
  }

  private int doEnumerate(Data value, boolean onlyCheckForExisting, boolean saveNewValue) throws IOException {
    if (myDoCaching && !saveNewValue) {
      synchronized (ourEnumerationCache) {
        final Integer cachedId = ourEnumerationCache.get(sharedKey(value, this));
        if (cachedId != null) return cachedId.intValue();
      }
    }

    final int id;
    try {
      id = enumerateImpl(value, onlyCheckForExisting, saveNewValue);
    }
    catch (Throwable e) {
      if (!isCorrupted()) {
        markCorrupted();
        LOG.info("Marking corrupted:" + myFile, e);
      }

      //noinspection InstanceofCatchParameter
      if (e instanceof IOException) throw (IOException)e;
      throw new IOException(e);
    }

    if (myDoCaching && id != NULL_ID) {
      synchronized (ourEnumerationCache) {
        ourEnumerationCache.put(new CacheKey(value, this), id);
      }
    }

    return id;
  }

  public int enumerate(Data value) throws IOException {
    return doEnumerate(value, false, false);
  }

  public interface DataFilter {
    boolean accept(int id);
  }

  protected void putMetaData(long data) {
    lockStorage();
    try {
      if (myStorage.length() < META_DATA_OFFSET + 8 || getMetaData() != data) myStorage.putLong(META_DATA_OFFSET, data);
    }
    finally {
      unlockStorage();
    }
  }

  protected long getMetaData() {
    lockStorage();
    try {
      return myStorage.getLong(META_DATA_OFFSET);
    }
    finally {
      unlockStorage();
    }
  }

  void putMetaData2(long data) {
    lockStorage();
    try {
      if (myStorage.length() < META_DATA_OFFSET + 16 || getMetaData2() != data) myStorage.putLong(META_DATA_OFFSET + 8, data);
    }
    finally {
      unlockStorage();
    }
  }

  long getMetaData2() {
    lockStorage();
    try {
      return myStorage.getLong(META_DATA_OFFSET + 8);
    }
    finally {
      unlockStorage();
    }
  }

  public boolean processAllDataObject(@Nonnull final Predicate<? super Data> processor, @Nullable final DataFilter filter) throws IOException {
    return traverseAllRecords(new RecordsProcessor() {
      @Override
      public boolean process(final int record) throws IOException {
        if (filter == null || filter.accept(record)) {
          return processor.test(valueOf(record));
        }
        return true;
      }
    });

  }

  @Nonnull
  public Collection<Data> getAllDataObjects(@Nullable final DataFilter filter) throws IOException {
    final List<Data> values = new ArrayList<>();
    processAllDataObject(data -> {
      values.add(data);
      return true;
    }, filter);
    return values;
  }

  public abstract static class RecordsProcessor {
    private int myKey;

    public abstract boolean process(int record) throws IOException;

    void setCurrentKey(int key) {
      myKey = key;
    }

    int getCurrentKey() {
      return myKey;
    }
  }

  public abstract boolean traverseAllRecords(RecordsProcessor p) throws IOException;

  protected abstract int enumerateImpl(final Data value, final boolean onlyCheckForExisting, boolean saveNewValue) throws IOException;

  protected boolean isKeyAtIndex(final Data value, final int idx) throws IOException {
    if (myKeyStorage == null) return false;

    // check if previous serialized state is the same as for value
    // this is much faster than myDataDescriptor.isEqualTo(valueOf(idx), value) for identical objects
    // TODO: key storage lock
    final int addr = indexToAddr(idx);

    if (myKeyStorage.checkBytesAreTheSame(addr, value, myDataDescriptor)) return true;
    if (myAssumeDifferentSerializedBytesMeansObjectsInequality) return false;
    return myDataDescriptor.equals(valueOf(idx), value);
  }

  protected int writeData(final Data value, int hashCode) {
    try {
      markDirty(true);

      final int dataOff = doWriteData(value);

      return setupValueId(hashCode, dataOff);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public int getLargestId() {
    assert myKeyStorage != null;
    return myKeyStorage.getCurrentLength();
  }

  protected int doWriteData(Data value) throws IOException {
    if (myKeyStorage != null) {
      return myKeyStorage.append(value, myDataDescriptor);

    }
    return ((InlineKeyDescriptor<Data>)myDataDescriptor).toInt(value);
  }

  protected int setupValueId(int hashCode, int dataOff) {
    final byte[] buf = myRecordHandler.getRecordBuffer(this);
    myRecordHandler.setupRecord(this, hashCode, dataOff, buf);
    final int pos = myRecordHandler.recordWriteOffset(this, buf);
    myStorage.put(pos, buf, 0, buf.length);

    return pos;
  }

  public boolean iterateData(@Nonnull Predicate<? super Data> processor) throws IOException {
    if (myKeyStorage == null) {
      throw new UnsupportedOperationException("Iteration over InlineIntegerKeyDescriptors is not supported");
    }

    lockStorage(); // todo locking in key storage
    try {
      myKeyStorage.force();
    }
    finally {
      unlockStorage();
    }

    return myKeyStorage.processAll(processor, myDataDescriptor);
  }

  private File keyStreamFile() {
    return new File(myFile.getPath() + ".keystream");
  }

  public Data valueOf(int idx) throws IOException {
    try {

      lockStorage();
      try {
        int addr = indexToAddr(idx);

        if (myKeyStorage == null) return ((InlineKeyDescriptor<Data>)myDataDescriptor).fromInt(addr);
        return myKeyStorage.read(addr, myDataDescriptor);
      }
      finally {
        unlockStorage();
      }

    }
    catch (IOException io) {
      markCorrupted();
      throw io;
    }
    catch (Throwable e) {
      markCorrupted();
      throw new RuntimeException(e);
    }
  }

  int reEnumerate(Data key) throws IOException {
    if (!canReEnumerate()) throw new UnsupportedOperationException();
    return doEnumerate(key, false, true);
  }

  boolean canReEnumerate() {
    return false;
  }

  protected abstract int indexToAddr(int idx);

  @Override
  public synchronized void close() throws IOException {
    lockStorage();
    try {
      if (!myClosed) {
        myClosed = true;
        doClose();
      }
    }
    finally {
      unlockStorage();
    }
  }

  protected void doClose() throws IOException {
    try {
      if (myKeyStorage != null) {
        myKeyStorage.close();
      }
      flush();
    }
    finally {
      myStorage.close();
    }
  }

  public synchronized boolean isClosed() {
    return myClosed;
  }

  @Override
  public synchronized boolean isDirty() {
    return myDirty;
  }

  public synchronized boolean isCorrupted() {
    return myCorrupted;
  }

  private synchronized void flush() throws IOException {
    lockStorage();
    try {
      if (myStorage.isDirty() || isDirty()) {
        doFlush();
      }
    }
    finally {
      unlockStorage();
    }
  }

  protected void doFlush() throws IOException {
    markDirty(false);
    myStorage.force();
  }

  @Override
  public synchronized void force() {
    lockStorage();

    try {
      if (myKeyStorage != null) {
        myKeyStorage.force();
      }
      flush();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      unlockStorage();
    }
  }

  protected final void markDirty(boolean dirty) throws IOException {
    //assert Thread.holdsLock(this) || Thread.holdsLock(ourLock); // we hold one lock or another so can access myDirty
    if (dirty && myDirty && !myDirtyStatusUpdateInProgress) return;
    lockStorage();
    try {
      if (myDirty) {
        if (!dirty) {
          myDirtyStatusUpdateInProgress = true;
          if (myMarkCleanCallback != null) myMarkCleanCallback.flush();
          if (!myCorrupted) {
            myStorage.putInt(0, myVersion.correctlyClosedMagic);
            myDirty = false;
          }
          myDirtyStatusUpdateInProgress = false;
        }
      }
      else {
        if (dirty) {
          myDirtyStatusUpdateInProgress = true;
          myStorage.putInt(0, myVersion.dirtyMagic);
          myDirtyStatusUpdateInProgress = false;
          myDirty = true;
        }
      }
    }
    finally {
      unlockStorage();
    }
  }

  protected synchronized void markCorrupted() {
    if (!myCorrupted) {
      myCorrupted = true;
      if (LOG.isDebugEnabled()) LOG.debug("Marking corrupted:" + myFile, new Throwable());
      try {
        markDirty(true);
        force();
      }
      catch (IOException e) {
        // ignore...
      }
    }
  }

  private static class FlyweightKey extends CacheKey {
    FlyweightKey() {
      super(null, null);
    }

    @Override
    public ShareableKey getStableCopy() {
      return new CacheKey(key, owner);
    }
  }
}
