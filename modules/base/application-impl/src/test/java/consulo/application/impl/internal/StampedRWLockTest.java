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
package consulo.application.impl.internal;

import consulo.application.AccessToken;
import consulo.application.util.ApplicationUtil;
import consulo.component.ProcessCanceledException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author VISTALL
 * @since 2026-11-14
 */
@Timeout(value = 60)
public class StampedRWLockTest {
    private static final long TIMEOUT_SECONDS = 10;

    private final AtomicBoolean myReadWaitCancelled = new AtomicBoolean();
    private final AtomicInteger myRelocks = new AtomicInteger();
    private volatile @Nullable Thread myUIThread;

    private final StampedRWLock myLock = new StampedRWLock(new StampedRWLockEnvironment() {
        @Override
        public boolean isReadWaitCancelled() {
            return myReadWaitCancelled.get();
        }

        @Override
        public boolean isImpatienceAllowed() {
            return true;
        }

        @Override
        public boolean isUIThread() {
            return Thread.currentThread() == myUIThread;
        }

        @Override
        public void beforeWriteRelock() {
            myRelocks.incrementAndGet();
        }
    });

    private final ExecutorService myWriter = Executors.newSingleThreadExecutor();
    private final ExecutorService myThreads = Executors.newCachedThreadPool();
    private final ExecutorService myUI = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Test UI Thread");
        myUIThread = thread;
        return thread;
    });

    @AfterEach
    public void tearDown() {
        myWriter.shutdownNow();
        myThreads.shutdownNow();
        myUI.shutdownNow();
    }

    @Test
    public void inputTakesWriteIntentOnlyWhenFree() throws Exception {
        get(myUI.submit(() -> {
            assertThat(myLock.tryStartInput(0)).isTrue();
            assertThat(myLock.isWriteThread()).isTrue();
            myLock.writeIntentUnlock();
        }));

        onWriter(myLock::writeIntentLock);
        get(myUI.submit(() -> assertThat(myLock.tryStartInput(0)).isFalse()));
        onWriter(myLock::writeIntentUnlock);

        get(myUI.submit(() -> {
            assertThat(myLock.tryStartInput(0)).isTrue();
            myLock.writeIntentUnlock();
        }));
    }

    @Test
    public void inputLeaseHoldsBackgroundWritersBack() throws Exception {
        long lease = TimeUnit.MILLISECONDS.toNanos(600);
        get(myUI.submit(() -> {
            assertThat(myLock.tryStartInput(lease)).isTrue();
            myLock.writeIntentUnlock();
        }));

        Future<?> background = myWriter.submit(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        });
        assertBlocked(background);
        assertThat(myLock.isInputPriority()).isTrue();

        get(background);
        assertThat(myLock.isInputPriority()).isFalse();
    }

    @Test
    public void waitingInputGetsWriteIntentBeforeBackgroundWriters() throws Exception {
        onWriter(myLock::writeIntentLock);

        CompletableFuture<Object> input = new CompletableFuture<>();
        CompletableFuture<?> free = myUI.submit(() -> {
            myLock.queueInput(input);
            return myLock.whenWriteIntentFree();
        }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(free).isNotDone();

        Future<?> secondWriter = async(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        });
        assertBlocked(secondWriter);

        onWriter(myLock::writeIntentUnlock);
        assertThat(free).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertBlocked(secondWriter);

        get(myUI.submit(() -> {
            assertThat(myLock.tryStartInput(0)).isTrue();
            myLock.writeIntentUnlock();
            input.complete(null);
        }));
        get(secondWriter);
    }

    @Test
    public void failedInputNoLongerHoldsBackgroundWritersBack() throws Exception {
        CompletableFuture<Object> first = new CompletableFuture<>();
        CompletableFuture<Object> second = new CompletableFuture<>();
        assertThat(myLock.queueInput(first)).isDone();
        assertThat(myLock.queueInput(second)).isSameAs(first);
        assertThat(myLock.isInputPriority()).isTrue();

        Future<?> background = myWriter.submit(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        });
        assertBlocked(background);

        first.completeExceptionally(new IllegalStateException("input failed"));
        assertBlocked(background);

        second.completeExceptionally(new IllegalStateException("input failed"));
        get(background);
        assertThat(myLock.isInputPriority()).isFalse();
    }

    @Test
    public void suspendedInputPriorityLetsBackgroundWritersIn() throws Exception {
        get(myUI.submit(() -> {
            assertThat(myLock.tryStartInput(TimeUnit.SECONDS.toNanos(30))).isTrue();
            myLock.writeIntentUnlock();
        }));

        Future<?> background = myWriter.submit(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        });
        assertBlocked(background);

        myLock.suspendInputPriority();
        try {
            get(background);
        }
        finally {
            myLock.resumeInputPriority();
        }
        assertThat(myLock.isInputPriority()).isTrue();
    }

    @Test
    public void uiThreadWritesAreNotHeldBackByInputPriority() throws Exception {
        get(myUI.submit(() -> {
            assertThat(myLock.tryStartInput(TimeUnit.SECONDS.toNanos(30))).isTrue();
            myLock.writeIntentUnlock();

            myLock.writeIntentLock();
            myLock.writeLock();
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        }));
    }

    @Test
    public void readerUpgradesToWriteIntentWhenFree() throws Exception {
        CountDownLatch upgraded = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> upgrader = async(() -> {
            RWLock.ReadToken token = Objects.requireNonNull(myLock.startRead());
            try {
                assertThat(myLock.tryUpgradeToWriteIntent()).isTrue();
                assertThat(myLock.isWriteThread()).isTrue();
            }
            finally {
                myLock.endRead(token);
            }
            assertThat(myLock.isReadLockedByThisThread()).isFalse();
            upgraded.countDown();
            await(release);
            myLock.writeLock();
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        });
        await(upgraded);

        AtomicBoolean readerRan = new AtomicBoolean();
        get(async(() -> read(() -> readerRan.set(true))));
        assertThat(readerRan).isTrue();

        Future<?> writer = myWriter.submit(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        });
        assertBlocked(writer);

        release.countDown();
        get(upgrader);
        get(writer);
    }

    @Test
    public void readerUpgradeFailsWhileAnotherThreadHoldsWriteIntent() throws Exception {
        onWriter(myLock::writeIntentLock);
        read(() -> {
            assertThat(myLock.tryUpgradeToWriteIntent()).isFalse();
            assertThat(myLock.isWriteThread()).isFalse();
        });
        onWriter(myLock::writeIntentUnlock);
    }

    @Test
    public void readerUpgradeDoesNotWaitForWriterWaitingForIt() throws Exception {
        CountDownLatch inRead = new CountDownLatch(1);
        Future<?> reader = myThreads.submit(() -> {
            RWLock.ReadToken token = Objects.requireNonNull(myLock.startRead());
            try {
                inRead.countDown();
                waitUntil(myLock::isWriteRequested);
                assertThat(myLock.tryUpgradeToWriteIntent()).isFalse();
            }
            finally {
                myLock.endRead(token);
            }
            return null;
        });
        await(inRead);

        Future<?> writer = myWriter.submit(() -> {
            myLock.writeIntentLock();
            myLock.writeLock();
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        });
        get(reader);
        get(writer);
    }

    @Test
    public void readerUpgradeYieldsToInput() throws Exception {
        get(myUI.submit(() -> {
            assertThat(myLock.tryStartInput(TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS))).isTrue();
            myLock.writeIntentUnlock();
        }));
        read(() -> assertThat(myLock.tryUpgradeToWriteIntent()).isFalse());
        assertThat(myLock.isWriteThread()).isFalse();
    }

    @Test
    public void upgradeNeedsReadLock() {
        assertThat(myLock.tryUpgradeToWriteIntent()).isFalse();
        assertThat(myLock.isWriteThread()).isFalse();
    }

    @Test
    public void readersRunConcurrently() throws Exception {
        CountDownLatch bothInside = new CountDownLatch(2);
        Runnable reader = () -> read(() -> {
            bothInside.countDown();
            await(bothInside);
        });

        Future<?> first = async(reader);
        Future<?> second = async(reader);

        get(first);
        get(second);
    }

    @Test
    public void writeIntentHolderReadsImplicitly() throws Exception {
        onWriter(() -> {
            myLock.writeIntentLock();
            try {
                assertThat(myLock.isWriteThread()).isTrue();
                assertThat(myLock.isWriteLocked()).isFalse();
                assertThat(myLock.startRead()).isNull();
                assertThat(myLock.startTryRead()).isNull();
                assertThat(myLock.isReadLockedByThisThread()).isFalse();
            }
            finally {
                myLock.writeIntentUnlock();
            }
            assertThat(myLock.isWriteThread()).isFalse();
        });
    }

    @Test
    public void writeIntentDoesNotBlockReaders() throws Exception {
        onWriter(myLock::writeIntentLock);

        get(async(() -> {
            RWLock.ReadToken token = Objects.requireNonNull(myLock.startTryRead());
            assertThat(token.readRequested()).isTrue();
            myLock.endRead(token);

            read(() -> assertThat(myLock.isReadLockedByThisThread()).isTrue());
        }));

        onWriter(myLock::writeIntentUnlock);
    }

    @Test
    public void writeIntentIsExclusive() throws Exception {
        onWriter(myLock::writeIntentLock);

        Future<?> second = async(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        });
        assertBlocked(second);

        onWriter(myLock::writeIntentUnlock);
        get(second);
    }

    @Test
    public void writeIntentIsReentrant() throws Exception {
        onWriter(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
            assertThat(myLock.isWriteThread()).isTrue();

            myLock.writeLock();
            myLock.writeUnlock();

            myLock.writeIntentUnlock();
            assertThat(myLock.isWriteThread()).isFalse();
        });

        get(async(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        }));
    }

    @Test
    public void requestedWriteStopsNewReaders() throws Exception {
        CountDownLatch readerInside = new CountDownLatch(1);
        CountDownLatch releaseReader = new CountDownLatch(1);
        Future<?> firstReader = async(() -> read(() -> {
            readerInside.countDown();
            await(releaseReader);
        }));
        await(readerInside);

        onWriter(myLock::writeIntentLock);
        Future<?> write = myWriter.submit(myLock::writeLock);
        waitUntil(myLock::isWriteRequested);
        assertBlocked(write);

        get(async(() -> assertThat(Objects.requireNonNull(myLock.startTryRead()).readRequested()).isFalse()));

        Future<?> secondReader = async(() -> read(() -> assertThat(myLock.isWriteLocked()).isFalse()));
        assertBlocked(secondReader);

        releaseReader.countDown();
        get(firstReader);
        get(write);
        assertThat(myLock.isWriteLocked()).isTrue();
        assertBlocked(secondReader);

        onWriter(() -> {
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        });
        get(secondReader);
    }

    @Test
    public void requestedWriteHoldsBackNewReadersUntilCancelled() throws Exception {
        onWriter(() -> {
            myLock.writeIntentLock();
            myLock.requestWrite();
        });
        assertThat(myLock.isWriteRequested()).isTrue();
        assertThat(myLock.isWriteLocked()).isFalse();

        get(async(() -> assertThat(Objects.requireNonNull(myLock.startTryRead()).readRequested()).isFalse()));
        Future<?> reader = async(() -> read(() -> {
        }));
        assertBlocked(reader);

        onWriter(myLock::cancelWriteRequest);
        get(reader);
        assertThat(myLock.isWriteRequested()).isFalse();
        onWriter(myLock::writeIntentUnlock);
    }

    @Test
    public void writeFinishedIsSignalledOnUnlockAndOnSuspend() throws Exception {
        assertThat(myLock.whenWriteFinished()).isNull();

        onWriter(() -> {
            myLock.writeIntentLock();
            myLock.requestWrite();
        });
        CompletableFuture<?> requested = Objects.requireNonNull(myLock.whenWriteFinished()).toCompletableFuture();
        onWriter(myLock::writeLock);
        assertThat(requested).isNotDone();

        AtomicReference<@Nullable AccessToken> suspension = new AtomicReference<>();
        onWriter(() -> suspension.set(myLock.writeSuspend()));
        assertThat(requested).isDone();
        assertThat(myLock.whenWriteFinished()).isNull();

        onWriter(() -> Objects.requireNonNull(suspension.get()).finish());
        CompletableFuture<?> relocked = Objects.requireNonNull(myLock.whenWriteFinished()).toCompletableFuture();
        assertThat(relocked).isNotDone();

        onWriter(() -> {
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        });
        assertThat(relocked).isDone();
    }

    @Test
    public void writeIntentInsideReadIsRejected() throws Exception {
        get(async(() -> read(() -> assertThatThrownBy(myLock::writeIntentLock).isInstanceOf(IllegalStateException.class))));

        get(async(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        }));
    }

    @Test
    public void suspendedWriteLetsReadersIn() throws Exception {
        onWriter(() -> {
            myLock.writeIntentLock();
            myLock.writeLock();
        });

        AtomicReference<@Nullable AccessToken> suspension = new AtomicReference<>();
        onWriter(() -> suspension.set(myLock.writeSuspend()));
        assertThat(myLock.isWriteLocked()).isFalse();
        assertThat(myLock.isWriteRequested()).isFalse();

        CountDownLatch readerInside = new CountDownLatch(1);
        CountDownLatch releaseReader = new CountDownLatch(1);
        Future<?> reader = async(() -> read(() -> {
            readerInside.countDown();
            await(releaseReader);
        }));
        await(readerInside);

        Future<?> relock = myWriter.submit(() -> Objects.requireNonNull(suspension.get()).finish());
        waitUntil(myLock::isWriteRequested);
        assertBlocked(relock);
        assertThat(myRelocks.get()).isEqualTo(1);
        get(async(() -> assertThat(Objects.requireNonNull(myLock.startTryRead()).readRequested()).isFalse()));

        releaseReader.countDown();
        get(reader);
        get(relock);
        assertThat(myLock.isWriteLocked()).isTrue();

        onWriter(() -> {
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        });
    }

    @Test
    public void transferRunsOnThreadWaitingForRead() throws Exception {
        onWriter(() -> {
            myLock.writeIntentLock();
            myLock.writeLock();
        });

        FutureTask<Void> uiRead = new FutureTask<>(() -> read(() -> assertThat(myLock.isWriteLocked()).isFalse()), null);
        Thread ui = new Thread(uiRead, "Test UI Thread");
        myUIThread = ui;
        ui.start();
        waitUntil(() -> ui.getState() == Thread.State.TIMED_WAITING);

        AtomicReference<@Nullable Thread> ranOn = new AtomicReference<>();
        AtomicBoolean writeThreadInside = new AtomicBoolean();
        onWriter(() -> myLock.transferWriteAction(true, () -> {
            ranOn.set(Thread.currentThread());
            writeThreadInside.set(myLock.isWriteThread() && myLock.isWriteLocked());
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        }, runnable -> {
        }));

        assertThat(ranOn.get()).isSameAs(ui);
        assertThat(writeThreadInside).isTrue();
        assertThat(uiRead.isDone()).isFalse();

        onWriter(() -> {
            assertThat(myLock.isWriteThread()).isTrue();
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        });
        get(uiRead);
    }

    @Test
    public void transferRunsOnThreadWaitingForWriteIntent() throws Exception {
        onWriter(() -> {
            myLock.writeIntentLock();
            myLock.writeLock();
        });

        FutureTask<Void> uiWrite = new FutureTask<>(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        }, null);
        Thread ui = new Thread(uiWrite, "Test UI Thread");
        myUIThread = ui;
        ui.start();
        waitUntil(() -> ui.getState() == Thread.State.TIMED_WAITING);

        AtomicReference<@Nullable Thread> ranOn = new AtomicReference<>();
        onWriter(() -> myLock.transferWriteAction(true, () -> ranOn.set(Thread.currentThread()), runnable -> {
        }));
        assertThat(ranOn.get()).isSameAs(ui);
        assertThat(uiWrite.isDone()).isFalse();

        onWriter(() -> {
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        });
        get(uiWrite);
    }

    @Test
    public void impatientReaderFailsWhileWriteIsRequested() throws Exception {
        onWriter(() -> {
            myLock.writeIntentLock();
            myLock.writeLock();
        });

        get(async(() -> myLock.executeByImpatientReader(
            () -> assertThatThrownBy(myLock::startRead).isInstanceOf(ApplicationUtil.CannotRunReadActionException.class)
        )));

        onWriter(() -> {
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        });
    }

    @Test
    public void cancelledReadWaitThrows() throws Exception {
        onWriter(() -> {
            myLock.writeIntentLock();
            myLock.writeLock();
        });

        Future<?> reader = async(() -> assertThatThrownBy(myLock::startRead).isInstanceOf(ProcessCanceledException.class));
        assertBlocked(reader);

        myReadWaitCancelled.set(true);
        get(reader);

        onWriter(() -> {
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        });
    }

    @Test
    public void interruptWhileWaitingForReadIsKept() throws Exception {
        onWriter(() -> {
            myLock.writeIntentLock();
            myLock.writeLock();
        });

        AtomicBoolean interruptedInside = new AtomicBoolean();
        FutureTask<Void> task = new FutureTask<>(() -> read(() -> interruptedInside.set(Thread.currentThread().isInterrupted())), null);
        Thread reader = new Thread(task, "Test Reader");
        reader.start();
        waitUntil(() -> reader.getState() == Thread.State.TIMED_WAITING);

        reader.interrupt();
        assertBlocked(task);

        onWriter(() -> {
            myLock.writeUnlock();
            myLock.writeIntentUnlock();
        });
        get(task);
        assertThat(interruptedInside).isTrue();
    }

    @Test
    public void releasingWriteIntentWithLeakedWriteRecovers() throws Exception {
        onWriter(() -> {
            myLock.writeIntentLock();
            myLock.writeLock();
            try {
                myLock.writeIntentUnlock();
            }
            catch (AssertionError ignored) {
            }
            assertThat(myLock.isWriteLocked()).isFalse();
            assertThat(myLock.isWriteThread()).isFalse();
        });

        get(async(() -> read(() -> {
        })));
        get(async(() -> {
            myLock.writeIntentLock();
            myLock.writeIntentUnlock();
        }));
    }

    @Test
    public void manyVirtualThreadReadersWithWriter() throws Exception {
        AtomicInteger readers = new AtomicInteger();
        AtomicBoolean overlap = new AtomicBoolean();

        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 10_000; i++) {
                futures.add(virtual.submit(() -> read(() -> {
                    readers.incrementAndGet();
                    if (myLock.isWriteLocked()) {
                        overlap.set(true);
                    }
                    readers.decrementAndGet();
                })));
            }

            for (int i = 0; i < 50; i++) {
                onWriter(() -> {
                    myLock.writeIntentLock();
                    myLock.writeLock();
                    if (readers.get() != 0) {
                        overlap.set(true);
                    }
                    myLock.writeUnlock();
                    myLock.writeIntentUnlock();
                });
            }

            for (Future<?> future : futures) {
                get(future);
            }
        }

        assertThat(overlap).isFalse();
    }

    @Test
    public void randomizedReadersAndWriters() throws Exception {
        AtomicInteger activeReaders = new AtomicInteger();
        AtomicInteger activeIntents = new AtomicInteger();
        AtomicBoolean violation = new AtomicBoolean();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(async(() -> {
                while (System.nanoTime() < deadline) {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        read(() -> checkReader(activeReaders, violation));
                    }
                    else {
                        RWLock.ReadToken token = Objects.requireNonNull(myLock.startTryRead());
                        if (token.readRequested()) {
                            try {
                                checkReader(activeReaders, violation);
                            }
                            finally {
                                myLock.endRead(token);
                            }
                        }
                    }
                }
            }));
        }
        for (int i = 0; i < 3; i++) {
            futures.add(async(() -> {
                while (System.nanoTime() < deadline) {
                    myLock.writeIntentLock();
                    try {
                        if (activeIntents.incrementAndGet() != 1) {
                            violation.set(true);
                        }
                        if (ThreadLocalRandom.current().nextBoolean()) {
                            myLock.writeLock();
                            if (activeReaders.get() != 0) {
                                violation.set(true);
                            }
                            myLock.writeUnlock();
                        }
                        else if (myLock.startRead() != null) {
                            violation.set(true);
                        }
                        activeIntents.decrementAndGet();
                    }
                    finally {
                        myLock.writeIntentUnlock();
                    }
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        assertThat(violation).isFalse();
    }

    private void checkReader(AtomicInteger activeReaders, AtomicBoolean violation) {
        activeReaders.incrementAndGet();
        if (myLock.isWriteLocked()) {
            violation.set(true);
        }
        Thread.onSpinWait();
        activeReaders.decrementAndGet();
    }

    private void read(Runnable action) {
        RWLock.ReadToken token = Objects.requireNonNull(myLock.startRead());
        try {
            action.run();
        }
        finally {
            myLock.endRead(token);
        }
    }

    private void onWriter(Runnable action) throws Exception {
        get(myWriter.submit(action));
    }

    private Future<?> async(Runnable action) {
        return myThreads.submit(action);
    }

    private static void get(Future<?> future) throws Exception {
        try {
            future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private static void assertBlocked(Future<?> future) throws InterruptedException {
        Thread.sleep(200);
        assertThat(future.isDone()).as("expected to stay blocked").isFalse();
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!condition.getAsBoolean()) {
            assertThat(System.nanoTime()).as("condition was not reached").isLessThan(deadline);
            Thread.sleep(5);
        }
    }
}
