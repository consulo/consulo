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
package consulo.ide.impl.idea.util.indexing;

import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.fileType.FileType;
import gnu.trove.TObjectIntHashMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

class IndexConfiguration {
  private final Map<ID<?, ?>, Pair<UpdatableIndex<?, ?, FileContent>, FileBasedIndex.InputFilter>> myIndices = new HashMap<>();
  private final TObjectIntHashMap<ID<?, ?>> myIndexIdToVersionMap = new TObjectIntHashMap<>();
  private final List<ID<?, ?>> myIndicesWithoutFileTypeInfo = new ArrayList<>();
  private final Map<FileType, List<ID<?, ?>>> myFileType2IndicesWithFileTypeInfoMap = new HashMap<>();
  private volatile boolean myFreezed;

  <K, V> UpdatableIndex<K, V, FileContent> getIndex(ID<K, V> indexId) {
    assert myFreezed;
    final Pair<UpdatableIndex<?, ?, FileContent>, FileBasedIndex.InputFilter> pair = myIndices.get(indexId);

    //noinspection unchecked
    return pair != null ? (UpdatableIndex<K, V, FileContent>)pair.getFirst() : null;
  }

  FileBasedIndex.InputFilter getInputFilter(@Nonnull ID<?, ?> indexId) {
    assert myFreezed;
    final Pair<UpdatableIndex<?, ?, FileContent>, FileBasedIndex.InputFilter> pair = myIndices.get(indexId);

    assert pair != null : "Index data is absent for index " + indexId;

    return pair.getSecond();
  }

  void freeze() {
    myFreezed = true;
  }

  <K, V> void registerIndex(ID<K, V> name,
                            @Nonnull UpdatableIndex<K, V, FileContent> index,
                            FileBasedIndex.InputFilter inputFilter,
                            int version,
                            @Nullable Collection<? extends FileType> associatedFileTypes) {
    assert !myFreezed;

    synchronized (myIndices) {
      myIndexIdToVersionMap.put(name, version);

      if (associatedFileTypes != null) {
        for (FileType fileType : associatedFileTypes) {
          List<ID<?, ?>> ids = myFileType2IndicesWithFileTypeInfoMap.computeIfAbsent(fileType, __ -> new ArrayList<>(5));
          ids.add(name);
        }
      }
      else {
        myIndicesWithoutFileTypeInfo.add(name);
      }

      Pair<UpdatableIndex<?, ?, FileContent>, FileBasedIndex.InputFilter> old = myIndices.put(name, new Pair<>(index, inputFilter));
      if (old != null) {
        throw new IllegalStateException("Index " + old.first + " already registered for the name '" + name + "'");
      }
    }
  }

  @Nonnull
  List<ID<?, ?>> getFileTypesForIndex(@Nonnull FileType fileType) {
    assert myFreezed;
    List<ID<?, ?>> ids = myFileType2IndicesWithFileTypeInfoMap.get(fileType);
    if (ids == null) ids = myIndicesWithoutFileTypeInfo;
    return ids;
  }

  void finalizeFileTypeMappingForIndices() {
    assert !myFreezed;
    synchronized (myIndices) {
      for (List<ID<?, ?>> value : myFileType2IndicesWithFileTypeInfoMap.values()) {
        value.addAll(myIndicesWithoutFileTypeInfo);
      }
    }
  }

  Collection<ID<?, ?>> getIndexIDs() {
    assert myFreezed;
    return myIndices.keySet();
  }

  boolean hasIndex(ID<?, ?> name) {
    assert myFreezed;
    return myIndices.containsKey(name);
  }

  int getIndexVersion(ID<?, ?> id) {
    assert myFreezed;
    return myIndexIdToVersionMap.get(id);
  }
}
