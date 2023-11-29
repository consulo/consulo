/*
 * Copyright 2013-2023 consulo.io
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
package consulo.application.impl.internal.concurent.locking;

import jakarta.annotation.Nonnull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author VISTALL
 * @since 2023-11-18
 */
@Deprecated
public class NewApplicationRWLock extends ReentrantReadWriteLock implements NewLock {
  private static class ReadCounter {
    private long id = Thread.currentThread().getId();
    private volatile int count;
  }

  private static class CountReadLock extends ReadLock {
    private final ThreadLocal<ReadCounter> myCounter = ThreadLocal.withInitial(ReadCounter::new);

    private CountReadLock(ReentrantReadWriteLock lock) {
      super(lock);
    }

    @Override
    public void lock() {
      super.lock();
      incCounter();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      super.lockInterruptibly();
      incCounter();
    }

    @Override
    public boolean tryLock() {
      boolean lock = super.tryLock();
      if (lock) {
        incCounter();
      }
      return lock;
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
      boolean lock = super.tryLock(timeout, unit);
      if (lock) {
        incCounter();
      }
      return lock;
    }

    @Override
    public void unlock() {
      super.unlock();
      decCounter();
    }

    private void incCounter() {
      ReadCounter counter = myCounter.get();
      counter.count++;
    }

    private void decCounter() {
      ReadCounter counter = myCounter.get();
      counter.count--;

      if (counter.count == 0) {
        myCounter.remove();
      }
    }

    public boolean isUnderReadLock() {
      ReadCounter readCounter = myCounter.get();
      return readCounter != null && readCounter.count > 0;
    }
  }

  private final CountReadLock myReadLock;

  public NewApplicationRWLock() {
    super(true);
    myReadLock = new CountReadLock(this);
  }

  @Nonnull
  @Override
  public ReadLock readLock() {
    return myReadLock;
  }

  @Override
  public boolean isReadLockedByCurrentThread() {
    return myReadLock.isUnderReadLock();
  }

  @Override
  public Lock asReadLock() {
    return readLock();
  }

  @Override
  public Lock asWriteLock() {
    return writeLock();
  }
}
