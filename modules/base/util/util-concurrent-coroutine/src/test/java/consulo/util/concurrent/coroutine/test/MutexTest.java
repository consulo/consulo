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
package consulo.util.concurrent.coroutine.test;

import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.Mutex;
import consulo.util.concurrent.coroutine.step.Delay;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static consulo.util.concurrent.coroutine.CoroutineScope.launch;
import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test of {@link Mutex}.
 *
 * @author VISTALL
 * @since 2026-09-05
 */
public class MutexTest {
    private static final long SETTLE_MILLIS = 100L;

    @Test
    public void testTryLockAndUnlock() {
        Mutex mutex = new Mutex();

        assertFalse(mutex.isLocked());
        assertTrue(mutex.tryLock("a"));
        assertTrue(mutex.isLocked());
        assertTrue(mutex.holdsLock("a"));
        assertFalse(mutex.holdsLock("b"));
        assertFalse(mutex.tryLock("b"));

        mutex.unlock("a");

        assertFalse(mutex.isLocked());
        assertFalse(mutex.holdsLock("a"));
        assertTrue(mutex.tryLock("b"));

        mutex.unlock(null);

        assertFalse(mutex.isLocked());
    }

    @Test
    public void testUnlockWithWrongOwnerThrows() {
        Mutex mutex = new Mutex();

        assertTrue(mutex.tryLock("a"));
        assertThrows(IllegalStateException.class, () -> mutex.unlock("b"));
        assertTrue(mutex.holdsLock("a"));

        mutex.unlock("a");

        assertThrows(IllegalStateException.class, () -> mutex.unlock("a"));
        assertThrows(IllegalStateException.class, () -> mutex.unlock(null));
    }

    @Test
    public void testTryLockBySameOwnerThrows() {
        Mutex mutex = new Mutex();

        assertTrue(mutex.tryLock("a"));
        assertThrows(IllegalStateException.class, () -> mutex.tryLock("a"));
        assertTrue(mutex.holdsLock("a"));
    }

    @Test
    public void testSuspendedLockResumesOnUnlock() {
        CoroutineContext context = TestCoroutineContext.newSilent();
        Mutex mutex = new Mutex();

        Coroutine<String, String> cr = Coroutine.first(mutex.<String>lock("waiter"))
            .then(apply((String s) -> s + ":" + mutex.holdsLock("waiter")))
            .then(apply((String s) -> {
                mutex.unlock("waiter");
                return s;
            }));

        assertTrue(mutex.tryLock("holder"));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, "in");

            assertFalse(ca.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS));
            assertTrue(mutex.holdsLock("holder"));

            mutex.unlock("holder");

            assertEquals("in:true", ca.getResult());
            assertFalse(mutex.isLocked());
        });
    }

    @Test
    public void testImmediateLockWhenFree() {
        CoroutineContext context = TestCoroutineContext.newSilent();
        Mutex mutex = new Mutex();

        launch(context, scope -> {
            Continuation<Boolean> ca = lockAndRelease(mutex, "async").runAsync(scope, 1);
            Continuation<Boolean> cb = lockAndRelease(mutex, "blocking").runBlocking(scope, 2);

            assertEquals(Boolean.TRUE, ca.getResult());
            assertEquals(Boolean.TRUE, cb.getResult());
            assertFalse(mutex.isLocked());
        });
    }

    private static Coroutine<Integer, Boolean> lockAndRelease(Mutex mutex, String owner) {
        return Coroutine.first(mutex.<Integer>lock(owner))
            .then(apply((Integer i) -> {
                boolean held = mutex.holdsLock(owner);
                mutex.unlock(owner);
                return held;
            }));
    }

    @Test
    public void testMutualExclusionBetweenCoroutines() {
        CoroutineContext context = TestCoroutineContext.newSilent(Executors.newFixedThreadPool(4));
        Mutex mutex = new Mutex();

        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxInside = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();

        int count = 8;
        List<Coroutine<Integer, Integer>> coroutines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String owner = "c" + i;
            coroutines.add(Coroutine.first(mutex.<Integer>lock(owner))
                .then(apply((Integer v) -> {
                    maxInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
                    assertTrue(mutex.holdsLock(owner));
                    return v;
                }))
                .then(Delay.sleep(5))
                .then(apply((Integer v) -> {
                    inside.decrementAndGet();
                    completed.incrementAndGet();
                    mutex.unlock(owner);
                    return v;
                })));
        }

        launch(context, scope -> {
            List<Continuation<Integer>> continuations = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                continuations.add(coroutines.get(i).runAsync(scope, i));
            }
            for (int i = 0; i < count; i++) {
                assertEquals(Integer.valueOf(i), continuations.get(i).getResult());
            }
        });

        assertEquals(count, completed.get());
        assertEquals(1, maxInside.get());
        assertEquals(0, inside.get());
        assertFalse(mutex.isLocked());
    }

    @Test
    public void testCancelledWaiterIsSkipped() {
        CoroutineContext context = TestCoroutineContext.newSilent();
        Mutex mutex = new Mutex();

        Coroutine<String, String> first = Coroutine.first(mutex.<String>lock("first"))
            .then(apply((String s) -> {
                mutex.unlock("first");
                return s;
            }));
        Coroutine<String, String> second = Coroutine.first(mutex.<String>lock("second"))
            .then(apply((String s) -> {
                mutex.unlock("second");
                return s;
            }));

        assertTrue(mutex.tryLock("holder"));

        launch(context, scope -> {
            Continuation<String> c1 = first.runAsync(scope, "1");

            assertFalse(c1.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS));

            c1.cancel();
            c1.await();

            Continuation<String> c2 = second.runAsync(scope, "2");

            assertFalse(c2.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS));

            mutex.unlock("holder");

            assertEquals("2", c2.getResult());
            assertTrue(c1.isCancelled());
            assertFalse(mutex.isLocked());
        });
    }

    @Test
    public void testLockCancellableHonoursCancellationCheck() throws Exception {
        Mutex mutex = new Mutex();
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean caught = new AtomicBoolean();

        assertTrue(mutex.tryLock("holder"));

        Thread thread = new Thread(() -> {
            try {
                mutex.lockCancellable("waiter", () -> {
                    if (cancelled.get()) {
                        throw new CancellationException();
                    }
                });
            }
            catch (CancellationException e) {
                caught.set(true);
            }
        });
        thread.start();

        thread.join(SETTLE_MILLIS);
        assertTrue(thread.isAlive());

        cancelled.set(true);
        thread.join(TimeUnit.SECONDS.toMillis(5));

        assertFalse(thread.isAlive());
        assertTrue(caught.get());
        assertTrue(mutex.holdsLock("holder"));
        assertFalse(mutex.holdsLock("waiter"));
    }

    @Test
    public void testLockCancellableAcquiresAfterUnlock() throws Exception {
        Mutex mutex = new Mutex();
        AtomicBoolean acquired = new AtomicBoolean();

        assertTrue(mutex.tryLock("holder"));

        Thread thread = new Thread(() -> {
            mutex.lockCancellable("waiter", () -> {
            });
            acquired.set(true);
        });
        thread.start();

        thread.join(SETTLE_MILLIS);
        assertFalse(acquired.get());

        mutex.unlock("holder");
        thread.join(TimeUnit.SECONDS.toMillis(5));

        assertTrue(acquired.get());
        assertTrue(mutex.holdsLock("waiter"));
    }
}
