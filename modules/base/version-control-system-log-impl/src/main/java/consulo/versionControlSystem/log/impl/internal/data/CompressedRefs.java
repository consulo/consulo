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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.util.collection.SmartList;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.impl.internal.util.TroveUtil;
import consulo.virtualFileSystem.VirtualFile;
import gnu.trove.TIntObjectHashMap;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompressedRefs {
  
  private final VcsLogStorage myHashMap;

  // maps each commit id to the list of tag ids on this commit
  
  private final TIntObjectHashMap<IntList> myTags = new TIntObjectHashMap<>();
  // maps each commit id to the list of branches on this commit
  
  private final TIntObjectHashMap<List<VcsRef>> myBranches = new TIntObjectHashMap<>();

  public CompressedRefs(Set<VcsRef> refs, VcsLogStorage hashMap) {
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

  
  SmartList<VcsRef> refsToCommit(int index) {
    SmartList<VcsRef> result = new SmartList<>();
    if (myBranches.containsKey(index)) result.addAll(myBranches.get(index));
    IntList tags = myTags.get(index);
    if (tags != null) {
      tags.forEach(value -> result.add(myHashMap.getVcsRef(value)));
    }
    return result;
  }

  
  Stream<VcsRef> streamBranches() {
    return TroveUtil.streamValues(myBranches).flatMap(Collection::stream);
  }

  
  private Stream<VcsRef> streamTags() {
    return TroveUtil.streamValues(myTags).flatMapToInt(TroveUtil::stream).mapToObj(myHashMap::getVcsRef);
  }

  
  public Stream<VcsRef> stream() {
    return Stream.concat(streamBranches(), streamTags());
  }

  
  public Collection<VcsRef> getRefs() {
    return new AbstractCollection<>() {
      private final Supplier<Collection<VcsRef>> myLoadedRefs =
        LazyValue.notNull(() -> CompressedRefs.this.stream().collect(Collectors.toList()));

      
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

  
  public Collection<Integer> getCommits() {
    Set<Integer> result = new HashSet<>();
    TroveUtil.streamKeys(myBranches).forEach(result::add);
    TroveUtil.streamKeys(myTags).forEach(result::add);
    return result;
  }

  private static void putRef(TIntObjectHashMap<List<VcsRef>> map, VcsRef ref, VcsLogStorage hashMap) {
    int index = hashMap.getCommitIndex(ref.getCommitHash(), ref.getRoot());
    List<VcsRef> list = map.get(index);
    if (list == null) map.put(index, list = new SmartList<>());
    list.add(ref);
  }

  private static void putRefIndex(TIntObjectHashMap<IntList> map, VcsRef ref, VcsLogStorage hashMap) {
    int index = hashMap.getCommitIndex(ref.getCommitHash(), ref.getRoot());
    IntList list = map.get(index);
    if (list == null) map.put(index, list = IntLists.newArrayList());
    list.add(hashMap.getRefIndex(ref));
  }
}
