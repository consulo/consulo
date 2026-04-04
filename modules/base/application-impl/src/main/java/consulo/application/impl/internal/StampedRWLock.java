/*
 * Copyright 2013-2025 consulo.io
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
package consulo.application.impl.internal;

import consulo.application.util.ApplicationUtil;
import consulo.ui.UIAccess;

import java.util.concurrent.locks.StampedLock;

/**
 * Application read-write lock based on {@link StampedLock}.
 * <p>
 * Write lock can be acquired from any thread. Only one writer at a time.
 * Multiple readers can run concurrently. Writer has preference (readers block when writer is waiting).
 * <p>
 * The write lock holder gets implicit read access (no need to acquire read lock separately).
 *
 * @author VISTALL
 * @since 2025-03-10
 */
public final class StampedRWLock implements RWLock {
    public record StampedReadToken(long stamp) implements ReadToken {
        static final StampedReadToken FAILED = new StampedReadToken(0);

        @Override
        public boolean readRequested() {
            return stamp != 0;
        }
    }

    private final StampedLock myLock = new StampedLock();

    private volatile Thread myWriteThread;
    private volatile long myWriteStamp;

    private final ThreadLocal<Long> myReadStamp = new ThreadLocal<>();
    private final ThreadLocal<Boolean> myImpatientReads = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // ── Write ──

    @Override
    public void writeLock() {
        long stamp = myLock.writeLock();
        myWriteStamp = stamp;
        myWriteThread = Thread.currentThread();
    }

    @Override
    public void writeUnlock() {
        long stamp = myWriteStamp;
        myWriteThread = null;
        myWriteStamp = 0;
        myLock.unlockWrite(stamp);
    }


    // ── Read ──

    @Override
    public ReadToken startRead() {
        // write holder gets implicit read access
        if (Thread.currentThread() == myWriteThread) {
            return null;
        }

        // already in read action (nested)
        if (myReadStamp.get() != null) {
            return null;
        }

        throwIfImpatient();

        // UI thread must never block waiting for a write lock to be released — that would freeze the UI.
        // Use tryRunReadAction() or move the read action to a background thread.
        if (UIAccess.isUIThread() && myLock.isWriteLocked()) {
            throw new IllegalStateException("Can't run ReadAction from UI thread while write lock is active — would freeze UI");
        }

        long stamp = myLock.readLock();
        myReadStamp.set(stamp);
        return new StampedReadToken(stamp);
    }

    @Override
    public ReadToken startTryRead() {
        // write holder gets implicit read access
        if (Thread.currentThread() == myWriteThread) {
            return null;
        }

        // already in read action (nested)
        if (myReadStamp.get() != null) {
            return null;
        }

        throwIfImpatient();

        long stamp = myLock.tryReadLock();
        if (stamp == 0) {
            return StampedReadToken.FAILED;
        }

        myReadStamp.set(stamp);
        return new StampedReadToken(stamp);
    }

    @Override
    public void endRead(ReadToken read) {
        long stamp = ((StampedReadToken) read).stamp();
        myReadStamp.remove();
        myLock.unlockRead(stamp);
    }

    // ── Impatient Reader ──

    @Override
    public void executeByImpatientReader(Runnable runnable) throws ApplicationUtil.CannotRunReadActionException {
        boolean old = myImpatientReads.get();
        try {
            myImpatientReads.set(true);
            runnable.run();
        }
        finally {
            myImpatientReads.set(old);
        }
    }

    @Override
    public boolean isInImpatientReader() {
        return myImpatientReads.get();
    }

    private void throwIfImpatient() throws ApplicationUtil.CannotRunReadActionException {
        if (myImpatientReads.get() && myLock.isWriteLocked()) {
            throw ApplicationUtil.CannotRunReadActionException.create();
        }
    }

    // ── Queries ──

    @Override
    public boolean isWriteThread() {
        return Thread.currentThread() == myWriteThread;
    }

    @Override
    public boolean isReadLockedByThisThread() {
        return myReadStamp.get() != null;
    }

    @Override
    public boolean isWriteLocked() {
        return myLock.isWriteLocked();
    }

    @Override
    public String toString() {
        return "StampedRWLock{" +
            "writeThread=" + myWriteThread +
            ", writeLocked=" + myLock.isWriteLocked() +
            '}';
    }
}
