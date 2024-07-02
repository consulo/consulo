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

package consulo.ide.impl.idea.vcs.log.graph.impl.permanent;


import consulo.versionControlSystem.log.graph.GraphCommit;
import jakarta.annotation.Nonnull;

import java.util.*;

public class DuplicateParentFixer {

  public static <CommitId> AbstractList<? extends GraphCommit<CommitId>> fixDuplicateParentCommits(final List<? extends GraphCommit<CommitId>> finalCommits) {
    return new AbstractList<>() {
      @Override
      public GraphCommit<CommitId> get(int index) {
        return fixParentsDuplicate(finalCommits.get(index));
      }

      @Override
      public int size() {
        return finalCommits.size();
      }
    };
  }

  private static class DelegateGraphCommit<CommitId> implements GraphCommit<CommitId> {
    @Nonnull
    private final GraphCommit<CommitId> myDelegate;

    @Nonnull
    private final List<CommitId> myParents;

    private DelegateGraphCommit(@Nonnull GraphCommit<CommitId> delegate, @Nonnull List<CommitId> parents) {
      myDelegate = delegate;
      myParents = parents;
    }

    @Nonnull
    @Override
    public CommitId getId() {
      return myDelegate.getId();
    }

    @Nonnull
    @Override
    public List<CommitId> getParents() {
      return myParents;
    }

    @Override
    public long getTimestamp() {
      return myDelegate.getTimestamp();
    }
  }

  @Nonnull
  private static <CommitId> GraphCommit<CommitId> fixParentsDuplicate(@Nonnull GraphCommit<CommitId> commit) {
    List<CommitId> parents = commit.getParents();
    if (parents.size() <= 1) return commit;

    if (parents.size() == 2) {
      CommitId commitId0 = parents.get(0);
      if (!commitId0.equals(parents.get(1))) {
        return commit;
      }
      else {
        return new DelegateGraphCommit<>(commit, Collections.singletonList(commitId0));
      }
    }

    Set<CommitId> allParents = new HashSet<>(parents);
    if (parents.size() == allParents.size()) return commit;

    List<CommitId> correctParents = new ArrayList<>();
    for (CommitId commitId : parents) {
      if (allParents.remove(commitId)) {
        correctParents.add(commitId);
      }
    }

    return new DelegateGraphCommit<>(commit, correctParents);
  }
}
