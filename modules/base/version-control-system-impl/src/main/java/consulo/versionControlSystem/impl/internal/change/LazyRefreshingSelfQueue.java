/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.logging.Logger;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * need to update:
 * 1. if TS is null
 * 2. if checker returns TRUE -> those whose timestamp is older than required
 *
 */
// TODO: Used only in RemoteRevisionsNumberCache
public class LazyRefreshingSelfQueue<T> {
  private static final Logger LOG = Logger.getInstance(LazyRefreshingSelfQueue.class);

  // provides update interval in milliseconds.
  private final Supplier<Long> myUpdateInterval;
  // structure:
  // 1) pairs with First == null
  // 2) pairs with First != null sorted by First ascending
  // pair.First - time when T was last processed
  // pair.Second - some item T
  private final LinkedList<Pair<Long, T>> myQueue;
  // Set of items that should be processed by myUpdater
  private final Set<T> myInProgress;
  // checks if updateStep should be really performed
  private final Supplier<Boolean> myShouldUpdateOldChecker;
  // performs some actions on item T, for instance - updates some data for T in cache
  private final Consumer<T> myUpdater;
  private final Object myLock;

  public LazyRefreshingSelfQueue(final Supplier<Long> updateInterval, final Supplier<Boolean> shouldUpdateOldChecker, final Consumer<T> updater) {
    myUpdateInterval = updateInterval;
    myShouldUpdateOldChecker = shouldUpdateOldChecker;
    myUpdater = updater;
    myQueue = new LinkedList<>();
    myInProgress = new HashSet<>();
    myLock = new Object();
  }

  // adds item that should be updated at next updateStep() call
  public void addRequest(@Nonnull final T t) {
    synchronized (myLock) {
      myQueue.addFirst(new Pair<>(null, t));
    }
  }

  // unschedules item from update at next updateStep() call
  public void forceRemove(@Nonnull final T t) {
    synchronized (myLock) {
      for (Iterator<Pair<Long, T>> iterator = myQueue.iterator(); iterator.hasNext();) {
        final Pair<Long, T> pair = iterator.next();
        if (t.equals(pair.getSecond())) {
          iterator.remove();
        }
      }
      myInProgress.remove(t);
    }
  }

  // called by outside timer or something
  public void updateStep() {
    final long startTime = System.currentTimeMillis() - myUpdateInterval.get();
    boolean onlyAbsolute = true;
    // TODO: Actually we could store items with pair.First == null in separate list.
    // checks item that has smallest update time - i.e. was not updated by the most time
    // if its update time greater than current - interval => we should not update any item with pair.First != null this time (as they are ordered)
    synchronized (myLock) {
      for (Pair<Long, T> pair : myQueue) {
        if (pair.getFirst() != null) {
          onlyAbsolute = pair.getFirst() > startTime;
          break;
        }
      }
    }

    // do not ask under lock
    final Boolean shouldUpdateOld = onlyAbsolute ? false : myShouldUpdateOldChecker.get();
    final List<T> dirty = new LinkedList<>();

    synchronized (myLock) {
      // adds all pairs with pair.First == null to dirty
      while (! myQueue.isEmpty()) {
        final Pair<Long, T> pair = myQueue.get(0);
        if (pair.getFirst() == null) {
          dirty.add(myQueue.removeFirst().getSecond());
        } else {
          break;
        }
      }
      if (Boolean.TRUE.equals(shouldUpdateOld) && (! myQueue.isEmpty())) {
        // adds all pairs with update time (pair.First) < current - interval to dirty
        while (! myQueue.isEmpty()) {
          final Pair<Long, T> pair = myQueue.get(0);
          if (pair.getFirst() < startTime) {
            myQueue.removeFirst();
            dirty.add(pair.getSecond());
          } else {
            break;
          }
        }
      }

      myInProgress.addAll(dirty);
    }

    LOG.debug("found something to update: " + (! dirty.isEmpty()));
    for (T t : dirty) {
      myUpdater.accept(t);
      synchronized (myLock) {
        // output value of remove() is tracked not to process items that were removed from myInProgress in forceRemove()
        // TODO: Probably more clear logic should be implemented
        if (myInProgress.remove(t)) {
          myQueue.addLast(new Pair<>(System.currentTimeMillis(), t));
        }
      }
    }
  }
}
