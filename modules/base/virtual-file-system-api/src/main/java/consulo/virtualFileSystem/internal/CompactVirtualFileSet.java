// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.internal;

import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * Set of VirtualFiles optimized for compact storage of very large number of files
 * Remove operations are not supported.
 */
public class CompactVirtualFileSet extends AbstractSet<VirtualFile> {
  // all non-VirtualFileWithId files and first several files are stored here
  private final Set<VirtualFile> myWeirdFiles = new HashSet<>();
  // when file set become large, they stored as id-set here
  private @Nullable IntSet myIdSet = null;
  // when file set become very big (e.g. whole project files AnalysisScope) the bit-mask of their ids are stored here
  private @Nullable BitSet myFileIds = null;
  private boolean myFrozen = false;

  public CompactVirtualFileSet() {
  }

  public CompactVirtualFileSet(Collection<? extends VirtualFile> files) {
    addAll(files);
  }

  @Override
  public boolean contains(Object file) {
    if (file instanceof VirtualFileWithId) {
      BitSet ids = myFileIds;
      int id = ((VirtualFileWithId)file).getId();
      if (ids != null) {
        return ids.get(id);
      }
      IntSet idSet = myIdSet;
      if (idSet != null) {
        return idSet.contains(id);
      }
    }
    return myWeirdFiles.contains(file);
  }

  @Override
  public boolean add(VirtualFile file) {
    if (myFrozen) {
      throw new UnsupportedOperationException();
    }
    boolean added;
    if (file instanceof VirtualFileWithId) {
      int id = ((VirtualFileWithId)file).getId();
      BitSet ids = myFileIds;
      IntSet idSet = myIdSet;
      if (ids != null) {
        added = !ids.get(id);
        ids.set(id);
      }
      else if (idSet != null) {
        added = idSet.add(id);
        if (idSet.size() > 1000) {
          BitSet fileIds = new BitSet();
          myFileIds = fileIds;
          idSet.forEach(i -> fileIds.set(i));
          myIdSet = null;
        }
      }
      else {
        added = myWeirdFiles.add(file);
        if (myWeirdFiles.size() > 10) {
          myIdSet = idSet = IntSets.newHashSet(myWeirdFiles.size());
          for (Iterator<VirtualFile> iterator = myWeirdFiles.iterator(); iterator.hasNext(); ) {
            VirtualFile wf = iterator.next();
            if (wf instanceof VirtualFileWithId) {
              int i = ((VirtualFileWithId)wf).getId();
              idSet.add(i);
              iterator.remove();
            }
          }
        }
      }
    }
    else {
      added = myWeirdFiles.add(file);
    }
    return added;
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  /**
   * Make unmodifiable
   */
  public void freeze() {
    myFrozen = true;
  }

  public boolean process(Predicate<? super VirtualFile> processor) {
    VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
    BitSet ids = myFileIds;
    if (ids != null) {
      for (int id = ids.nextSetBit(0); id < ids.size(); id = ids.nextSetBit(id + 1)) {
        if (id < 0) break;
        VirtualFile file = virtualFileManager.findFileById(id);
        if (file != null && !processor.test(file)) return false;
      }
    }
    IntSet idSet = myIdSet;
    if (idSet != null) {
      PrimitiveIterator.OfInt iterator = idSet.iterator();

      while (iterator.hasNext()) {
        int id = iterator.nextInt();
        VirtualFile file = virtualFileManager.findFileById(id);

        if(file != null && !processor.test(file)) {
          return false;
        }
      }
    }
    return ContainerUtil.process(myWeirdFiles, processor);
  }

  @Override
  public int size() {
    BitSet ids = myFileIds;
    IntSet idSet = myIdSet;
    return (ids == null ? 0 : ids.cardinality()) + (idSet == null ? 0 : idSet.size()) + myWeirdFiles.size();
  }

  @Override
  public Iterator<VirtualFile> iterator() {
    BitSet ids = myFileIds;
    IntSet idSet = myIdSet;
    VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
    Iterator<VirtualFile> idsIterator = ids == null ? Collections.emptyIterator() : ContainerUtil.<VirtualFile>mapIterator(ids.stream().iterator(), virtualFileManager::findFileById);
    Iterator<VirtualFile> idSetIterator = idSet == null ? Collections.emptyIterator() : ContainerUtil.<VirtualFile>mapIterator(idSet.iterator(), virtualFileManager::findFileById);
    Iterator<VirtualFile> weirdFileIterator = myWeirdFiles.iterator();
    return ContainerUtil.filterIterator(ContainerUtil.concatIterators(idsIterator, idSetIterator, weirdFileIterator), Objects::nonNull);
  }
}
