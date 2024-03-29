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
package consulo.util.concurrent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Intended to use within try-with-resources.
 */
public class LockToken implements AutoCloseable {

  private final Lock myLock;

  private LockToken(Lock lock) {
    myLock = lock;
  }

  @Override
  public void close() {
    myLock.unlock();
  }

  @Nonnull
  public static LockToken acquireLock(@Nonnull Lock lock) {
    lock.lock();
    return new LockToken(lock);
  }

  @Nullable
  public static LockToken attemptLock(@Nonnull Lock lock, long time) throws InterruptedException {
    if (lock.tryLock(time, TimeUnit.MILLISECONDS)) {
      return new LockToken(lock);
    }
    else {
      return null;
    }
  }
}
