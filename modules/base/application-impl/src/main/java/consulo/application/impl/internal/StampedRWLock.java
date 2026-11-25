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

import consulo.application.AccessToken;
import consulo.application.util.ApplicationUtil;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Application read-write lock based on {@link StampedLock}.
 * <p>
 * Read is compatible with read and with write-intent, write excludes everything. Write-intent is exclusive among writers and
 * is upgraded to write by its holder. The write-intent holder (and a thread a write action is transferred to) gets implicit read
 * access. Stamps have no owner, so a write action can be transferred to another thread.
 * <p>
 * Readers are spread over a stripe of locks, one per processor, picked by the thread, so that parallel readers don't contend
 * for one lock word; the writer locks every stripe.
 * <p>
 * A requested write has priority: from {@link #requestWrite()} until {@link #writeUnlock()} no new read lock is granted, readers
 * wait for the write to finish. A thread waiting for a read lock or for write-intent runs a write action transferred to it.
 * <p>
 * A reader switches to write-intent without letting a write action in between only when write-intent is free at that moment
 * ({@link #tryUpgradeToWriteIntent()}) - waiting for it inside a read would deadlock with the write-intent holder waiting for
 * the readers.
 * <p>
 * Input on a UI thread has priority over background writers: while input keeps coming ({@link #tryStartInput(long)} renews a
 * lease) or queued input has not run yet ({@link #queueInput}), threads other than UI threads don't take write-intent.
 *
 * @author VISTALL
 * @since 2025-03-10
 */
public final class StampedRWLock implements RWLock {
    private static final Logger LOG = Logger.getInstance(StampedRWLock.class);

    private static final ReadToken FAILED = () -> false;

    private static final long WAIT_MILLIS = 1;

    private static final int MAX_STRIPES = 64;

    private static final long MAX_INPUT_WAIT_NANOS = TimeUnit.SECONDS.toNanos(30);

    private final StampedLockStripe[] myStripes;
    private final long[] myWriteStamps;
    private final Semaphore myWriteIntent = new Semaphore(1, true);
    private final StampedRWLockEnvironment myEnvironment;
    private final WriteActionTransfer myTransfer;

    private volatile @Nullable Thread myWriteThread;
    private volatile @Nullable CompletableFuture<Void> myWriteGate;
    private volatile boolean myWriteLocked;
    private int myWriteIntentHolds;

    private final AtomicReference<@Nullable CompletableFuture<Void>> myWriteIntentFree = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<?>> myInputQueue = new AtomicReference<>(CompletableFuture.completedFuture(null));
    private volatile long myInputQueuedUntil;
    private volatile long myInputLeaseUntil = System.nanoTime();
    private final AtomicInteger myInputPrioritySuspended = new AtomicInteger();

    private final ThreadLocal<StampedReaderState> myReader = ThreadLocal.withInitial(StampedReaderState::new);
    private final ThreadLocal<Boolean> myImpatientReads = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private final ThreadLocal<Boolean> myAheadOfInput = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public StampedRWLock() {
        this(StampedRWLockEnvironment.DEFAULT);
    }

    StampedRWLock(StampedRWLockEnvironment environment) {
        myEnvironment = environment;
        myTransfer = new WriteActionTransfer(thread -> myWriteThread = thread, environment::isUIThread);

        int processors = Runtime.getRuntime().availableProcessors();
        int stripes = Math.min(MAX_STRIPES, Integer.highestOneBit(Math.max(1, processors - 1)) << 1);
        myStripes = new StampedLockStripe[stripes];
        for (int i = 0; i < stripes; i++) {
            myStripes[i] = new StampedLockStripe();
        }
        myWriteStamps = new long[stripes];
    }

    @Override
    public @Nullable ReadToken startRead() {
        if (isWriteThread()) {
            return null;
        }
        StampedReaderState reader = myReader.get();
        if (reader.myStamp != 0) {
            return null;
        }

        throwIfImpatient();

        boolean interrupted = false;
        try {
            while (true) {
                CompletableFuture<Void> gate = myWriteGate;
                if (gate == null) {
                    if (tryAcquireRead(reader)) {
                        return reader;
                    }
                    Thread.onSpinWait();
                    continue;
                }

                myTransfer.poll();
                if (myEnvironment.isReadWaitCancelled()) {
                    throw new ProcessCanceledException();
                }
                throwIfImpatient();

                try {
                    gate.get(WAIT_MILLIS, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e) {
                    interrupted = true;
                }
                catch (TimeoutException | ExecutionException ignored) {
                }
            }
        }
        finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public @Nullable ReadToken startTryRead() {
        if (isWriteThread()) {
            return null;
        }
        StampedReaderState reader = myReader.get();
        if (reader.myStamp != 0) {
            return null;
        }

        throwIfImpatient();

        if (myWriteGate != null || !tryAcquireRead(reader)) {
            return FAILED;
        }
        return reader;
    }

    private boolean tryAcquireRead(StampedReaderState reader) {
        int stripe = (int) Thread.currentThread().threadId() & (myStripes.length - 1);
        StampedLock lock = myStripes[stripe];
        long stamp = lock.tryReadLock();
        if (stamp == 0) {
            return false;
        }

        if (myWriteGate != null) {
            lock.unlockRead(stamp);
            return false;
        }

        reader.myStripe = stripe;
        reader.myStamp = stamp;
        return true;
    }

    @Override
    public void endRead(ReadToken read) {
        StampedReaderState reader = (StampedReaderState) read;
        long stamp = reader.myStamp;
        reader.myStamp = 0;
        myStripes[reader.myStripe].unlockRead(stamp);
    }

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
        if (myImpatientReads.get() && myWriteGate != null && myEnvironment.isImpatienceAllowed()) {
            throw ApplicationUtil.CannotRunReadActionException.create();
        }
    }

    @Override
    public void writeIntentLock() {
        Thread current = Thread.currentThread();
        if (current == myWriteThread) {
            myWriteIntentHolds++;
            return;
        }

        checkNoReadLock("Write-intent");

        boolean yieldsToInput = yieldsToInput();
        boolean interrupted = false;
        while (true) {
            if (yieldsToInput && isInputPriority()) {
                myTransfer.poll();
                LockSupport.parkNanos(this, TimeUnit.MILLISECONDS.toNanos(WAIT_MILLIS));
                continue;
            }
            try {
                if (myWriteIntent.tryAcquire(WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                    if (!yieldsToInput || !isInputPriority()) {
                        break;
                    }
                    myWriteIntent.release();
                }
            }
            catch (InterruptedException e) {
                interrupted = true;
            }
            myTransfer.poll();
        }

        myWriteThread = current;
        myWriteIntentHolds = 1;

        if (interrupted) {
            current.interrupt();
        }
    }

    @Override
    public void writeIntentUnlock() {
        checkWriteThread();

        if (--myWriteIntentHolds > 0) {
            return;
        }

        boolean leakedWrite = myWriteLocked;
        if (leakedWrite) {
            writeUnlock();
        }

        myWriteThread = null;
        myWriteIntent.release();

        CompletableFuture<Void> free = myWriteIntentFree.getAndSet(null);
        if (free != null) {
            free.complete(null);
        }

        if (leakedWrite) {
            LOG.error(new IllegalStateException("Write-intent released while the write lock is held: " + Thread.currentThread()));
        }
    }

    @Override
    public boolean tryUpgradeToWriteIntent() {
        Thread current = Thread.currentThread();
        if (current == myWriteThread || myReader.get().myStamp == 0) {
            return false;
        }

        boolean yieldsToInput = yieldsToInput();
        if (yieldsToInput && isInputPriority() || !myWriteIntent.tryAcquire()) {
            return false;
        }
        if (yieldsToInput && isInputPriority()) {
            myWriteIntent.release();
            return false;
        }

        myWriteThread = current;
        myWriteIntentHolds = 1;
        return true;
    }

    @Override
    public <T extends @Nullable Object> T computeAheadOfInput(Supplier<T> action) {
        Boolean old = myAheadOfInput.get();
        myAheadOfInput.set(Boolean.TRUE);
        try {
            return action.get();
        }
        finally {
            myAheadOfInput.set(old);
        }
    }

    private boolean yieldsToInput() {
        return !myEnvironment.isUIThread() && !myAheadOfInput.get();
    }

    @Override
    public boolean tryStartInput(long leaseNanos) {
        Thread current = Thread.currentThread();
        if (current == myWriteThread) {
            myWriteIntentHolds++;
            renewInputLease(leaseNanos);
            return true;
        }

        if (myReader.get().myStamp != 0 || !myWriteIntent.tryAcquire()) {
            return false;
        }

        myWriteThread = current;
        myWriteIntentHolds = 1;
        renewInputLease(leaseNanos);
        return true;
    }

    @Override
    public void renewInputLease(long leaseNanos) {
        long until = System.nanoTime() + leaseNanos;
        if (until - myInputLeaseUntil > 0) {
            myInputLeaseUntil = until;
        }
    }

    @Override
    public CompletableFuture<?> whenWriteIntentFree() {
        while (true) {
            CompletableFuture<Void> free = myWriteIntentFree.get();
            if (free == null) {
                free = new CompletableFuture<>();
                if (!myWriteIntentFree.compareAndSet(null, free)) {
                    continue;
                }
            }
            if (myWriteThread == null && myWriteIntentFree.compareAndSet(free, null)) {
                free.complete(null);
            }
            return free;
        }
    }

    @Override
    public CompletableFuture<?> queueInput(CompletableFuture<?> input) {
        myInputQueuedUntil = System.nanoTime() + MAX_INPUT_WAIT_NANOS;
        return myInputQueue.getAndSet(input);
    }

    @Override
    public boolean isInputQueued() {
        return !myInputQueue.get().isDone();
    }

    @Override
    public void suspendInputPriority() {
        myInputPrioritySuspended.incrementAndGet();
    }

    @Override
    public void resumeInputPriority() {
        myInputPrioritySuspended.decrementAndGet();
    }

    boolean isInputPriority() {
        if (myInputPrioritySuspended.get() > 0) {
            return false;
        }
        long now = System.nanoTime();
        return myInputLeaseUntil - now > 0 || !myInputQueue.get().isDone() && myInputQueuedUntil - now > 0;
    }

    @Override
    public void requestWrite() {
        checkWriteThread();
        checkNoReadLock("Write");

        if (myWriteLocked) {
            throw new IllegalStateException("Write lock is already held: " + Thread.currentThread());
        }

        if (myWriteGate == null) {
            myWriteGate = new CompletableFuture<>();
        }
    }

    @Override
    public void cancelWriteRequest() {
        checkWriteThread();

        if (!myWriteLocked) {
            releaseWriteGate();
        }
    }

    @Override
    public void writeLock() {
        requestWrite();
        lockStripes();
    }

    @Override
    public void writeUnlock() {
        checkWriteThread();

        if (!myWriteLocked) {
            throw new IllegalStateException("Write lock is not held: " + Thread.currentThread());
        }

        unlockStripes();
        releaseWriteGate();
    }

    private void lockStripes() {
        for (int i = 0; i < myStripes.length; i++) {
            myWriteStamps[i] = myStripes[i].writeLock();
        }
        myWriteLocked = true;
    }

    private void unlockStripes() {
        myWriteLocked = false;
        for (int i = myStripes.length - 1; i >= 0; i--) {
            long stamp = myWriteStamps[i];
            myWriteStamps[i] = 0;
            myStripes[i].unlockWrite(stamp);
        }
    }

    @Override
    public AccessToken writeSuspend() {
        writeUnlock();

        return new AccessToken() {
            @Override
            public void finish() {
                checkWriteThread();

                myWriteGate = new CompletableFuture<>();
                try {
                    myEnvironment.beforeWriteRelock();
                }
                finally {
                    lockStripes();
                }
            }
        };
    }

    private void releaseWriteGate() {
        CompletableFuture<Void> gate = myWriteGate;
        myWriteGate = null;
        if (gate != null) {
            gate.complete(null);
        }
    }

    @Override
    public void transferWriteAction(boolean runOnEdt, Runnable action, Consumer<Runnable> scheduleTarget) {
        Thread source = Thread.currentThread();
        if (source != myWriteThread) {
            throw new IllegalStateException(
                "Write action can be transferred only from the write thread: " + source + "; current write thread: " + myWriteThread
            );
        }

        myTransfer.transfer(runOnEdt, action, scheduleTarget);
    }

    @Override
    public void pollTransferredWriteAction() {
        myTransfer.poll();
    }

    private void checkWriteThread() {
        Thread current = Thread.currentThread();
        if (current != myWriteThread) {
            throw new IllegalStateException("Current thread: " + current + "; write thread: " + myWriteThread);
        }
    }

    private void checkNoReadLock(String lock) {
        if (myReader.get().myStamp != 0) {
            throw new IllegalStateException(lock + " lock can't be acquired inside a read action: " + Thread.currentThread());
        }
    }

    @Override
    public boolean isWriteThread() {
        return Thread.currentThread() == myWriteThread;
    }

    @Override
    public @Nullable Thread getWriteThread() {
        return myWriteThread;
    }

    @Override
    public boolean isReadLockedByThisThread() {
        return myReader.get().myStamp != 0;
    }

    @Override
    public boolean isWriteLocked() {
        return myWriteLocked;
    }

    @Override
    public boolean isWriteRequested() {
        return myWriteGate != null;
    }

    @Override
    public @Nullable CompletionStage<?> whenWriteFinished() {
        CompletableFuture<Void> gate = myWriteGate;
        return gate == null ? null : gate.minimalCompletionStage();
    }

    @Override
    public String toString() {
        return "StampedRWLock{" +
            "writeThread=" + myWriteThread +
            ", writeLocked=" + myWriteLocked +
            ", writeRequested=" + (myWriteGate != null) +
            '}';
    }
}
