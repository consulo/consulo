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
import consulo.application.internal.ApplicationEx;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessUIAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Input on the UI thread never waits for a background write action: it runs at once holding write-intent, or - while a
 * background write action runs - it is collected and runs right after it. Background write actions wait while input keeps
 * coming.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class InputDuringBackgroundWriteTest {
    private static final long TIMEOUT_SECONDS = 20;

    @Test
    public void inputRunsAtOnceHoldingWriteIntent(Application application) throws Exception {
        ApplicationEx applicationEx = (ApplicationEx) application;
        settle(applicationEx);

        AtomicBoolean writeThreadInside = new AtomicBoolean();
        CompletableFuture<Boolean> doneAtOnce = HeadlessUIAccess.INSTANCE.giveAsync(
            () -> applicationEx.runInput(HeadlessUIAccess.INSTANCE, () -> {
                writeThreadInside.set(applicationEx.isWriteThread());
                WriteAction.run(() -> {
                });
            }).isDone()
        );

        assertThat(doneAtOnce).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS).isEqualTo(Boolean.TRUE);
        assertThat(writeThreadInside).isTrue();
    }

    @Test
    public void inputDuringBackgroundWriteRunsAfterItInOrder(Application application) throws Exception {
        ApplicationEx applicationEx = (ApplicationEx) application;
        settle(applicationEx);

        CountDownLatch writeStarted = new CountDownLatch(1);
        CountDownLatch releaseWrite = new CountDownLatch(1);
        AtomicBoolean writeFinished = new AtomicBoolean();
        Thread writer = new Thread(() -> WriteAction.run(() -> {
            writeStarted.countDown();
            await(releaseWrite);
            writeFinished.set(true);
        }), "Test Background Writer");
        writer.start();

        List<String> applied = new CopyOnWriteArrayList<>();
        try {
            assertThat(writeStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            CompletableFuture<CompletableFuture<?>> input = HeadlessUIAccess.INSTANCE.<CompletableFuture<?>>giveAsync(() -> {
                CompletableFuture<?> first = applicationEx.runInput(HeadlessUIAccess.INSTANCE, () -> {
                    applied.add("a:" + writeFinished.get());
                    WriteAction.run(() -> applied.add("write"));
                });
                return first.thenApply(ignored -> (Object) null)
                    .thenCompose(ignored -> applicationEx.runInput(HeadlessUIAccess.INSTANCE, () -> applied.add("b"))
                        .thenApply(result -> (Object) null));
            });
            CompletableFuture<?> inputDone = input.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThat(inputDone).isNotDone();
            assertThat(HeadlessUIAccess.INSTANCE.giveAsync(() -> Boolean.TRUE)).succeedsWithin(5, TimeUnit.SECONDS);
            assertThat(applied).isEmpty();

            releaseWrite.countDown();
            assertThat(inputDone).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(applied).containsExactly("a:true", "write", "b");
        }
        finally {
            releaseWrite.countDown();
            writer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        }
    }

    @Test
    public void inputGivenWhileInputRunsWaitsForIt(Application application) throws Exception {
        ApplicationEx applicationEx = (ApplicationEx) application;
        settle(applicationEx);

        List<String> order = new CopyOnWriteArrayList<>();
        CompletableFuture<CompletableFuture<?>> nested = new CompletableFuture<>();
        CompletableFuture<Boolean> outerDone = HeadlessUIAccess.INSTANCE.giveAsync(
            () -> applicationEx.runInput(HeadlessUIAccess.INSTANCE, () -> {
                order.add("outer:" + applicationEx.isInsideInput());
                nested.complete(applicationEx.runInput(HeadlessUIAccess.INSTANCE, () -> order.add("nested")));
                order.add("outer-end");
            }).isDone()
        );

        assertThat(outerDone).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS).isEqualTo(Boolean.TRUE);
        assertThat(nested.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(order).containsExactly("outer:true", "outer-end", "nested");
        assertThat(applicationEx.isInsideInput()).isFalse();
    }

    @Test
    public void backgroundWriteWaitsWhileInputKeepsComing(Application application) throws Exception {
        ApplicationEx applicationEx = (ApplicationEx) application;
        settle(applicationEx);

        assertThat(HeadlessUIAccess.INSTANCE.giveAsync(() -> applicationEx.runInput(HeadlessUIAccess.INSTANCE, () -> {
        }).isDone())).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS).isEqualTo(Boolean.TRUE);

        long requested = System.nanoTime();
        AtomicLong startedAfter = new AtomicLong(-1);
        Thread writer = new Thread(
            () -> WriteAction.run(() -> startedAfter.set(System.nanoTime() - requested)),
            "Test Background Writer"
        );
        writer.start();
        writer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

        assertThat(startedAfter.get()).as("the background write did not run").isGreaterThanOrEqualTo(0);
        assertThat(TimeUnit.NANOSECONDS.toMillis(startedAfter.get()))
            .as("the background write did not wait for the input pause")
            .isGreaterThanOrEqualTo(300);
    }

    private static void settle(ApplicationEx application) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (application.isWriteActionPending() || application.isWriteActionInProgress()) {
            assertThat(System.nanoTime()).as("write actions never settled").isLessThan(deadline);
            Thread.sleep(10);
        }
        HeadlessUIAccess.INSTANCE.giveAsync(() -> null).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Thread.sleep(800);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
