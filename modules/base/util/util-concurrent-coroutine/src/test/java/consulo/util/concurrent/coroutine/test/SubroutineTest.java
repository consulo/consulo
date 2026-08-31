//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package consulo.util.concurrent.coroutine.test;

import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.dataholder.Key;
import org.junit.jupiter.api.Test;

import static consulo.util.concurrent.coroutine.Coroutine.first;
import static consulo.util.concurrent.coroutine.CoroutineScope.launch;
import static consulo.util.concurrent.coroutine.step.CallSubroutine.call;
import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;
import static consulo.util.concurrent.coroutine.step.CodeExecution.supply;
import static consulo.util.concurrent.coroutine.step.Condition.doIf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link Subroutine}.
 *
 * @author eso
 */
public class SubroutineTest {

    /**
     * Test of subroutine invocations.
     */
    @Test
    public void testSubroutine() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, Integer> cr =
            first(call(CoroutineTest.CONVERT_INT))
                .then(apply(i -> i + 10));

        launch(context, scope -> {
            Continuation<Integer> ca = cr.runAsync( scope, "test1234");
            Continuation<Integer> cb = cr.runBlocking(scope, "test1234");

            assertEquals(Integer.valueOf(12355), ca.getResult());
            assertEquals(Integer.valueOf(12355), cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
        });
    }

    /**
     * Test of invoking an empty coroutine as a subroutine.
     */
    @Test
    public void testEmptySubroutine() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, String> cr =
            first(call(Coroutine.<String, Object>empty()))
                .then(apply(i -> "after"));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, "test");
            Continuation<String> cb = cr.runBlocking(scope, "test");

            assertEquals("after", ca.getResult());
            assertEquals("after", cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
        });
    }

    /**
     * Test of early subroutine termination.
     */
    @Test
    public void testSubroutineTermination() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<Boolean, String> cr = first(call(first(
            doIf((Boolean b) -> b == Boolean.TRUE, supply(() -> "TRUE")))));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, false);
            Continuation<String> cb = cr.runBlocking(scope, false);

            assertEquals(null, ca.getResult());
            assertEquals(null, cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());

            ca = cr.runAsync(scope, true);
            cb = cr.runBlocking(scope, true);

            assertEquals("TRUE", ca.getResult());
            assertEquals("TRUE", cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
        });
    }

    /**
     * Test of invoking a lazily supplied coroutine as a subroutine. The supplier must be
     * evaluated only when the step runs, so it can capture values produced by earlier steps.
     */
    @Test
    public void testLazySubroutine() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        String[] holder = new String[1];

        Coroutine<String, String> cr =
            first(apply((String s) -> {
                holder[0] = s.toUpperCase();
                return s;
            }))
                .then(call(() -> {
                    // captured when the supplier runs; would be null if evaluated before the first step
                    String captured = holder[0];
                    return first(apply((String s) -> s + ":" + captured));
                }));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, "abc");
            Continuation<String> cb = cr.runBlocking(scope, "abc");

            assertEquals("abc:ABC", ca.getResult());
            assertEquals("abc:ABC", cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
        });
    }

    /**
     * Test of invoking a subroutine produced from the continuation user data, which was
     * populated by an earlier step.
     */
    @Test
    public void testContinuationSubroutine() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Key<String> key = Key.create("test.subroutine.value");

        Coroutine<String, String> cr =
            first(apply((String s, Continuation<?> c) -> {
                c.putUserData(key, s.toUpperCase());
                return s;
            }))
                .then(call(c -> {
                    // read from the continuation when the factory runs; would be null if evaluated too early
                    String captured = c.getUserData(key);
                    return first(apply((String s) -> s + ":" + captured));
                }));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, "abc");
            Continuation<String> cb = cr.runBlocking(scope, "abc");

            assertEquals("abc:ABC", ca.getResult());
            assertEquals("abc:ABC", cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
        });
    }
}