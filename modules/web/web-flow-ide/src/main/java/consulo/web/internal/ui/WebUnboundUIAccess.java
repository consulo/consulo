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
package consulo.web.internal.ui;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.clipboard.Clipboard;
import consulo.ui.impl.clipboard.MemoryClipboard;
import consulo.ui.impl.BaseUIAccess;
import consulo.ui.impl.SingleUIAccessScheduler;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Answers for the application while no session is current. Asking a browser frontend for "the" ui is a question
 * with no answer - which is a bug in the caller, not something to work around here - so the work still runs, on
 * a thread of its own, and every use is reported with the stack that asked.
 * <p/>
 * Nothing it runs reaches a browser. It exists so that a caller which lost track of its ui fails loudly and in
 * one place, rather than throwing out of whatever it happened to be doing.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public class WebUnboundUIAccess extends BaseUIAccess implements UIAccess {
    private static final Logger LOG = Logger.getInstance(WebUnboundUIAccess.class);

    private volatile @Nullable ExecutorService myExecutor;

    /**
     * Resolved on first use - the application may not be started yet when the first browser request arrives.
     */
    private ExecutorService executor() {
        ExecutorService executor = myExecutor;
        if (executor != null) {
            return executor;
        }

        synchronized (this) {
            executor = myExecutor;
            if (executor == null) {
                myExecutor = executor = Application.get()
                    .getInstance(ApplicationConcurrency.class)
                    .createSequentialApplicationPoolExecutor("WebUnboundUIAccess");
            }
        }

        return executor;
    }

    private void report() {
        LOG.error("UI access without a session - the caller must hold the ui of its project", new Throwable());
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public <T> CompletableFuture<T> giveAsync(Supplier<T> supplier) {
        report();

        CompletableFuture<T> result = new CompletableFuture<>();
        executor().execute(() -> {
            try {
                result.complete(supplier.get());
            }
            catch (Throwable e) {
                LOG.error(e);
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    @Override
    public void give(Runnable runnable) {
        report();

        executor().execute(runnable);
    }

    @Override
    public void giveAndWait(Runnable runnable) {
        report();

        runnable.run();
    }

    @Override
    protected Clipboard createClipboard() {
        return new MemoryClipboard();
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
