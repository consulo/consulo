/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.graph;

import consulo.logging.Logger;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsLogRefManager;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.graph.GraphColorManager;
import consulo.versionControlSystem.log.impl.internal.data.RefsModel;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class GraphColorManagerImpl implements GraphColorManager<Integer> {

  private static final Logger LOG = Logger.getInstance(GraphColorManagerImpl.class);
  static final int DEFAULT_COLOR = 0;

  @Nonnull
  private final HeadsComparator myHeadsComparator;
  @Nonnull
  private final RefsModel myRefsModel;

  public GraphColorManagerImpl(@Nonnull RefsModel refsModel,
                               @Nonnull Function<Integer, Hash> hashGetter,
                               @Nonnull Map<VirtualFile, VcsLogRefManager> refManagers) {
    myRefsModel = refsModel;

    myHeadsComparator = new HeadsComparator(refsModel, refManagers, hashGetter);
  }

  @Override
  public int getColorOfBranch(Integer headCommit) {
    VcsRef firstRef = myRefsModel.bestRefToHead(headCommit);
    if (firstRef == null) {
      return DEFAULT_COLOR;
    }
    // TODO dark variant
    return firstRef.getName().hashCode();
  }

  @Override
  public int getColorOfFragment(Integer headCommit, int magicIndex) {
    return magicIndex;
  }

  @Override
  public int compareHeads(Integer head1, Integer head2) {
    return myHeadsComparator.compare(head1, head2);
  }

  public static class HeadsComparator implements Comparator<Integer> {
    @Nonnull
    private final RefsModel myRefsModel;
    @Nonnull
    private final Map<VirtualFile, VcsLogRefManager> myRefManagers;
    @Nonnull
    private final Function<Integer, Hash> myHashGetter;

    @Nonnull
    private final LinkedHashMap<Integer, Integer> myErrorWasReported = new LinkedHashMap<>(10) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
        return size() > 100;
      }
    };

    public HeadsComparator(@Nonnull RefsModel refsModel,
                           @Nonnull Map<VirtualFile, VcsLogRefManager> refManagers,
                           @Nonnull Function<Integer, Hash> hashGetter) {
      myRefsModel = refsModel;
      myRefManagers = refManagers;
      myHashGetter = hashGetter;
    }

    public void reportNoRefs(int head) {
      if (!myErrorWasReported.containsKey(head)) {
        myErrorWasReported.put(head, head);
        LOG.warn("No references found at head " + head + " which corresponds to hash " + myHashGetter.apply(head));
      }
    }

    @Override
    public int compare(Integer head1, Integer head2) {
      if (head1.equals(head2)) {
        return 0;
      }

      VcsRef ref1 = myRefsModel.bestRefToHead(head1);
      VcsRef ref2 = myRefsModel.bestRefToHead(head2);
      if (ref1 == null) {
        reportNoRefs(head1);
        if (ref2 == null) {
          reportNoRefs(head2);
          return head1 - head2;
        }
        return 1;
      }
      if (ref2 == null) {
        reportNoRefs(head2);
        return -1;
      }
      if (ref1.equals(ref2)) {
        LOG.warn("Different heads " + myHashGetter.apply(head1) + " and " + myHashGetter.apply(head2) + " contain the same reference " + ref1);
      }

      VirtualFile root1 = ref1.getRoot();
      VirtualFile root2 = ref2.getRoot();
      VcsLogRefManager refManager1 = myRefManagers.get(root1);
      VcsLogRefManager refManager2 = myRefManagers.get(root2);
      if (!refManager1.equals(refManager2)) {
        return refManager1.toString().compareTo(refManager2.toString());
      }

      return refManager1.getBranchLayoutComparator().compare(ref1, ref2);
    }
  }
}
