/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.localHistory.impl.internal;

import consulo.util.collection.primitive.ints.IntSet;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InMemoryChangeListStorage implements ChangeListStorage {
  private int myCurrentId;
  private List<ChangeSet> mySets = new ArrayList<>();

  @Override
  public void close() {
  }

  @Override
  public long nextId() {
    return myCurrentId++;
  }

  @Override
  @Nullable
  public ChangeSetHolder readPrevious(int id, IntSet recursionGuard) {
    if (mySets.isEmpty()) return null;
    if (id == -1) return new ChangeSetHolder(mySets.size() - 1, mySets.get(mySets.size() - 1));
    return id == 0 ? null : new ChangeSetHolder(id -1, mySets.get(id - 1));
  }

  @Override
  public void writeNextSet(ChangeSet changeSet) {
    mySets.add(changeSet);
  }

  @Override
  public void purge(long period, int intervalBetweenActivities, Consumer<ChangeSet> processor) {
  }
}
