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

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.versionControlSystem.log.VcsCommitMetadata;
import consulo.versionControlSystem.log.VcsLogStorage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TopCommitsCache {
  @Nonnull
  private final VcsLogStorage myHashMap;
  @Nonnull
  private final ConcurrentIntObjectMap<VcsCommitMetadata> myCache = IntMaps.newConcurrentIntObjectHashMap();
  @Nonnull
  private List<VcsCommitMetadata> mySortedDetails = new ArrayList<>();

  public TopCommitsCache(@Nonnull VcsLogStorage hashMap) {
    myHashMap = hashMap;
  }

  private int getIndex(@Nonnull VcsCommitMetadata metadata) {
    return myHashMap.getCommitIndex(metadata.getId(), metadata.getRoot());
  }

  public void storeDetails(@Nonnull List<? extends VcsCommitMetadata> sortedDetails) {
    List<VcsCommitMetadata> newDetails = ContainerUtil.filter(sortedDetails, metadata -> !myCache.containsValue(metadata));
    if (newDetails.isEmpty()) return;
    Iterator<VcsCommitMetadata> it = new MergingIterator(mySortedDetails, newDetails);

    List<VcsCommitMetadata> result = new ArrayList<>();
    boolean isBroken = false;
    while (it.hasNext()) {
      VcsCommitMetadata detail = it.next();
      int index = getIndex(detail);
      if (index == VcsLogStorageImpl.NO_INDEX) {
        isBroken = true;
        continue; // means some error happened (and reported) earlier, nothing we can do here
      }
      if (result.size() < VcsLogDataImpl.RECENT_COMMITS_COUNT * 2) {
        result.add(detail);
        myCache.put(index, detail);
      }
      else {
        myCache.remove(index);
      }
    }
    assert result.size() == myCache.size() || isBroken : result.size() + " details to store, yet " + myCache.size() + " indexes in cache.";
    mySortedDetails = result;
  }

  @Nullable
  public VcsCommitMetadata get(int index) {
    return myCache.get(index);
  }

  public void clear() {
    myCache.clear();
    mySortedDetails.clear();
  }

  private static class MergingIterator implements Iterator<VcsCommitMetadata> {
    private final PeekingIterator<VcsCommitMetadata> myFirst;
    private final PeekingIterator<VcsCommitMetadata> mySecond;

    private MergingIterator(@Nonnull List<VcsCommitMetadata> first, @Nonnull List<VcsCommitMetadata> second) {
      myFirst = Iterators.peekingIterator(first.iterator());
      mySecond = Iterators.peekingIterator(second.iterator());
    }

    @Override
    public boolean hasNext() {
      return myFirst.hasNext() || mySecond.hasNext();
    }

    @Override
    public VcsCommitMetadata next() {
      if (!myFirst.hasNext()) return mySecond.next();
      if (!mySecond.hasNext()) return myFirst.next();
      VcsCommitMetadata data1 = myFirst.peek();
      VcsCommitMetadata data2 = mySecond.peek();
      // more recent commits (with bigger timestamp) should go first
      // if timestamp is the same, commit from the second list is chosen
      if (data1.getTimestamp() > data2.getTimestamp()) return myFirst.next();
      return mySecond.next();
    }
  }
}
