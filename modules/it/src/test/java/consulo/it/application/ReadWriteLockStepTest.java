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
package consulo.it.application;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.concurrent.coroutine.ReadWriteLock;
import consulo.application.internal.ApplicationEx;
import consulo.it.HeadlessApplicationExtension;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.CoroutineStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@link ReadWriteLock} step lets no write action in between its read and its write, with either application lock.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ReadWriteLockStepTest {
    private static final long TIMEOUT_SECONDS = 20;

    @Test
    public void readRunsOnceWithoutCompetingWrite(Application application) throws Exception {
        AtomicInteger state = new AtomicInteger();
        AtomicInteger reads = new AtomicInteger();

        Integer seen = run(application, ReadWriteLock.<Object, Integer, Integer>apply(
            ignored -> {
                reads.incrementAndGet();
                assertThat(application.isReadAccessAllowed()).isTrue();
                return state.get();
            },
            value -> {
                assertThat(application.isWriteAccessAllowed()).isTrue();
                state.set(value + 1);
                return value;
            }
        ));

        assertThat(seen).isEqualTo(0);
        assertThat(reads).hasValue(1);
        assertThat(state).hasValue(1);
    }

    @Test
    public void writeAfterTheReadMakesTheReadRunAgain(Application application) throws Exception {
        ApplicationEx applicationEx = (ApplicationEx) application;
        AtomicInteger state = new AtomicInteger();
        AtomicInteger reads = new AtomicInteger();
        AtomicReference<Thread> competitor = new AtomicReference<>();

        Integer seen = run(application, ReadWriteLock.<Object, Integer, Integer>apply(
            ignored -> {
                if (reads.incrementAndGet() == 1) {
                    Thread writer = new Thread(() -> WriteAction.run(state::incrementAndGet), "Test Competing Writer");
                    competitor.set(writer);
                    writer.start();
                    waitUntil(applicationEx::isWriteActionPending);
                }
                return state.get();
            },
            value -> {
                assertThat(state.get()).as("a write action ran between the read and the write").isEqualTo(value);
                return value;
            }
        ));

        competitor.get().join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        assertThat(reads).hasValue(2);
        assertThat(seen).isEqualTo(1);
    }

    @Test
    public void failedReadReleasesTheLock(Application application) throws Exception {
        CompletableFuture<?> failed = Coroutine.first(ReadWriteLock.<Object, Integer, Integer>apply(
                ignored -> {
                    throw new IllegalStateException("read failed");
                },
                value -> value
            ))
            .runAsync(CoroutineScope.of(application.coroutineContext()), null)
            .toFuture();
        assertThat(failed).failsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Thread writer = new Thread(() -> WriteAction.run(() -> {
        }), "Test Writer");
        writer.start();
        writer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        assertThat(writer.isAlive()).as("write-intent leaked by the failed read").isFalse();
    }

    private static <O> O run(Application application, CoroutineStep<Object, O> step) throws Exception {
        return Coroutine.first(step)
            .runAsync(CoroutineScope.of(application.coroutineContext()), null)
            .toFuture()
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static void waitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!condition.getAsBoolean()) {
            assertThat(System.nanoTime()).as("condition was not reached").isLessThan(deadline);
            Thread.onSpinWait();
        }
    }
}
