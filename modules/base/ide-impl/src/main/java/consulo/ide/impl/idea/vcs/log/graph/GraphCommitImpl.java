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
package consulo.ide.impl.idea.vcs.log.graph;

import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ImmutableList;
import consulo.versionControlSystem.log.graph.GraphCommit;

import jakarta.annotation.Nonnull;

import java.util.List;

public class GraphCommitImpl<CommitId> extends ImmutableList<CommitId> implements GraphCommit<CommitId> {

  @Nonnull
  private final CommitId myId;
  @Nonnull
  private final Object myParents;
  private final long myTimestamp;

  public GraphCommitImpl(@Nonnull CommitId id, @Nonnull List<CommitId> parents, long timestamp) {
    myId = id;
    myTimestamp = timestamp;
    if (parents.isEmpty()) {
      myParents = ArrayUtil.EMPTY_OBJECT_ARRAY;
    }
    else if (parents.size() == 1) {
      myParents = parents.get(0);
      assert !(myParents instanceof Object[]);
    }
    else {
      myParents = parents.toArray();
    }
  }

  @Nonnull
  @Override
  public CommitId getId() {
    return myId;
  }

  @Nonnull
  @Override
  public List<CommitId> getParents() {
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public CommitId get(int index) {
    if (myParents instanceof Object[]) {
      Object[] array = (Object[])myParents;
      if (index < 0 || index >= array.length) {
        throw new ArrayIndexOutOfBoundsException(index);
      }
      return (CommitId)array[index];
    }
    if (index != 0) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    return (CommitId)myParents;
  }

  @Override
  public int size() {
    return myParents instanceof Object[] ? ((Object[])myParents).length : 1;
  }

  @Override
  public long getTimestamp() {
    return myTimestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof GraphCommit)) return false;
    GraphCommit commit = (GraphCommit)o;
    return myId.equals(commit.getId());
  }

  @Override
  public int hashCode() {
    return myId.hashCode();
  }
}
