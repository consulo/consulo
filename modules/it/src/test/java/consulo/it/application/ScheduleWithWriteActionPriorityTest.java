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
import consulo.application.internal.ReadTask;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessUIAccess;
import consulo.ui.ModalityState;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A read task scheduled with write action priority starts on its executor right away, or right after the running write action;
 * only its continuation goes to the UI thread.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ScheduleWithWriteActionPriorityTest {
    private static final long TIMEOUT_SECONDS = 10;

    @Test
    public void backgroundPartRunsWhileUIThreadIsBusy() throws Exception {
        HeadlessUIAccess.INSTANCE.giveAsync(() -> null).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        CountDownLatch uiBlocked = new CountDownLatch(1);
        CountDownLatch releaseUI = new CountDownLatch(1);
        CompletableFuture<Object> ui = HeadlessUIAccess.INSTANCE.giveAsync(() -> {
            uiBlocked.countDown();
            await(releaseUI);
            return null;
        });

        CountDownLatch ranInBackground = new CountDownLatch(1);
        AtomicBoolean continuationRan = new AtomicBoolean();
        ProgressIndicatorBase indicator = new ProgressIndicatorBase();
        try {
            assertThat(uiBlocked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            CompletableFuture<?> future = ProgressIndicatorUtils.scheduleWithWriteActionPriority(
                indicator,
                AppExecutorUtil.getAppExecutorService(),
                new ReadTask() {
                    @Override
                    public ReadTask.@Nullable Continuation performInReadAction(ProgressIndicator progressIndicator) {
                        ranInBackground.countDown();
                        return new ReadTask.Continuation(() -> continuationRan.set(true), ModalityState.any());
                    }

                    @Override
                    public void onCanceled(ProgressIndicator progressIndicator) {
                    }
                }
            );

            assertThat(ranInBackground.await(5, TimeUnit.SECONDS)).as("the read task waited for the UI thread").isTrue();
            assertThat(continuationRan).isFalse();

            releaseUI.countDown();
            assertThat(future).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(continuationRan).isTrue();
        }
        finally {
            indicator.cancel();
            releaseUI.countDown();
            ui.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Test
    public void startsAfterTheRunningWriteAction(Application application) throws Exception {
        ApplicationEx applicationEx = (ApplicationEx) application;

        CountDownLatch writeStarted = new CountDownLatch(1);
        CountDownLatch releaseWrite = new CountDownLatch(1);
        AtomicBoolean writeFinished = new AtomicBoolean();
        Thread writer = new Thread(() -> WriteAction.run(() -> {
            writeStarted.countDown();
            await(releaseWrite);
            writeFinished.set(true);
        }), "Test Writer");

        AtomicBoolean startedAfterWrite = new AtomicBoolean();
        CountDownLatch ran = new CountDownLatch(1);
        ProgressIndicatorBase indicator = new ProgressIndicatorBase();
        writer.start();
        try {
            assertThat(writeStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

            int followups = applicationEx.writeActionFollowupsSize();
            ProgressIndicatorUtils.scheduleWithWriteActionPriority(
                indicator,
                AppExecutorUtil.getAppExecutorService(),
                new ReadTask() {
                    @Override
                    public ReadTask.@Nullable Continuation performInReadAction(ProgressIndicator progressIndicator) {
                        startedAfterWrite.set(writeFinished.get());
                        ran.countDown();
                        return null;
                    }

                    @Override
                    public void onCanceled(ProgressIndicator progressIndicator) {
                    }
                }
            );

            waitUntil(() -> applicationEx.writeActionFollowupsSize() > followups);
            assertThat(ran.getCount()).isEqualTo(1);

            releaseWrite.countDown();
            assertThat(ran.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(startedAfterWrite).isTrue();
        }
        finally {
            indicator.cancel();
            releaseWrite.countDown();
            writer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        }
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
