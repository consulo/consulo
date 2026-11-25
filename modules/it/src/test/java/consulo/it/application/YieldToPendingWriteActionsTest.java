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
import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessUIAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Write actions don't run on the UI thread, so waiting for them must not go through the UI queue: the wait ends when the write
 * action ends, and a busy UI thread doesn't hold it up.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class YieldToPendingWriteActionsTest {
    private static final long TIMEOUT_SECONDS = 10;

    @Test
    public void returnsAtOnceWithoutWriteActionWhileUIThreadIsBusy(Application application) throws Exception {
        waitUntil(() -> !isWriting((ApplicationEx) application));
        HeadlessUIAccess.INSTANCE.giveAsync(() -> null).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        CountDownLatch uiBlocked = new CountDownLatch(1);
        CountDownLatch releaseUI = new CountDownLatch(1);
        CompletableFuture<Object> ui = HeadlessUIAccess.INSTANCE.giveAsync(() -> {
            uiBlocked.countDown();
            await(releaseUI);
            return null;
        });

        ProgressIndicatorBase indicator = new ProgressIndicatorBase();
        try {
            assertThat(uiBlocked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            CompletableFuture<Void> yielded = CompletableFuture.runAsync(() -> ProgressIndicatorUtils.yieldToPendingWriteActions(indicator));
            assertThat(yielded).succeedsWithin(5, TimeUnit.SECONDS);
        }
        finally {
            indicator.cancel();
            releaseUI.countDown();
            ui.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Test
    public void waitsUntilTheRunningWriteActionEnds(Application application) throws Exception {
        ApplicationEx applicationEx = (ApplicationEx) application;

        CountDownLatch writeStarted = new CountDownLatch(1);
        CountDownLatch releaseWrite = new CountDownLatch(1);
        AtomicBoolean writeFinished = new AtomicBoolean();
        Thread writer = new Thread(() -> WriteAction.run(() -> {
            writeStarted.countDown();
            await(releaseWrite);
            writeFinished.set(true);
        }), "Test Writer");

        ProgressIndicatorBase indicator = new ProgressIndicatorBase();
        writer.start();
        try {
            assertThat(writeStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            int followups = applicationEx.writeActionFollowupsSize();
            CompletableFuture<Boolean> yielded = CompletableFuture.supplyAsync(() -> {
                ProgressIndicatorUtils.yieldToPendingWriteActions(indicator);
                return writeFinished.get();
            });

            waitUntil(() -> applicationEx.writeActionFollowupsSize() > followups);
            assertThat(yielded).isNotDone();

            releaseWrite.countDown();
            assertThat(yielded).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS).isEqualTo(Boolean.TRUE);
        }
        finally {
            indicator.cancel();
            releaseWrite.countDown();
            writer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        }
    }

    private static boolean isWriting(ApplicationEx application) {
        return application.isWriteActionPending() || application.isWriteActionInProgress();
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!condition.getAsBoolean()) {
            assertThat(System.nanoTime()).as("condition was not reached").isLessThan(deadline);
            Thread.sleep(5);
        }
    }
}
