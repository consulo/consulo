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
package com.intellij.openapi.vfs.newvfs.impl;

import com.intellij.openapi.application.ApplicationAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException;
import com.intellij.openapi.vfs.newvfs.persistent.FSRecords;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import com.intellij.util.ArrayUtil;
import com.intellij.util.concurrency.AtomicFieldUpdater;
import com.intellij.util.containers.ConcurrentBitSet;
import com.intellij.util.containers.ConcurrentIntObjectMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.keyFMap.KeyFMap;
import com.intellij.util.text.CaseInsensitiveStringHashingStrategy;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry.ALL_FLAGS_MASK;
import static com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry.EMPTY_ARRAY;
import static com.intellij.util.ObjectUtils.assertNotNull;

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
 * kept on hard references in {@link com.intellij.openapi.vfs.newvfs.persistent.PersistentFS}
 * <p>
 * 4. If a file is deleted (invalidated), then its data is not needed anymore, and should be removed. But this can only happen after
 * all the listener have been notified about the file deletion and have had their chance to look at the data the last time. See {@link #killInvalidatedFiles()}
 * <p>
 * 5. The file with removed data is marked as "dead" (see {@link #myDeadMarker}, any access to it will throw {@link com.intellij.openapi.vfs.InvalidVirtualFileAccessException}
 * Dead ids won't be reused in the same session of the IDE.
 *
 * @author peter
 */
public class VfsData {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.vfs.newvfs.impl.VfsData");
  private static final int SEGMENT_BITS = 9;
  private static final int SEGMENT_SIZE = 1 << SEGMENT_BITS;
  private static final int OFFSET_MASK = SEGMENT_SIZE - 1;
  private final Object myDeadMarker = new String("dead file");

  private TIntHashSet myDyingIds = new TIntHashSet();

  private final ConcurrentIntObjectMap<Segment> mySegments = ContainerUtil.createConcurrentIntObjectMap();
  private final ConcurrentBitSet myInvalidatedIds = new ConcurrentBitSet();
  private final ConcurrentIntObjectMap<VirtualDirectoryImpl> myChangedParents = ContainerUtil.createConcurrentIntObjectMap();

  private final PersistentFSImpl myPersistentFS;

  public VfsData(PersistentFSImpl persistentFS) {
    myPersistentFS = persistentFS;

    ApplicationManager.getApplication().addApplicationListener(new ApplicationAdapter() {
      @Override
      public void writeActionFinished(@Nonnull Object action) {
        // after top-level write action is finished, all the deletion listeners should have processed the deleted files
        // and their data is considered safe to remove. From this point on accessing a removed file will result in an exception.
        if (!ApplicationManager.getApplication().isWriteAccessAllowed()) {
          killInvalidatedFiles();
        }
      }
    });
  }

  private void killInvalidatedFiles() {
    synchronized (myDeadMarker) {
      if (!myDyingIds.isEmpty()) {
        for (int id : myDyingIds.toArray()) {
          assertNotNull(getSegment(id, false)).myObjectArray.set(getOffset(id), myDeadMarker);
          myChangedParents.remove(id);
        }
        myDyingIds = new TIntHashSet();
      }
    }
  }

  @Nullable
  public VirtualFileSystemEntry getFileById(int id, VirtualDirectoryImpl parent) {
    Segment segment = getSegment(id, false);
    if (segment == null) return null;

    int offset = getOffset(id);
    Object o = segment.myObjectArray.get(offset);
    if (o == null) return null;

    if (o == myDeadMarker) {
      throw reportDeadFileAccess(new VirtualFileImpl(id, segment, parent, myPersistentFS));
    }
    final int nameId = segment.getNameId(id);
    if (nameId <= 0) {
      FSRecords.invalidateCaches();
      throw new AssertionError("nameId=" + nameId + "; data=" + o + "; parent=" + parent + "; parent.id=" + parent.getId() + "; db.parent=" + FSRecords.getParent(id));
    }

    return o instanceof DirectoryData
           ? new VirtualDirectoryImpl(id, segment, (DirectoryData)o, parent, parent.getFileSystem(), myPersistentFS)
           : new VirtualFileImpl(id, segment, parent, myPersistentFS);
  }

  private static InvalidVirtualFileAccessException reportDeadFileAccess(VirtualFileSystemEntry file) {
    return new InvalidVirtualFileAccessException("Accessing dead virtual file: " + file.getUrl());
  }

  private static int getOffset(int id) {
    return id & OFFSET_MASK;
  }

  @Nullable
  @Contract("_,true->!null")
  public Segment getSegment(int id, boolean create) {
    int key = id >>> SEGMENT_BITS;
    Segment segment = mySegments.get(key);
    if (segment != null || !create) return segment;
    return mySegments.cacheOrGet(key, new Segment());
  }

  public static class FileAlreadyCreatedException extends Exception {
    private FileAlreadyCreatedException(String message) {
      super(message);
    }
  }

  public void initFile(int id, Segment segment, int nameId, @Nonnull Object data) throws FileAlreadyCreatedException {
    assert id > 0;
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

  CharSequence getNameByFileId(int id) {
    return FileNameCache.getVFileName(assertNotNull(getSegment(id, false)).getNameId(id));
  }

  boolean isFileValid(int id) {
    return !myInvalidatedIds.get(id);
  }

  @Nullable
  VirtualDirectoryImpl getChangedParent(int id) {
    return myChangedParents.get(id);
  }

  void changeParent(int id, VirtualDirectoryImpl parent) {
    myChangedParents.put(id, parent);
  }

  void invalidateFile(int id) {
    myInvalidatedIds.set(id);
    synchronized (myDeadMarker) {
      myDyingIds.add(id);
    }
  }

  public static class Segment {
    // user data for files, DirectoryData for folders
    private final AtomicReferenceArray<Object> myObjectArray = new AtomicReferenceArray<Object>(SEGMENT_SIZE);

    // <nameId, flags> pairs, "flags" part containing flags per se and modification stamp
    private final AtomicIntegerArray myIntArray = new AtomicIntegerArray(SEGMENT_SIZE * 2);

    int getNameId(int fileId) {
      return myIntArray.get(getOffset(fileId) * 2);
    }

    void setNameId(int fileId, int nameId) {
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
        int updated = value ? (oldInt | mask) : (oldInt & ~mask);
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
  public class DirectoryData {
    private final AtomicFieldUpdater<DirectoryData, KeyFMap> updater = AtomicFieldUpdater.forFieldOfType(DirectoryData.class, KeyFMap.class);
    @Nonnull
    volatile KeyFMap myUserMap = KeyFMap.EMPTY_MAP;
    @Nonnull
    int[] myChildrenIds = ArrayUtil.EMPTY_INT_ARRAY;
    private Set<String> myAdoptedNames;

    VirtualFileSystemEntry[] getFileChildren(int fileId, VirtualDirectoryImpl parent) {
      assert fileId > 0;
      if (myChildrenIds.length == 0) {
        return EMPTY_ARRAY;
      }
      VirtualFileSystemEntry[] children = new VirtualFileSystemEntry[myChildrenIds.length];
      for (int i = 0; i < myChildrenIds.length; i++) {
        children[i] = assertNotNull(getFileById(myChildrenIds[i], parent));
      }
      return children;
    }

    boolean changeUserMap(KeyFMap oldMap, KeyFMap newMap) {
      return updater.compareAndSet(this, oldMap, newMap);
    }

    boolean isAdoptedName(String name) {
      return myAdoptedNames != null && myAdoptedNames.contains(name);
    }

    void removeAdoptedName(String name) {
      if (myAdoptedNames != null) {
        myAdoptedNames.remove(name);
        if (myAdoptedNames.isEmpty()) {
          myAdoptedNames = null;
        }
      }
    }

    void addAdoptedName(String name, boolean caseSensitive) {
      if (myAdoptedNames == null) {
        //noinspection unchecked
        myAdoptedNames = new THashSet<String>(0, caseSensitive ? TObjectHashingStrategy.CANONICAL : CaseInsensitiveStringHashingStrategy.INSTANCE);
      }
      myAdoptedNames.add(name);
    }

    List<String> getAdoptedNames() {
      return myAdoptedNames == null ? Collections.<String>emptyList() : ContainerUtil.newArrayList(myAdoptedNames);
    }

    void clearAdoptedNames() {
      myAdoptedNames = null;
    }

    @Override
    public String toString() {
      return "DirectoryData{" + "myUserMap=" + myUserMap + ", myChildrenIds=" + Arrays.toString(myChildrenIds) + ", myAdoptedNames=" + myAdoptedNames + '}';
    }
  }

}
