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
package com.intellij.vcs.log.data;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.VcsRef;
import com.intellij.vcs.log.util.TroveUtil;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import gnu.trove.TIntObjectHashMap;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompressedRefs {
  @Nonnull
  private final VcsLogStorage myHashMap;

  // maps each commit id to the list of tag ids on this commit
  @Nonnull
  private final TIntObjectHashMap<IntList> myTags = new TIntObjectHashMap<>();
  // maps each commit id to the list of branches on this commit
  @Nonnull
  private final TIntObjectHashMap<List<VcsRef>> myBranches = new TIntObjectHashMap<>();

  public CompressedRefs(@Nonnull Set<VcsRef> refs, @Nonnull VcsLogStorage hashMap) {
    myHashMap = hashMap;

    Ref<VirtualFile> root = new Ref<>();

    refs.forEach(ref -> {
      assert root.get() == null || root.get().equals(ref.getRoot()) : "All references are supposed to be from the single root";
      root.set(ref.getRoot());

      if (ref.getType().isBranch()) {
        putRef(myBranches, ref, myHashMap);
      }
      else {
        putRefIndex(myTags, ref, myHashMap);
      }
    });
    myTags.forEachValue(list -> {
      IntLists.trimToSize(list);
      return true;
    });
    myHashMap.flush();
  }

  @Nonnull
  SmartList<VcsRef> refsToCommit(int index) {
    SmartList<VcsRef> result = new SmartList<>();
    if (myBranches.containsKey(index)) result.addAll(myBranches.get(index));
    IntList tags = myTags.get(index);
    if (tags != null) {
      tags.forEach(value -> {
        result.add(myHashMap.getVcsRef(value));
      });
    }
    return result;
  }

  @Nonnull
  Stream<VcsRef> streamBranches() {
    return TroveUtil.streamValues(myBranches).flatMap(Collection::stream);
  }

  @Nonnull
  private Stream<VcsRef> streamTags() {
    return TroveUtil.streamValues(myTags).flatMapToInt(TroveUtil::stream).mapToObj(myHashMap::getVcsRef);
  }

  @Nonnull
  public Stream<VcsRef> stream() {
    return Stream.concat(streamBranches(), streamTags());
  }

  @Nonnull
  public Collection<VcsRef> getRefs() {
    return new AbstractCollection<VcsRef>() {
      private final Supplier<Collection<VcsRef>> myLoadedRefs =
              Suppliers.memoize(() -> CompressedRefs.this.stream().collect(Collectors.toList()));

      @Nonnull
      @Override
      public Iterator<VcsRef> iterator() {
        return myLoadedRefs.get().iterator();
      }

      @Override
      public int size() {
        return myLoadedRefs.get().size();
      }
    };
  }

  @Nonnull
  public Collection<Integer> getCommits() {
    Set<Integer> result = ContainerUtil.newHashSet();
    TroveUtil.streamKeys(myBranches).forEach(result::add);
    TroveUtil.streamKeys(myTags).forEach(result::add);
    return result;
  }

  private static void putRef(@Nonnull TIntObjectHashMap<List<VcsRef>> map, @Nonnull VcsRef ref, @Nonnull VcsLogStorage hashMap) {
    int index = hashMap.getCommitIndex(ref.getCommitHash(), ref.getRoot());
    List<VcsRef> list = map.get(index);
    if (list == null) map.put(index, list = new SmartList<>());
    list.add(ref);
  }

  private static void putRefIndex(@Nonnull TIntObjectHashMap<IntList> map, @Nonnull VcsRef ref, @Nonnull VcsLogStorage hashMap) {
    int index = hashMap.getCommitIndex(ref.getCommitHash(), ref.getRoot());
    IntList list = map.get(index);
    if (list == null) map.put(index, list = IntLists.newArrayList());
    list.add(hashMap.getRefIndex(ref));
  }
}
