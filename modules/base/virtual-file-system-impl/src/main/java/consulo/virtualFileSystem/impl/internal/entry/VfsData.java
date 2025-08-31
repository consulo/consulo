// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.entry;

import consulo.application.ApplicationManager;
import consulo.application.event.ApplicationListener;
import consulo.logging.Logger;
import consulo.util.collection.*;
import consulo.util.collection.primitive.ints.*;
import consulo.util.concurrent.AtomicFieldUpdater;
import consulo.util.dataholder.internal.keyFMap.KeyFMap;
import consulo.util.lang.BitUtil;
import consulo.util.lang.ByteArrayCharSequence;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.function.Functions;
import consulo.virtualFileSystem.InvalidVirtualFileAccessException;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.impl.internal.FSRecords;
import consulo.virtualFileSystem.impl.internal.FileNameCache;
import consulo.virtualFileSystem.impl.internal.PersistentFSImpl;
import consulo.virtualFileSystem.internal.PersistentFS;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static consulo.util.lang.ObjectUtil.assertNotNull;
import static consulo.virtualFileSystem.impl.internal.entry.VirtualFileSystemEntry.ALL_FLAGS_MASK;

/**
 * The place where all the data is stored for VFS parts loaded into a memory: name-ids, flags, user data, children.
 * <p>
 * The purpose is to avoid holding this data in separate immortal file/directory objects because that involves space overhead, significant
 * when there are hundreds of thousands of files.
 * <p>
 * The data is stored per-id in blocks of {@link #SEGMENT_SIZE}. File ids in one project tend to cluster together,
 * so the overhead for non-loaded id should not be large in most cases.
 * <p>
 * File objects are still created if needed. There might be several objects for the same file, so equals() should be used instead of ==.
 * <p>
 * The lifecycle of a file object is as follows:
 * <p>
 * 1. The file has not been instantiated yet, so {@link #getFileById} returns null.
 * <p>
 * 2. A file is explicitly requested by calling getChildren or findChild on its parent. The parent initializes all the necessary data (in a thread-safe context)
 * and creates the file instance. See {@link #initFile}
 * <p>
 * 3. After that the file is live, an object representing it can be retrieved any time from its parent. File system roots are
 * kept on hard references in {@link PersistentFS}
 * <p>
 * 4. If a file is deleted (invalidated), then its data is not needed anymore, and should be removed. But this can only happen after
 * all the listener have been notified about the file deletion and have had their chance to look at the data the last time. See {@link #killInvalidatedFiles()}
 * <p>
 * 5. The file with removed data is marked as "dead" (see {@link #myDeadMarker}, any access to it will throw {@link InvalidVirtualFileAccessException}
 * Dead ids won't be reused in the same session of the IDE.
 *
 * @author peter
 */
public class VfsData {
  private static final Logger LOG = Logger.getInstance(VfsData.class);
  private static final int SEGMENT_BITS = 9;
  private static final int SEGMENT_SIZE = 1 << SEGMENT_BITS;
  private static final int OFFSET_MASK = SEGMENT_SIZE - 1;

  private final Object myDeadMarker = ObjectUtil.sentinel("dead file");

  private final ConcurrentIntObjectMap<Segment> mySegments = IntMaps.newConcurrentIntObjectHashMap();
  private final ConcurrentBitSet myInvalidatedIds = new ConcurrentBitSet();
  private IntSet myDyingIds = IntSets.newHashSet();

  private boolean myHasChangedParents; // synchronized by read-write lock; clients outside read-action deserve to get outdated result
  private final IntObjectMap<VirtualDirectoryImpl> myChangedParents = IntMaps.newConcurrentIntObjectHashMap();

  public VfsData() {
    ApplicationManager.getApplication().addApplicationListener(new ApplicationListener() {
      @Override
      public void writeActionFinished(@Nonnull Object action) {
        // after top-level write action is finished, all the deletion listeners should have processed the deleted files
        // and their data is considered safe to remove. From this point on accessing a removed file will result in an exception.
        if (!ApplicationManager.getApplication().isWriteAccessAllowed()) {
          killInvalidatedFiles();
        }
      }
    }, ApplicationManager.getApplication());
  }

  private void killInvalidatedFiles() {
    synchronized (myDeadMarker) {
      if (!myDyingIds.isEmpty()) {
        for (int id : myDyingIds.toArray()) {
          Segment segment = assertNotNull(getSegment(id, false));
          segment.myObjectArray.set(getOffset(id), myDeadMarker);
          myChangedParents.remove(id);
        }
        myDyingIds = IntSets.newHashSet();
      }
    }
  }

  @Nullable
  VirtualFileSystemEntry getFileById(int id, @Nonnull VirtualDirectoryImpl parent) {
    PersistentFSImpl persistentFS = (PersistentFSImpl)PersistentFS.getInstance();
    VirtualFileSystemEntry dir = persistentFS.getCachedDir(id);
    if (dir != null) return dir;

    Segment segment = getSegment(id, false);
    if (segment == null) return null;

    int offset = getOffset(id);
    Object o = segment.myObjectArray.get(offset);
    if (o == null) return null;

    if (o == myDeadMarker) {
      throw reportDeadFileAccess(new VirtualFileImpl(id, segment, parent));
    }
    int nameId = segment.getNameId(id);
    if (nameId <= 0) {
      FSRecords.invalidateCaches();
      throw new AssertionError("nameId=" + nameId + "; data=" + o + "; parent=" + parent + "; parent.id=" + parent.getId() + "; db.parent=" + FSRecords.getParent(id));
    }

    return o instanceof DirectoryData
           ? persistentFS.getOrCacheDir(id, new VirtualDirectoryImpl(id, segment, (DirectoryData)o, parent, parent.getFileSystem()))
           : new VirtualFileImpl(id, segment, parent);
  }

  private static InvalidVirtualFileAccessException reportDeadFileAccess(VirtualFileSystemEntry file) {
    return new InvalidVirtualFileAccessException("Accessing dead virtual file: " + file.getUrl());
  }

  private static int getOffset(int id) {
    if (id <= 0) throw new IllegalArgumentException("invalid argument id: " + id);
    return id & OFFSET_MASK;
  }

  @Nullable
  @Contract("_,true->!null")
  public Segment getSegment(int id, boolean create) {
    int key = id >>> SEGMENT_BITS;
    Segment segment = mySegments.get(key);
    if (segment != null || !create) return segment;
    return mySegments.cacheOrGet(key, new Segment(this));
  }

  public boolean hasLoadedFile(int id) {
    Segment segment = getSegment(id, false);
    return segment != null && segment.myObjectArray.get(getOffset(id)) != null;
  }

  public static class FileAlreadyCreatedException extends Exception {
    private FileAlreadyCreatedException(String message) {
      super(message);
    }
  }

  static void initFile(int id, @Nonnull Segment segment, int nameId, @Nonnull Object data) throws FileAlreadyCreatedException {
    int offset = getOffset(id);

    segment.setNameId(id, nameId);

    Object existingData = segment.myObjectArray.get(offset);
    if (existingData != null) {
      FSRecords.invalidateCaches();
      int parent = FSRecords.getParent(id);
      String msg = "File already created: " + nameId + ", data=" + existingData + "; parentId=" + parent;
      if (parent > 0) {
        msg += "; parent.name=" + FSRecords.getName(parent);
        msg += "; parent.children=" + Arrays.toString(FSRecords.listAll(id));
      }
      throw new FileAlreadyCreatedException(msg);
    }
    segment.myObjectArray.set(offset, data);
  }

  @Nonnull
  CharSequence getNameByFileId(int id) {
    return FileNameCache.getVFileName(getNameId(id));
  }

  int getNameId(int id) {
    return assertNotNull(getSegment(id, false)).getNameId(id);
  }

  boolean isFileValid(int id) {
    return !myInvalidatedIds.get(id);
  }

  @Nullable
  VirtualDirectoryImpl getChangedParent(int id) {
    return myHasChangedParents ? myChangedParents.get(id) : null;
  }

  void changeParent(int id, VirtualDirectoryImpl parent) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    myHasChangedParents = true;
    myChangedParents.put(id, parent);
  }

  void invalidateFile(int id) {
    myInvalidatedIds.set(id);
    synchronized (myDeadMarker) {
      myDyingIds.add(id);
    }
  }

  static class Segment {
    // user data for files, DirectoryData for folders
    private final AtomicReferenceArray<Object> myObjectArray = new AtomicReferenceArray<>(SEGMENT_SIZE);

    // <nameId, flags> pairs, "flags" part containing flags per se and modification stamp
    private final AtomicIntegerArray myIntArray = new AtomicIntegerArray(SEGMENT_SIZE * 2);

    @Nonnull
    final VfsData vfsData;

    Segment(@Nonnull VfsData vfsData) {
      this.vfsData = vfsData;
    }

    int getNameId(int fileId) {
      return myIntArray.get(getOffset(fileId) * 2);
    }

    void setNameId(int fileId, int nameId) {
      if (fileId <= 0 || nameId <= 0) throw new IllegalArgumentException("invalid arguments id: " + fileId + "; nameId: " + nameId);
      myIntArray.set(getOffset(fileId) * 2, nameId);
    }

    void setUserMap(int fileId, @Nonnull KeyFMap map) {
      myObjectArray.set(getOffset(fileId), map);
    }

    KeyFMap getUserMap(VirtualFileSystemEntry file, int id) {
      Object o = myObjectArray.get(getOffset(id));
      if (!(o instanceof KeyFMap)) {
        throw reportDeadFileAccess(file);
      }
      return (KeyFMap)o;
    }

    boolean changeUserMap(int fileId, KeyFMap oldMap, KeyFMap newMap) {
      return myObjectArray.compareAndSet(getOffset(fileId), oldMap, newMap);
    }

    boolean getFlag(int id, int mask) {
      assert (mask & ~ALL_FLAGS_MASK) == 0 : "Unexpected flag";
      return (myIntArray.get(getOffset(id) * 2 + 1) & mask) != 0;
    }

    void setFlag(int id, int mask, boolean value) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Set flag " + Integer.toHexString(mask) + "=" + value + " for id=" + id);
      }
      assert (mask & ~ALL_FLAGS_MASK) == 0 : "Unexpected flag";
      int offset = getOffset(id) * 2 + 1;
      while (true) {
        int oldInt = myIntArray.get(offset);
        int updated = BitUtil.set(oldInt, mask, value);
        if (myIntArray.compareAndSet(offset, oldInt, updated)) {
          return;
        }
      }
    }

    long getModificationStamp(int id) {
      return myIntArray.get(getOffset(id) * 2 + 1) & ~ALL_FLAGS_MASK;
    }

    void setModificationStamp(int id, long stamp) {
      int offset = getOffset(id) * 2 + 1;
      while (true) {
        int oldInt = myIntArray.get(offset);
        int updated = (oldInt & ALL_FLAGS_MASK) | ((int)stamp & ~ALL_FLAGS_MASK);
        if (myIntArray.compareAndSet(offset, oldInt, updated)) {
          return;
        }
      }
    }

  }

  // non-final field accesses are synchronized on this instance, but this happens in VirtualDirectoryImpl
  static class DirectoryData {
    private static final AtomicFieldUpdater<DirectoryData, KeyFMap> MY_USER_MAP_UPDATER = AtomicFieldUpdater.forFieldOfType(DirectoryData.class, KeyFMap.class);
    @Nonnull
    volatile KeyFMap myUserMap = KeyFMap.EMPTY_MAP;
    /**
     * sorted by {@link VfsData#getNameByFileId(int)}
     * assigned under lock(this) only; never modified in-place
     *
     * @see VirtualDirectoryImpl#findIndex(int[], CharSequence, boolean)
     */
    @Nonnull
    volatile int[] myChildrenIds = ArrayUtil.EMPTY_INT_ARRAY; // guarded by this

    // assigned under lock(this) only; accessed/modified map contents under lock(myAdoptedNames)
    private volatile Set<CharSequence> myAdoptedNames;

    @Nonnull
    VirtualFileSystemEntry[] getFileChildren(@Nonnull VirtualDirectoryImpl parent) {
      int[] ids = myChildrenIds;
      VirtualFileSystemEntry[] children = new VirtualFileSystemEntry[ids.length];
      for (int i = 0; i < ids.length; i++) {
        int childId = ids[i];
        VirtualFileSystemEntry child = parent.mySegment.vfsData.getFileById(childId, parent);
        if (child == null) {
          throw new AssertionError("No file for id " + childId + ", parentId = " + parent.myId);
        }
        children[i] = child;
      }
      return children;
    }

    boolean changeUserMap(KeyFMap oldMap, KeyFMap newMap) {
      return MY_USER_MAP_UPDATER.compareAndSet(this, oldMap, newMap);
    }

    boolean isAdoptedName(@Nonnull CharSequence name) {
      Set<CharSequence> adopted = myAdoptedNames;
      if (adopted == null) {
        return false;
      }
      synchronized (adopted) {
        return adopted.contains(name);
      }
    }

    /**
     * must call removeAdoptedName() before adding new child with the same name
     * or otherwise {@link VirtualDirectoryImpl#doFindChild(String, boolean, NewVirtualFileSystem, boolean)} would risk finding already non-existing child
     * <p>
     * Must be called in synchronized(VfsData)
     */
    void removeAdoptedName(@Nonnull CharSequence name) {
      Set<CharSequence> adopted = myAdoptedNames;
      if (adopted == null) {
        return;
      }
      synchronized (adopted) {
        boolean removed = adopted.remove(name);
        if (removed && adopted.isEmpty()) {
          myAdoptedNames = null;
        }
      }
    }

    /**
     * Must be called in synchronized(VfsData)
     */
    void addAdoptedName(@Nonnull CharSequence name, boolean caseSensitive) {
      Set<CharSequence> adopted = getOrCreateAdoptedNames(caseSensitive);
      CharSequence sequence = ByteArrayCharSequence.convertToBytesIfPossible(name);
      synchronized (adopted) {
        adopted.add(sequence);
      }
    }

    /**
     * Optimization: faster than call {@link #addAdoptedName(CharSequence, boolean)} one by one
     * Must be called in synchronized(VfsData)
     */
    void addAdoptedNames(@Nonnull Collection<? extends CharSequence> names, boolean caseSensitive) {
      Set<CharSequence> adopted = getOrCreateAdoptedNames(caseSensitive);
      synchronized (adopted) {
        adopted.addAll(names);
      }
    }

    /**
     * Must be called in synchronized(VfsData)
     */
    @Nonnull
    private Set<CharSequence> getOrCreateAdoptedNames(boolean caseSensitive) {
      Set<CharSequence> adopted = myAdoptedNames;
      if (adopted == null) {
        myAdoptedNames = adopted = Sets.newHashSet(0, caseSensitive ? CharSequenceHashingStrategy.CASE_SENSITIVE : CharSequenceHashingStrategy.CASE_INSENSITIVE);
      }
      return adopted;
    }

    @Nonnull
    List<String> getAdoptedNames() {
      Set<CharSequence> adopted = myAdoptedNames;
      if (adopted == null) return Collections.emptyList();
      synchronized (adopted) {
        return ContainerUtil.map(adopted, Functions.TO_STRING());
      }
    }

    /**
     * Must be called in synchronized(VfsData)
     */
    void clearAdoptedNames() {
      myAdoptedNames = null;
    }

    @Override
    public String toString() {
      return "DirectoryData{" + "myUserMap=" + myUserMap + ", myChildrenIds=" + Arrays.toString(myChildrenIds) + ", myAdoptedNames=" + myAdoptedNames + '}';
    }
  }

}
