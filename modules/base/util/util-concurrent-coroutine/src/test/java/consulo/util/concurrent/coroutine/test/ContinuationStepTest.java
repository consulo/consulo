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
import org.junit.jupiter.api.Test;

import static consulo.util.concurrent.coroutine.Coroutine.first;
import static consulo.util.concurrent.coroutine.CoroutineScope.launch;
import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;
import static consulo.util.concurrent.coroutine.step.ContinuationStep.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link consulo.util.concurrent.coroutine.step.ContinuationStep}.
 *
 * @author VISTALL
 * @since 2026-07-13
 */
public class ContinuationStepTest {

    /**
     * Joins the {@link Continuation} of a nested coroutine into a parent chain and continues with its result.
     */
    @Test
    public void testAwaitContinuation() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, Integer> sub = first(apply((String s) -> s.length()));

        launch(context, scope -> {
            Coroutine<String, Integer> parent =
                Coroutine.<String, Integer>first(await(s -> sub.runAsync(scope, s)))
                    .then(apply(i -> i + 100));

            Continuation<Integer> ca = parent.runAsync(scope, "hello");
            Continuation<Integer> cb = parent.runBlocking(scope, "hello");

            assertEquals(Integer.valueOf(105), ca.getResult());
            assertEquals(Integer.valueOf(105), cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
        });
    }

    /**
     * The awaited continuation may itself be produced by a blocking run of the nested coroutine.
     */
    @Test
    public void testAwaitBlockingContinuation() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<Integer, Integer> sub = first(apply((Integer i) -> i * 2));

        launch(context, scope -> {
            Coroutine<Integer, Integer> parent =
                Coroutine.<Integer, Integer>first(await(i -> sub.runBlocking(scope, i)))
                    .then(apply(i -> i + 1));

            Continuation<Integer> ca = parent.runAsync(scope, 20);
            Continuation<Integer> cb = parent.runBlocking(scope, 20);

            assertEquals(Integer.valueOf(41), ca.getResult());
            assertEquals(Integer.valueOf(41), cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
        });
    }
}
