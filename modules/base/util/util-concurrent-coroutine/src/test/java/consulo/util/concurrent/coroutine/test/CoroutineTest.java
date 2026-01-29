//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2019 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
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

import consulo.util.concurrent.coroutine.*;
import consulo.util.concurrent.coroutine.internal.Coroutines;
import consulo.util.concurrent.coroutine.step.ChannelReceive;
import consulo.util.concurrent.coroutine.step.Iteration;
import consulo.util.concurrent.coroutine.step.Loop;
import consulo.util.dataholder.CopyableUserDataHolder;
import consulo.util.dataholder.Key;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import static consulo.util.concurrent.coroutine.ChannelId.stringChannel;
import static consulo.util.concurrent.coroutine.CoroutineScope.launch;
import static consulo.util.concurrent.coroutine.CoroutineScope.produce;
import static consulo.util.concurrent.coroutine.step.CodeExecution.*;
import static consulo.util.concurrent.coroutine.step.Condition.doIf;
import static consulo.util.concurrent.coroutine.step.Condition.doIfElse;
import static org.junit.jupiter.api.Assertions.*;

/*
 * Test of {@link Coroutine}.
 *
 * @author eso
 */
public class CoroutineTest {
    private static final Key<String> TEXT = Key.create("TEXT");

    static Coroutine<String, Integer> CONVERT_INT =
        Coroutine.first(apply((String s) -> s + 5))
            .then(apply(s -> s.replaceAll("\\D", "")))
            .then(apply(s -> Integer.valueOf(s)));

    /**
     * Test of coroutines with a single step.
     */
    @Test
    public void testCancelSuspension() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        ChannelId<String> ch = stringChannel("TEST_SUSP");

        Coroutine<?, ?> cr = Coroutine.first(ChannelReceive.receive(ch));

        launch(context, scope -> {
            // this will block because the channel is never sent to
            Continuation<?> ca = cr.runAsync(scope, null);

            // cancel the scope with the suspended receive
            scope.cancel();

            // await async cancelled receive so that states can be checked
            scope.await();

            assertTrue(ca.isCancelled());
            assertTrue(ca.isFinished());

            try {
                ca.getResult();
                fail();
            }
            catch (CancellationException e) {
                // expected
            }
        });
    }

    /**
     * Test of {@link Condition} step.
     */
    @Test
    public void testCondition() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        launch(context, scope -> {
            assertBooleanInput("true", "false", scope, Coroutine.first(
                doIfElse(b -> b, supply(() -> "true"), supply(() -> "false"))));

            assertBooleanInput("true", "false", scope, Coroutine.first(
                doIf((Boolean b) -> b, supply(() -> "true")).orElse(
                    supply(() -> "false"))));

            assertBooleanInput("true", "false", scope,
                Coroutine.first(apply((Boolean b) -> b.toString()))
                    .then(apply(s -> Boolean.valueOf(s)))
                    .then(doIfElse(b -> b, supply(() -> "true"),
                        supply(() -> "false"))));

            assertBooleanInput("true", "false", scope,
                Coroutine.first(apply((Boolean b) -> b.toString()))
                    .then(apply(s -> Boolean.valueOf(s)))
                    .then(doIf((Boolean b) -> b, supply(() -> "true")).orElse(
                        supply(() -> "false"))));

            assertBooleanInput("true", null, scope,
                Coroutine.first(doIf(b -> b, supply(() -> "true"))));
        });
    }

    /**
     * Test of
     * {@link
     * CoroutineScope#launch(CoroutineScope.ScopeCode)} with
     * an empty scope that doesn't execute coroutines.
     */
    @Test
    public void testEmptyScope() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        launch(context, scope -> {
        });

        try {
            launch(context, scope -> {
                throw new Exception("TEST");
            });
            fail();
        }
        catch (Exception e) {
            // expected
        }
    }

    /**
     * Test of coroutine error handling.
     */
    @Test
    public void testErrorHandling() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<?, ?> coroutine = Coroutine.first(run(() -> {
            throw new RuntimeException("TEST ERROR");
        }));

        try {
            launch(context, s -> coroutine.runBlocking(s, null));
            fail();
        }
        catch (CoroutineScopeException e) {
            assertEquals(1, e.getFailedContinuations().size());
        }
        try {
            launch(context, s -> coroutine.runAsync(s, null));
            fail();
        }
        catch (CoroutineScopeException e) {
            assertEquals(1, e.getFailedContinuations().size());
        }
    }

    /**
     * Test of {@link Iteration} step.
     */
    @Test
    public void testIteration() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, List<String>> cr =
            Coroutine.first(apply((String s) -> Arrays.asList(s.split(","))))
                .then(Iteration.collectEach(apply((String s) -> s.toUpperCase())));

        launch(context, scope -> {
            Continuation<?> ca = cr.runAsync(scope, "a,b,c,d");
            Continuation<?> cb = cr.runBlocking(scope, "a,b,c,d");

            assertEquals(Arrays.asList("A", "B", "C", "D"), ca.getResult());
            assertEquals(Arrays.asList("A", "B", "C", "D"), cb.getResult());
        });
    }

    private static void addListener(CopyableUserDataHolder dataHolder, Consumer<CoroutineEvent> listener) {
        List<Consumer<CoroutineEvent>> list = dataHolder.getCopyableUserData(Coroutines.COROUTINE_LISTENERS);
        if (list == null) {
            dataHolder.putCopyableUserData(Coroutines.COROUTINE_LISTENERS, list = new ArrayList<>());
        }

        list.add(listener);
    }

    /**
     * Test of coroutines with a single step.
     */
    @Test
    public void testListener() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Set<String> aEvents = new HashSet<>();
        Coroutine<String, String> cr =
            Coroutine.first(apply((String s) -> s.toUpperCase()));

        addListener(cr, e -> aEvents.add("CR-" + e.getType()));

        addListener(context, e -> aEvents.add("CTX-" + e.getType()));

        launch(context, scope -> {
            addListener(scope, e -> aEvents.add("SCOPE-" + e.getType()));

            Continuation<String> c = cr.runAsync(scope, "test");

            c.await();
            assertTrue(c.isFinished());
        });

        assertTrue(aEvents.contains("CR-STARTED"));
        assertTrue(aEvents.contains("CR-FINISHED"));
        assertTrue(aEvents.contains("SCOPE-STARTED"));
        assertTrue(aEvents.contains("SCOPE-FINISHED"));
        assertTrue(aEvents.contains("CTX-STARTED"));
        assertTrue(aEvents.contains("CTX-FINISHED"));
    }

    /**
     * Test of the {@link Loop} step.
     */
    @Test
    public void testLoop() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<Integer, Integer> cr =
            Coroutine.first(Loop.loopWhile(i -> i < 10, apply(i -> i + 1)));

        launch(context, scope -> {
            Continuation<Integer> ca = cr.runAsync(scope, 1);
            Continuation<Integer> cb = cr.runBlocking(scope, 1);

            assertEquals(Integer.valueOf(10), ca.getResult());
            assertEquals(Integer.valueOf(10), cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
        });
    }

    /**
     * Test of coroutines with multiple steps.
     */
    @Test
    public void testMultiStep() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        launch(context, scope -> {
            Continuation<Integer> ca = CONVERT_INT.runAsync(scope, "test1234");

            Continuation<Integer> cb =
                CONVERT_INT.runBlocking(scope, "test1234");

            assertEquals(Integer.valueOf(12345), ca.getResult());
            assertEquals(Integer.valueOf(12345), cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
        });
    }

    /**
     * Test of
     * {@link CoroutineScope#produce(java.util.function.Function,
     * CoroutineScope.ScopeCode)}.
     */
    @Test
    public void testProduce() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, String> cr =
            Coroutine.first(apply((String s) -> s.toUpperCase()))
                .then(setScopeParameter(TEXT));

        CoroutineScope.ScopeFuture<String> result =
            produce(context, c -> c.getUserData(TEXT), scope -> cr.runAsync(scope, "test"));

        assertEquals("TEST", result.get());
        assertTrue(result.isDone());
    }

    /**
     * Test of coroutines with a single step.
     */
    @Test
    public void testSingleStep() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, String> cr =
            Coroutine.first(apply((String s) -> s.toUpperCase()));

        launch(context, scope -> {
            Continuation<String> ca = cr.runAsync(scope, "test");
            Continuation<String> cb = cr.runBlocking(scope, "test");

            assertEquals("TEST", ca.getResult());
            assertEquals("TEST", cb.getResult());
            assertTrue(ca.isFinished());
            assertTrue(cb.isFinished());
        });
    }

    /**
     * Test of {@link Coroutine#toString()} and
     * {@link CoroutineScope#toString()}.
     */
    @Test
    public void testToString() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, String> cr =
            Coroutine.first(apply((String s) -> s.toUpperCase()));

        assertEquals("Coroutine[CodeExecution -> FinishStep]", cr.toString());

        launch(context, scope -> assertEquals("CoroutineScope[0]", scope.toString()));
    }

    /**
     * Asserts the results of executing a {@link Coroutine} with a boolean
     * input.
     *
     * @param sTrueResult  The expected result for a TRUE input
     * @param sFalseResult The expected result for a FALSE input
     * @param rScope       run The coroutine scope
     * @param cr           The coroutine
     */
    void assertBooleanInput(String sTrueResult, String sFalseResult,
                            CoroutineScope rScope, Coroutine<Boolean, String> cr) {
        Continuation<String> cat = cr.runAsync(rScope, true);
        Continuation<String> caf = cr.runAsync(rScope, false);
        Continuation<String> cbt = cr.runBlocking(rScope, true);
        Continuation<String> cbf = cr.runBlocking(rScope, false);

        assertEquals(sTrueResult, cat.getResult());
        assertEquals(sTrueResult, cbt.getResult());
        assertEquals(sFalseResult, caf.getResult());
        assertEquals(sFalseResult, cbf.getResult());
        assertTrue(cat.isFinished());
        assertTrue(caf.isFinished());
        assertTrue(cbt.isFinished());
        assertTrue(cbf.isFinished());
    }
}