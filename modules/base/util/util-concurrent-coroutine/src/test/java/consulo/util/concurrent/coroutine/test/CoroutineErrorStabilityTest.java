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

import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that a coroutine stays stable when a step throws: the failure must always propagate to
 * the {@link CompletableFuture} returned by {@link consulo.util.concurrent.coroutine.Continuation#toFuture()}
 * instead of being swallowed and leaving the future hanging.
 *
 * @author VISTALL
 */
public class CoroutineErrorStabilityTest {

    @Test
    public void testStepErrorFailsFuture() throws Exception {
        CoroutineContext context = TestCoroutineContext.newSilent();
        CoroutineScope scope = CoroutineScope.of(context);

        Coroutine<String, String> cr = Coroutine.first(CodeExecution.<String, String>apply(s -> {
            throw new RuntimeException("BOOM");
        }));

        CompletableFuture<String> future = cr.runAsync(scope, "in").toFuture();

        ExecutionException error = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertEquals("BOOM", Objects.requireNonNull(error.getCause()).getMessage());
    }

    @Test
    public void testMiddleStepErrorFailsFutureAndSkipsRest() throws Exception {
        CoroutineContext context = TestCoroutineContext.newSilent();
        CoroutineScope scope = CoroutineScope.of(context);

        AtomicBoolean afterErrorReached = new AtomicBoolean(false);

        Coroutine<String, String> cr = Coroutine.first(apply((String s) -> s + "-start"))
            .then(CodeExecution.<String, String>apply(s -> {
                throw new RuntimeException("MIDDLE BOOM");
            }))
            .then(CodeExecution.<String, String>apply(s -> {
                afterErrorReached.set(true);
                return s;
            }));

        CompletableFuture<String> future = cr.runAsync(scope, "in").toFuture();

        ExecutionException error = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertEquals("MIDDLE BOOM", Objects.requireNonNull(error.getCause()).getMessage());
        assertFalse(afterErrorReached.get());
    }

    @Test
    public void testSubroutineFactoryErrorFailsFuture() throws Exception {
        CoroutineContext context = TestCoroutineContext.newSilent();
        CoroutineScope scope = CoroutineScope.of(context);

        Supplier<Coroutine<String, String>> factory = () -> {
            throw new RuntimeException("FACTORY BOOM");
        };

        Coroutine<String, String> cr = Coroutine.first(CallSubroutine.call(factory));

        CompletableFuture<String> future = cr.runAsync(scope, "in").toFuture();

        ExecutionException error = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertEquals("FACTORY BOOM", Objects.requireNonNull(error.getCause()).getMessage());
    }

    @Test
    public void testSubroutineStepErrorFailsFuture() throws Exception {
        CoroutineContext context = TestCoroutineContext.newSilent();
        CoroutineScope scope = CoroutineScope.of(context);

        Coroutine<String, String> subroutine = Coroutine.first(CodeExecution.<String, String>apply(s -> {
            throw new RuntimeException("SUB BOOM");
        }));

        Coroutine<String, String> cr = Coroutine.first(CallSubroutine.call(subroutine));

        CompletableFuture<String> future = cr.runAsync(scope, "in").toFuture();

        ExecutionException error = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertEquals("SUB BOOM", Objects.requireNonNull(error.getCause()).getMessage());
    }

    @Test
    public void testScopeSurvivesErrorForNextCoroutine() throws Exception {
        CoroutineContext context = TestCoroutineContext.newSilent();

        CoroutineScope failingScope = CoroutineScope.of(context);
        Coroutine<String, String> failing = Coroutine.first(CodeExecution.<String, String>apply(s -> {
            throw new RuntimeException("BOOM");
        }));
        CompletableFuture<String> failingFuture = failing.runAsync(failingScope, "in").toFuture();
        assertThrows(ExecutionException.class, () -> failingFuture.get(5, TimeUnit.SECONDS));

        CoroutineScope healthyScope = CoroutineScope.of(context);
        Coroutine<String, String> healthy = Coroutine.first(CodeExecution.<String, String>apply(s -> s.toUpperCase()));
        CompletableFuture<String> healthyFuture = healthy.runAsync(healthyScope, "ok").toFuture();
        assertEquals("OK", healthyFuture.get(5, TimeUnit.SECONDS));
    }
}
