// Copyright 2013-2026 consulo.io
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package consulo.util.concurrent.coroutine.test;

import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static consulo.util.concurrent.coroutine.CoroutineScope.launch;
import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link Continuation#finishEarly(Object)}.
 */
public class ContinuationFinishEarlyTest {
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
    public void testFinishEarlyWithToFuture() {
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
