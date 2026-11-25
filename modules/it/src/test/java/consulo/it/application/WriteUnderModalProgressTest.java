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
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.progress.ProgressManager;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessUIAccess;
import consulo.localize.LocalizeValue;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A write action run by a modal task must neither deadlock against the UI thread that shows the modal progress nor freeze it.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class WriteUnderModalProgressTest {
    private static final long TIMEOUT_SECONDS = 20;

    @Test
    public void writeInsideSynchronousModalProgress() {
        CompletableFuture<Boolean> result = HeadlessUIAccess.INSTANCE.giveAsync(
            () -> ProgressManager.getInstance().runProcessWithProgressSynchronously(
                () -> WriteAction.run(() -> {
                }),
                LocalizeValue.of("Write under modal progress"),
                false,
                null
            )
        );

        assertThat(result).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS).isEqualTo(Boolean.TRUE);
    }

    @Test
    public void uiThreadAnswersWhileModalTaskWrites(Application application, ProgressBuilderFactory progressBuilderFactory)
        throws Exception {
        CountDownLatch writeStarted = new CountDownLatch(1);
        CountDownLatch releaseWrite = new CountDownLatch(1);

        CompletableFuture<String> future = HeadlessUIAccess.INSTANCE.giveAsync(
            () -> progressBuilderFactory.newProgressBuilder(null, LocalizeValue.of("Write under modal progress"))
                .modal()
                .execute(HeadlessUIAccess.INSTANCE, () -> Coroutine.first(WriteLock.<Object, String>apply(input -> {
                    writeStarted.countDown();
                    await(releaseWrite);
                    return "written";
                })))
        ).thenCompose(execution -> execution);

        try {
            assertThat(writeStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(((ApplicationEx) application).isWriteActionInProgress()).isTrue();

            assertThat(HeadlessUIAccess.INSTANCE.giveAsync(() -> Boolean.TRUE)).succeedsWithin(5, TimeUnit.SECONDS);
        }
        finally {
            releaseWrite.countDown();
        }

        assertThat(future).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS).isEqualTo("written");
    }

    @Test
    public void modalProgressStartedFromBackgroundThread(ProgressBuilderFactory progressBuilderFactory) {
        for (int i = 0; i < 20; i++) {
            CompletableFuture<String> future = progressBuilderFactory.newProgressBuilder(null, LocalizeValue.of("Modal from background"))
                .modal()
                .execute(HeadlessUIAccess.INSTANCE, () -> Coroutine.first(CodeExecution.<Object, String>apply(input -> "done")));

            assertThat(future).succeedsWithin(TIMEOUT_SECONDS, TimeUnit.SECONDS).isEqualTo("done");
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
}
