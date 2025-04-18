/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.highlight;

import consulo.application.util.Semaphore;
import consulo.application.util.function.Processor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Queue;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Allows to process elements in the EDT.
 * Processes elements in batches, no longer than 200ms (or maxUnitOfWorkThresholdMs constructor parameter) per batch,
 * and reschedules processing later for longer batches.
 * Usage: {@link TransferToEDTQueue#offer(Object)} } : schedules element for processing in EDT (via invokeLater)
 *
 * @deprecated use {@link EdtExecutorService} instead
 */
@Deprecated
public class TransferToEDTQueue<T> {
    /**
     * This is a default threshold used to join units of work.
     * It allows to generate more that 30 frames per second.
     * It is not recommended to block EDT longer,
     * because people feel that UI is laggy.
     *
     * @see #TransferToEDTQueue(String, Processor, Condition, int)
     * @see #createRunnableMerger(String, int)
     */
    public static final int DEFAULT_THRESHOLD = 30;
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private final String myName;
    private final Processor<T> myProcessor;
    private volatile boolean stopped;
    private final BooleanSupplier myShutUpCondition;
    private final int myMaxUnitOfWorkThresholdMs; //-1 means indefinite

    private final Queue<T> myQueue = new Queue<T>(10); // guarded by myQueue
    private final AtomicBoolean invokeLaterScheduled = new AtomicBoolean();
    private final Runnable myUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            boolean b = invokeLaterScheduled.compareAndSet(true, false);
            assert b;
            if (stopped || myShutUpCondition.getAsBoolean()) {
                stop();
                return;
            }
            long start = System.currentTimeMillis();
            while (processNext()) {
                long finish = System.currentTimeMillis();
                if (myMaxUnitOfWorkThresholdMs != -1 && finish - start > myMaxUnitOfWorkThresholdMs) {
                    break;
                }
            }
            if (!isEmpty()) {
                scheduleUpdate();
            }
        }

        @Override
        public String toString() {
            return TransferToEDTQueue.this.getClass().getSimpleName() + "[" + myName + "]";
        }
    };

    public TransferToEDTQueue(@Nonnull @NonNls String name, @Nonnull Processor<T> processor, @Nonnull BooleanSupplier shutUpCondition) {
        this(name, processor, shutUpCondition, DEFAULT_THRESHOLD);
    }

    public TransferToEDTQueue(
        @Nonnull @NonNls String name,
        @Nonnull Processor<T> processor,
        @Nonnull BooleanSupplier shutUpCondition,
        int maxUnitOfWorkThresholdMs
    ) {
        myName = name;
        myProcessor = processor;
        myShutUpCondition = shutUpCondition;
        myMaxUnitOfWorkThresholdMs = maxUnitOfWorkThresholdMs;
    }

    public static TransferToEDTQueue<Runnable> createRunnableMerger(@Nonnull @NonNls String name) {
        return createRunnableMerger(name, DEFAULT_THRESHOLD);
    }

    public static TransferToEDTQueue<Runnable> createRunnableMerger(@Nonnull @NonNls String name, int maxUnitOfWorkThresholdMs) {
        return new TransferToEDTQueue<Runnable>(name, runnable -> {
            runnable.run();
            return true;
        }, () -> false, maxUnitOfWorkThresholdMs);
    }

    private boolean isEmpty() {
        synchronized (myQueue) {
            return myQueue.isEmpty();
        }
    }

    // return true if element was pulled from the queue and processed successfully
    private boolean processNext() {
        T thing = pullFirst();
        if (thing == null) {
            return false;
        }
        if (!myProcessor.process(thing)) {
            stop();
            return false;
        }
        return true;
    }

    protected T pullFirst() {
        synchronized (myQueue) {
            return myQueue.isEmpty() ? null : myQueue.pullFirst();
        }
    }

    public boolean offer(@Nonnull T thing) {
        synchronized (myQueue) {
            myQueue.addLast(thing);
        }
        scheduleUpdate();
        return true;
    }

    public boolean offerIfAbsent(@Nonnull T thing) {
        return offerIfAbsent(thing, ContainerUtil.<T>canonicalStrategy());
    }

    public boolean offerIfAbsent(@Nonnull final T thing, @Nonnull final HashingStrategy<T> equality) {
        synchronized (myQueue) {
            boolean absent = myQueue.process(t -> !equality.equals(t, thing));
            if (absent) {
                myQueue.addLast(thing);
                scheduleUpdate();
            }
            return absent;
        }
    }

    private void scheduleUpdate() {
        if (!stopped && invokeLaterScheduled.compareAndSet(false, true)) {
            schedule(myUpdateRunnable);
        }
    }

    protected void schedule(@Nonnull Runnable updateRunnable) {
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(updateRunnable);
    }

    public void stop() {
        stopped = true;
        synchronized (myQueue) {
            myQueue.clear();
        }
    }

    public int size() {
        synchronized (myQueue) {
            return myQueue.size();
        }
    }

    @TestOnly
    @Nonnull
    public Collection<T> dump() {
        synchronized (myQueue) {
            return myQueue.toList();
        }
    }

    // process all queue in current thread
    public void drain() {
        int processed = 0;
        while (processNext()) {
            processed++;
        }
    }

    // blocks until all elements in the queue are processed
    public void waitFor() {
        final Semaphore semaphore = new Semaphore();
        semaphore.down();
        schedule(new Runnable() {
            @Override
            public void run() {
                semaphore.up();
            }
        });
        semaphore.waitFor();
    }
}
