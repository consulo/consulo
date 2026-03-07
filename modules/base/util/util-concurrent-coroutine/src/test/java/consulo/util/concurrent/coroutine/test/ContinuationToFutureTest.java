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

import consulo.util.concurrent.coroutine.ChannelId;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.step.ChannelReceive;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static consulo.util.concurrent.coroutine.ChannelId.stringChannel;
import static consulo.util.concurrent.coroutine.CoroutineScope.launch;
import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Continuation#toFuture()}.
 *
 * @author VISTALL
 * @since 2026-03-07
 */
public class ContinuationToFutureTest {

    @Test
    public void testToFutureSuccess() throws Exception {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, String> cr =
            Coroutine.first(apply((String s) -> s.toUpperCase()));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, "hello");
            CompletableFuture<String> future = ca.toFuture();

            assertEquals("HELLO", future.get());
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        });
    }

    @Test
    public void testToFutureCancellation() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        ChannelId<String> ch = stringChannel("TEST_CANCEL");
        Coroutine<?, ?> cr = Coroutine.first(ChannelReceive.receive(ch));

        launch(context, scope -> {
            Continuation<?> ca = cr.runAsync(scope, null);
            CompletableFuture<?> future = ca.toFuture();

            scope.cancel();

            // wait on the future itself — scope.await() can return before
            // the onCancel callback fires (finish signal counted down first)
            try {
                future.get();
                fail("Expected CancellationException");
            }
            catch (CancellationException e) {
                // expected
            }

            assertTrue(future.isCancelled());
            assertTrue(future.isDone());
        });
    }

    @Test
    public void testToFutureError() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<?, ?> cr = Coroutine.first(apply(v -> {
            throw new RuntimeException("TEST_ERROR");
        }));

        launch(context, scope -> {
            Continuation<?> ca = cr.runAsync(scope, null);
            CompletableFuture<?> future = ca.toFuture();

            // wait on the future itself — ca.await() can return before
            // the onError callback fires (finish signal counted down first)
            try {
                future.get();
                fail("Expected ExecutionException");
            }
            catch (ExecutionException e) {
                assertNotNull(e.getCause());
            }

            assertTrue(future.isCompletedExceptionally());
            assertTrue(future.isDone());

            // mark error as handled so launch() doesn't throw CoroutineScopeException
            ca.errorHandled();
        });
    }

    @Test
    public void testToFutureAlreadyFinished() throws Exception {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, String> cr =
            Coroutine.first(apply((String s) -> s.toUpperCase()));

        launch(context, scope -> {
            Continuation<String> ca = cr.runBlocking(scope, "world");

            assertTrue(ca.isFinished());

            CompletableFuture<String> future = ca.toFuture();

            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
            assertEquals("WORLD", future.getNow(null));
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFinishEarly() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        AtomicBoolean step2Executed = new AtomicBoolean(false);
        AtomicBoolean step3Executed = new AtomicBoolean(false);

        Coroutine<String, String> cr = Coroutine
            .first(CodeExecution.<String, String>apply((v, continuation) -> {
                ((Continuation<String>) continuation).finishEarly("EARLY_RESULT");
                return "ignored";
            }))
            .then(apply((String s) -> {
                step2Executed.set(true);
                return s + "_step2";
            }))
            .then(apply((String s) -> {
                step3Executed.set(true);
                return s + "_step3";
            }));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, "input");

            assertEquals("EARLY_RESULT", ca.getResult());
            assertTrue(ca.isFinished());
            assertFalse(ca.isCancelled());
            assertFalse(step2Executed.get());
            assertFalse(step3Executed.get());
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFinishEarlyWithToFuture() throws Exception {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, String> cr = Coroutine
            .first(CodeExecution.<String, String>apply((v, continuation) -> {
                ((Continuation<String>) continuation).finishEarly("EARLY");
                return "ignored";
            }))
            .then(apply((String s) -> s + "_never"));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, "input");
            CompletableFuture<String> future = ca.toFuture();

            assertEquals("EARLY", future.get());
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        });
    }
}
