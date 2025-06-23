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
package consulo.ide.impl.idea.util.containers;

import jakarta.annotation.Nonnull;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author cdr
 */
public final class StripedReentrantLocks extends StripedLockHolder<ReentrantLock> {
  private StripedReentrantLocks() {
    super(ReentrantLock.class);
  }

  @Nonnull
  @Override
  protected ReentrantLock create() {
    return new ReentrantLock();
  }

  private static final StripedReentrantLocks INSTANCE = new StripedReentrantLocks();
  public static StripedReentrantLocks getInstance() {
    return INSTANCE;
  }

  public void lock(int index) {
    ourLocks[index].lock();
  }
  public void unlock(int index) {
    ourLocks[index].unlock();
  }
}
