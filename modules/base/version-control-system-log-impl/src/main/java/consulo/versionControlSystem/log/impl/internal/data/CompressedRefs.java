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
import consulo.util.lang.lazy.LazyValue;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.VcsRef;
import consulo.virtualFileSystem.VirtualFile;
import it.unimi.dsi.fastutil.ints.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompressedRefs {
    private final VcsLogStorage myStorage;

    // maps each commit id to the list of tag ids on this commit
    private final Int2ObjectMap<IntList> myTags = new Int2ObjectOpenHashMap<>();
    // maps each commit id to the list of branches on this commit
    private final Int2ObjectMap<List<VcsRef>> myBranches = new Int2ObjectOpenHashMap<>();

    public CompressedRefs(Set<VcsRef> refs, VcsLogStorage storage) {
        myStorage = storage;

        SimpleReference<VirtualFile> root = new SimpleReference<>();

        refs.forEach(ref -> {
            assert root.get() == null || root.get().equals(ref.getRoot()) : "All references are supposed to be from the single root";
            root.set(ref.getRoot());

            if (ref.getType().isBranch()) {
                putRef(myBranches, ref, myStorage);
            }
            else {
                putRefIndex(myTags, ref, myStorage);
            }
        });
        myTags.values().forEach(intList -> ((IntArrayList) intList).trim());
        myStorage.flush();
    }

    SmartList<VcsRef> refsToCommit(int index) {
        SmartList<VcsRef> result = new SmartList<>();
        if (myBranches.containsKey(index)) {
            result.addAll(myBranches.get(index));
        }
        IntList tags = myTags.get(index);
        if (tags != null) {
            tags.forEach(value -> result.add(myStorage.getVcsRef(value)));
        }
        return result;
    }

    Stream<VcsRef> streamBranches() {
        return myBranches.values().stream().flatMap(Collection::stream);
    }

    private Stream<VcsRef> streamTags() {
        return myTags.values().stream()
            .flatMapToInt(IntList::intStream)
            .mapToObj(myStorage::getVcsRef);
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

    public IntCollection getCommits() {
        IntSet result = new IntOpenHashSet(myBranches.size() + myTags.size());
        result.addAll(myBranches.keySet());
        result.addAll(myTags.keySet());
        return result;
    }

    private static void putRef(Int2ObjectMap<List<VcsRef>> map, VcsRef ref, VcsLogStorage storage) {
        int index = storage.getCommitIndex(ref.getCommitHash(), ref.getRoot());
        List<VcsRef> list = map.get(index);
        if (list == null) {
            map.put(index, list = new SmartList<>());
        }
        list.add(ref);
    }

    private static void putRefIndex(Int2ObjectMap<IntList> map, VcsRef ref, VcsLogStorage storage) {
        int index = storage.getCommitIndex(ref.getCommitHash(), ref.getRoot());
        IntList list = map.get(index);
        if (list == null) {
            map.put(index, list = new IntArrayList());
        }
        list.add(storage.getRefIndex(ref));
    }
}
