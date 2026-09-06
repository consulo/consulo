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
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.step.AwaitValue;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static consulo.util.concurrent.coroutine.CoroutineScope.launch;
import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test of {@link AwaitValue}.
 *
 * @author VISTALL
 * @since 2026-09-05
 */
public class AwaitValueTest {
    private static final long SETTLE_MILLIS = 100L;

    @Test
    public void testImmediateCompletion() {
        CoroutineContext context = TestCoroutineContext.newSilent();
        ObservableValue<Boolean> value = ObservableValue.of(true);

        Coroutine<@Nullable Void, Boolean> cr = Coroutine.first(AwaitValue.until(value, v -> v));

        launch(context, scope -> {
            Continuation<Boolean> ca = cr.runAsync(scope, null);
            Continuation<Boolean> cb = cr.runBlocking(scope, null);

            assertEquals(Boolean.TRUE, ca.getResult());
            assertEquals(Boolean.TRUE, cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
            assertEquals(0, value.listenerCount());
        });
    }

    @Test
    public void testWakeUpOnMatchingSet() {
        CoroutineContext context = TestCoroutineContext.newSilent();
        ObservableValue<Boolean> value = ObservableValue.of(false);

        Coroutine<@Nullable Void, String> cr = Coroutine.<@Nullable Void, Boolean>first(AwaitValue.until(value, v -> v == Boolean.TRUE))
            .then(apply(v -> "resumed:" + v));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, null);

            assertFalse(ca.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS));
            assertEquals(1, value.listenerCount());

            value.set(true);

            assertEquals("resumed:true", ca.getResult());
            assertEquals(0, value.listenerCount());
        });
    }

    @Test
    public void testNoWakeUpOnNonMatchingSet() {
        CoroutineContext context = TestCoroutineContext.newSilent();
        ObservableValue<Integer> value = ObservableValue.of(0);

        Coroutine<@Nullable Void, Integer> cr = Coroutine.first(AwaitValue.until(value, v -> v > 5));

        launch(context, scope -> {
            Continuation<Integer> ca = cr.runAsync(scope, null);

            assertFalse(ca.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS));

            value.set(1);
            value.set(2);

            assertFalse(ca.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS));
            assertEquals(1, value.listenerCount());

            value.set(10);

            assertEquals(Integer.valueOf(10), ca.getResult());
            assertEquals(0, value.listenerCount());
        });
    }

    @Test
    public void testCombineWakesWhenEitherSourceFlips() {
        CoroutineContext context = TestCoroutineContext.newSilent();
        ObservableValue<Boolean> running = ObservableValue.of(true);
        ObservableValue<Boolean> allowed = ObservableValue.of(false);

        Coroutine<String, String> cr = Coroutine.first(AwaitValue.until(() -> !running.get() && allowed.get(), running, allowed));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, "first");

            assertFalse(ca.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS));

            running.set(false);

            assertFalse(ca.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS));

            allowed.set(true);

            assertEquals("first", ca.getResult());
            assertEquals(0, running.listenerCount());
            assertEquals(0, allowed.listenerCount());
        });

        running.set(true);

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, "second");

            assertFalse(ca.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS));

            running.set(false);

            assertEquals("second", ca.getResult());
            assertEquals(0, running.listenerCount());
            assertEquals(0, allowed.listenerCount());
        });
    }

    @Test
    public void testCancellationDeregistersListeners() {
        CoroutineContext context = TestCoroutineContext.newSilent();
        ObservableValue<Boolean> value = ObservableValue.of(false);

        Coroutine<@Nullable Void, Boolean> cr = Coroutine.first(AwaitValue.until(value, v -> v));

        launch(context, scope -> {
            Continuation<Boolean> ca = cr.runAsync(scope, null);

            assertFalse(ca.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS));
            assertEquals(1, value.listenerCount());

            ca.cancel();
            ca.await();

            assertTrue(ca.isCancelled());
            assertEquals(0, value.listenerCount());

            value.set(true);

            assertTrue(ca.isCancelled());
        });
    }

    @Test
    public void testBlockingWaitsForMatchingValue() {
        CoroutineContext context = TestCoroutineContext.newSilent();
        ObservableValue<Boolean> value = ObservableValue.of(false);

        Coroutine<@Nullable Void, Boolean> cr = Coroutine.first(AwaitValue.until(value, v -> v));

        launch(context, scope -> {
            context.getScheduler().schedule(() -> value.set(true), SETTLE_MILLIS, TimeUnit.MILLISECONDS);

            Continuation<Boolean> cb = cr.runBlocking(scope, null);

            assertEquals(Boolean.TRUE, cb.getResult());
            assertTrue(cb.isFinished());
            assertEquals(0, value.listenerCount());
        });
    }
}
