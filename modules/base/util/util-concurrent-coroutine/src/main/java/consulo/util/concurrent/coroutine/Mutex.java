/*
 * Copyright 2013-2026 consulo.io
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
package consulo.util.concurrent.coroutine;

import consulo.util.concurrent.coroutine.internal.RunLock;
import consulo.util.concurrent.coroutine.step.MutexLock;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * A non-reentrant mutual exclusion lock for coroutines. Suspended acquisitions never block a
 * thread: the coroutine is suspended and resumed when the lock is released. An optional owner
 * token identifies the holder for diagnostics and guards against unlocking from the wrong place.
 *
 * @author VISTALL
 * @since 2026-09-05
 */
public final class Mutex {
    private static final long PARK_NANOS = TimeUnit.MILLISECONDS.toNanos(50);

    private final RunLock myLock = new RunLock();

    private final Deque<Suspension<?>> myWaiters = new ConcurrentLinkedDeque<>();

    private boolean myLocked;

    private @Nullable Object myOwner;

    public boolean tryLock(@Nullable Object owner) {
        return myLock.supplyLocked(() -> {
            checkNotHeldBy(owner);
            if (myLocked) {
                return false;
            }
            myLocked = true;
            myOwner = owner;
            return true;
        });
    }

    public void unlock(@Nullable Object owner) {
        List<Suspension<?>> woken = myLock.supplyLocked(() -> {
            if (!myLocked) {
                throw new IllegalStateException("Mutex is not locked");
            }
            if (owner != null && owner != myOwner) {
                throw new IllegalStateException("Mutex is locked by " + myOwner + " but expected owner is " + owner);
            }
            myLocked = false;
            myOwner = null;

            List<Suspension<?>> waiters = new ArrayList<>();
            for (Suspension<?> waiter = myWaiters.poll(); waiter != null; waiter = myWaiters.poll()) {
                waiters.add(waiter);
            }
            return waiters;
        });

        for (Suspension<?> waiter : woken) {
            waiter.ifNotCancelled(waiter::resume);
        }
    }

    public boolean isLocked() {
        return myLock.supplyLocked(() -> myLocked);
    }

    public boolean holdsLock(Object owner) {
        return myLock.supplyLocked(() -> myLocked && myOwner == owner);
    }

    public <T extends @Nullable Object> MutexLock<T> lock(@Nullable Object owner) {
        return MutexLock.lock(this, owner);
    }

    public void lockCancellable(@Nullable Object owner, Runnable cancellationCheck) {
        while (!tryLock(owner)) {
            cancellationCheck.run();
            LockSupport.parkNanos(PARK_NANOS);
        }
    }

    public void awaitUnlock(Suspension<?> suspension) {
        boolean queued = myLock.supplyLocked(() -> {
            if (!myLocked) {
                return false;
            }
            myWaiters.add(suspension);
            return true;
        });

        if (!queued) {
            suspension.resume();
            return;
        }

        suspension.onCancel(Optional.of(() -> myWaiters.remove(suspension)));
        if (suspension.isCancelled()) {
            myWaiters.remove(suspension);
        }
    }

    @Override
    public String toString() {
        return myLock.supplyLocked(() -> {
            String state = myLocked ? "locked by " + myOwner : "unlocked";
            return String.format("%s[%s]", getClass().getSimpleName(), state);
        });
    }

    private void checkNotHeldBy(@Nullable Object owner) {
        if (owner != null && myLocked && myOwner == owner) {
            throw new IllegalStateException("Mutex is already locked by owner " + owner);
        }
    }
}
