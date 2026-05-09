// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.application.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessToken;
import consulo.application.impl.internal.RWLock;
import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.ApplicationUtil;
import consulo.component.ProcessCanceledException;
import consulo.util.collection.ConcurrentList;
import consulo.util.collection.Lists;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>Read-Write lock optimised for mostly reads, with write-intent support for the AWT EDT model.
 * Scales better than {@link ReentrantReadWriteLock} with a number of readers due to reduced contention
 * thanks to thread local structures.
 * The lock has writer preference, i.e. no reader can obtain read lock while there is a writer pending.
 * NOT reentrant.
 * Writer assumed to issue write requests from the dedicated thread {@link #writeThread} only.
 * Readers must not issue read requests from the write thread {@link #writeThread}.</p>
 *
 * <p>Based on paper <a href="http://mcg.cs.tau.ac.il/papers/ppopp2013-rwlocks.pdf">"NUMA-Aware Reader-Writer Locks"
 * by Calciu, Dice, Lev, Luchangco, Marathe, Shavit.</a></p>
 */
public final class ReadMostlyRWLock implements RWLock {
    public volatile Thread writeThread;
    private volatile Thread writeIntendedThread;

    volatile boolean writeRequested;
    private final AtomicBoolean writeIntent = new AtomicBoolean(false);
    private volatile boolean writeAcquired;
    private final ConcurrentList<Reader> readers = Lists.newLockFreeCopyOnWriteList();

    private volatile boolean writeSuspended;
    private volatile long deadReadersGCStamp;

    public ReadMostlyRWLock(@Nullable Thread writeThread) {
        this.writeThread = writeThread;
    }

    public static class Reader implements ReadToken {
        private final Thread thread;
        public volatile boolean readRequested;
        private volatile boolean blocked;
        private boolean impatientReads;

        Reader(Thread readerThread) {
            thread = readerThread;
        }

        @Override
        public String toString() {
            return "Reader{" +
                "thread=" + thread +
                ", readRequested=" + readRequested +
                ", blocked=" + blocked +
                ", impatientReads=" + impatientReads +
                '}';
        }

        @Override
        public boolean readRequested() {
            return readRequested;
        }
    }

    private final ThreadLocal<Reader> R = ThreadLocal.withInitial(() -> {
        Reader status = new Reader(Thread.currentThread());
        boolean added = readers.addIfAbsent(status);
        assert added : readers + "; " + Thread.currentThread();
        return status;
    });

    @Override
    public boolean isWriteThread() {
        return Thread.currentThread() == writeThread;
    }

    @Override
    public boolean isReadLockedByThisThread() {
        checkReadThreadAccess();
        Reader status = R.get();
        return status.readRequested;
    }

    @Override
    public Reader startRead() {
        if (Thread.currentThread() == writeThread) {
            return null;
        }
        Reader status = R.get();
        throwIfImpatient(status);
        if (status.readRequested) {
            return null;
        }

        if (!tryReadLock(status)) {
            ProgressIndicator progress = ProgressManager.getGlobalProgressIndicator();
            for (int iter = 0; ; iter++) {
                if (tryReadLock(status)) {
                    break;
                }
                if (progress != null && progress.isCanceled() && !ProgressManager.getInstance().isInNonCancelableSection()) {
                    throw new ProcessCanceledException();
                }
                waitABit(status, iter);
            }
        }
        return status;
    }

    @Override
    public Reader startTryRead() {
        if (Thread.currentThread() == writeThread) {
            return null;
        }
        Reader status = R.get();
        throwIfImpatient(status);
        if (status.readRequested) {
            return null;
        }

        tryReadLock(status);
        return status;
    }

    @Override
    public void endRead(RWLock.ReadToken status) {
        checkReadThreadAccess();

        ((Reader) status).readRequested = false;

        if (writeRequested) {
            LockSupport.unpark(writeThread);
        }
    }

    private void waitABit(Reader status, int iteration) {
        if (iteration > SPIN_TO_WAIT_FOR_LOCK) {
            status.blocked = true;
            try {
                throwIfImpatient(status);
                LockSupport.parkNanos(this, 1_000_000);
            }
            finally {
                status.blocked = false;
            }
        }
        else {
            Thread.yield();
        }
    }

    private void throwIfImpatient(Reader status) throws ApplicationUtil.CannotRunReadActionException {
        if (status.impatientReads && writeRequested && !ProgressManager.getInstance()
            .isInNonCancelableSection() && CoreProgressManager.ENABLED) {
            throw ApplicationUtil.CannotRunReadActionException.create();
        }
    }

    @Override
    public boolean isInImpatientReader() {
        return R.get().impatientReads;
    }

    @Override
    public void executeByImpatientReader(@RequiredReadAction Runnable runnable)
        throws ApplicationUtil.CannotRunReadActionException {
        checkReadThreadAccess();
        Reader status = R.get();
        boolean old = status.impatientReads;
        try {
            status.impatientReads = true;
            runnable.run();
        }
        finally {
            status.impatientReads = old;
        }
    }

    private boolean tryReadLock(Reader status) {
        throwIfImpatient(status);
        if (!writeRequested) {
            status.readRequested = true;
            if (!writeRequested) {
                return true;
            }
            status.readRequested = false;
        }
        return false;
    }

    private static final int SPIN_TO_WAIT_FOR_LOCK = 100;

    @Override
    public void writeIntentLock() {
        writeIntendedThread = Thread.currentThread();
        for (int iter = 0; ; iter++) {
            if (writeIntent.compareAndSet(false, true)) {
                assert !writeRequested;
                assert !writeAcquired;

                writeThread = Thread.currentThread();
                break;
            }

            if (iter > SPIN_TO_WAIT_FOR_LOCK) {
                LockSupport.parkNanos(this, 1_000_000);
            }
            else {
                Thread.yield();
            }
        }
    }

    @Override
    public void writeIntentUnlock() {
        checkWriteThreadAccess();

        assert !writeAcquired;
        assert !writeRequested;

        writeThread = null;
        writeIntent.set(false);
        LockSupport.unpark(writeIntendedThread);
    }

    @Override
    public void writeLock() {
        checkWriteThreadAccess();
        assert !writeRequested;
        assert !writeAcquired;

        writeRequested = true;
        for (int iter = 0; ; iter++) {
            if (areAllReadersIdle()) {
                writeAcquired = true;
                break;
            }

            if (iter > SPIN_TO_WAIT_FOR_LOCK) {
                LockSupport.parkNanos(this, 1_000_000);
            }
            else {
                Thread.yield();
            }
        }
    }

    @Override
    public AccessToken writeSuspend() {
        boolean prev = writeSuspended;
        writeSuspended = true;
        writeUnlock();
        return new AccessToken() {
            @Override
            public void finish() {
                ProgressIndicatorUtils.cancelActionsToBeCancelledBeforeWrite();
                writeLock();
                writeSuspended = prev;
            }
        };
    }

    @Override
    public void writeUnlock() {
        checkWriteThreadAccess();
        writeAcquired = false;
        writeRequested = false;
        List<Reader> dead;
        long current = System.nanoTime();
        if (current - deadReadersGCStamp > 1_000_000) {
            dead = new ArrayList<>(readers.size());
            deadReadersGCStamp = current;
        }
        else {
            dead = null;
        }
        for (Reader reader : readers) {
            if (reader.blocked) {
                LockSupport.unpark(reader.thread);
            }
            else if (dead != null && !reader.thread.isAlive()) {
                dead.add(reader);
            }
        }
        if (dead != null) {
            readers.removeAll(dead);
        }
    }

    private void checkWriteThreadAccess() {
        if (Thread.currentThread() != writeThread) {
            throw new IllegalStateException("Current thread: " + Thread.currentThread() + "; expected: " + writeThread);
        }
    }

    private void checkReadThreadAccess() {
        if (Thread.currentThread() == writeThread) {
            throw new IllegalStateException("Must not start read from the write thread: " + Thread.currentThread());
        }
    }

    private boolean areAllReadersIdle() {
        for (Reader reader : readers) {
            if (reader.readRequested) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isWriteLocked() {
        return writeAcquired;
    }

    @Override
    public String toString() {
        return "ReadMostlyRWLock{" +
            "writeThread=" + writeThread +
            ", writeRequested=" + writeRequested +
            ", writeAcquired=" + writeAcquired +
            ", readers=" + readers +
            ", writeSuspended=" + writeSuspended +
            '}';
    }
}
