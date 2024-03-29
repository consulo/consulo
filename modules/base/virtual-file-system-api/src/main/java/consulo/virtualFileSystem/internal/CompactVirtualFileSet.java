// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.internal;

import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileWithId;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.function.Predicate;

/**
 * Set of VirtualFiles optimized for compact storage of very large number of files
 * Remove operations are not supported.
 */
public class CompactVirtualFileSet extends AbstractSet<VirtualFile> {
  // all non-VirtualFileWithId files and first several files are stored here
  private final Set<VirtualFile> weirdFiles = new HashSet<>();
  // when file set become large, they stored as id-set here
  private IntSet idSet;
  // when file set become very big (e.g. whole project files AnalysisScope) the bit-mask of their ids are stored here
  private BitSet fileIds;
  private boolean frozen;

  public CompactVirtualFileSet() {
  }

  public CompactVirtualFileSet(@Nonnull Collection<? extends VirtualFile> files) {
    addAll(files);
  }

  @Override
  public boolean contains(Object file) {
    if (file instanceof VirtualFileWithId) {
      BitSet ids = fileIds;
      int id = ((VirtualFileWithId)file).getId();
      if (ids != null) {
        return ids.get(id);
      }
      IntSet idSet = this.idSet;
      if (idSet != null) {
        return idSet.contains(id);
      }
    }
    return weirdFiles.contains(file);
  }

  @Override
  public boolean add(@Nonnull VirtualFile file) {
    if (frozen) {
      throw new UnsupportedOperationException();
    }
    boolean added;
    if (file instanceof VirtualFileWithId) {
      int id = ((VirtualFileWithId)file).getId();
      BitSet ids = fileIds;
      IntSet idSet = this.idSet;
      if (ids != null) {
        added = !ids.get(id);
        ids.set(id);
      }
      else if (idSet != null) {
        added = idSet.add(id);
        if (idSet.size() > 1000) {
          fileIds = new BitSet();
          idSet.forEach(i -> fileIds.set(i));
          this.idSet = null;
        }
      }
      else {
        added = weirdFiles.add(file);
        if (weirdFiles.size() > 10) {
          this.idSet = idSet = IntSets.newHashSet(weirdFiles.size());
          for (Iterator<VirtualFile> iterator = weirdFiles.iterator(); iterator.hasNext(); ) {
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
      added = weirdFiles.add(file);
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
    frozen = true;
  }

  public boolean process(@Nonnull Predicate<? super VirtualFile> processor) {
    VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
    BitSet ids = fileIds;
    if (ids != null) {
      for (int id = ids.nextSetBit(0); id < ids.size(); id = ids.nextSetBit(id + 1)) {
        if (id < 0) break;
        VirtualFile file = virtualFileManager.findFileById(id);
        if (file != null && !processor.test(file)) return false;
      }
    }
    IntSet idSet = this.idSet;
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
    return ContainerUtil.process(weirdFiles, processor);
  }

  @Override
  public int size() {
    BitSet ids = fileIds;
    IntSet idSet = this.idSet;
    return (ids == null ? 0 : ids.cardinality()) + (idSet == null ? 0 : idSet.size()) + weirdFiles.size();
  }

  @Nonnull
  @Override
  public Iterator<VirtualFile> iterator() {
    BitSet ids = fileIds;
    IntSet idSet = this.idSet;
    VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
    Iterator<VirtualFile> idsIterator = ids == null ? Collections.emptyIterator() : ContainerUtil.<VirtualFile>mapIterator(ids.stream().iterator(), virtualFileManager::findFileById);
    Iterator<VirtualFile> idSetIterator = idSet == null ? Collections.emptyIterator() : ContainerUtil.<VirtualFile>mapIterator(idSet.iterator(), virtualFileManager::findFileById);
    Iterator<VirtualFile> weirdFileIterator = weirdFiles.iterator();
    return ContainerUtil.filterIterator(ContainerUtil.concatIterators(idsIterator, idSetIterator, weirdFileIterator), Objects::nonNull);
  }
}
