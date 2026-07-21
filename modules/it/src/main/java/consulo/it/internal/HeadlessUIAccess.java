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
package consulo.it.internal;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.ui.ModalityState;
import consulo.ui.impl.BaseUIAccess;
import consulo.ui.impl.SingleUIAccessScheduler;
import consulo.util.concurrent.AsyncResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * A single-thread "UI thread" for headless integration tests. All {@code give}/{@code giveAsync}
 * work is scheduled onto one dedicated thread, which is the answer to {@code isUIThread()}.
 *
 * @author VISTALL
 */
public final class HeadlessUIAccess extends BaseUIAccess {
    public static final HeadlessUIAccess INSTANCE = new HeadlessUIAccess();

    private volatile Thread myThread;

    private final ExecutorService myExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Headless UI Thread");
        thread.setDaemon(true);
        myThread = thread;
        return thread;
    });

    public boolean isUIThread() {
        return Thread.currentThread() == myThread;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public <T> CompletableFuture<T> giveAsync(Supplier<T> supplier) {
        CompletableFuture<T> result = new CompletableFuture<>();
        myExecutor.execute(() -> {
            try {
                result.complete(supplier.get());
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    @Override
    public <T> AsyncResult<T> give(Supplier<T> supplier) {
        AsyncResult<T> result = AsyncResult.undefined();
        myExecutor.execute(() -> {
            try {
                result.setDone(supplier.get());
            }
            catch (Throwable e) {
                result.rejectWithThrowable(e);
            }
        });
        return result;
    }

    @Override
    protected SingleUIAccessScheduler createScheduler() {
        ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);
        return new SingleUIAccessScheduler(this, concurrency.getScheduledExecutorService()) {
            @Override
            public void runWithModalityState(Runnable runnable, ModalityState modalityState) {
                Application.get().invokeLater(runnable, modalityState);
            }
        };
    }
}
