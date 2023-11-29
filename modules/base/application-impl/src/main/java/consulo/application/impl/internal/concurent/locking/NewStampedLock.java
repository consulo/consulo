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

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.LongSupplier;

/**
 * @author VISTALL
 * @since 2023-11-26
 */
public class NewStampedLock extends StampedLock implements NewLock {
  private class LockWrapper implements Lock {
    private final LongSupplier myLockFunc;
    private final LongSupplier myTryLockFunc;

    private LockWrapper(LongSupplier lockFunc, LongSupplier tryLockFunc) {
      myLockFunc = lockFunc;
      myTryLockFunc = tryLockFunc;
    }

    @Override
    public void lock() {
      long stamp = myLockFunc.getAsLong();
      
      myStampHolder.set(stamp);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
      long stamp = myTryLockFunc.getAsLong();
      if (stamp == 0) {
        return false;
      }
      
      myStampHolder.set(stamp);
      return true;
    }

    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unlock() {
      Long stamp = myStampHolder.get();
      if (stamp != null) {
        myStampHolder.remove();
        NewStampedLock.this.unlock(stamp);
      }
    }

    @Nonnull
    @Override
    public Condition newCondition() {
      throw new UnsupportedOperationException();
    }
  }

  private final ThreadLocal<Long> myStampHolder = ThreadLocal.withInitial(() -> null);

  private final Lock myReadLock;
  private final Lock myWriteLock;

  public NewStampedLock() {
    myReadLock = new LockWrapper(this::readLock, this::tryReadLock);
    myWriteLock = new LockWrapper(this::writeLock, this::tryWriteLock);
  }

  @Override
  public boolean isReadLockedByCurrentThread() {
    Long stamp = myStampHolder.get();
    return stamp != null && StampedLock.isReadLockStamp(stamp);
  }

  @Override
  public boolean isWriteLockedByCurrentThread() {
    Long stamp = myStampHolder.get();
    return stamp != null && StampedLock.isWriteLockStamp(stamp);
  }

  @Override
  public Lock asReadLock() {
    return myReadLock;
  }

  @Override
  public Lock asWriteLock() {
    return myWriteLock;
  }
}
