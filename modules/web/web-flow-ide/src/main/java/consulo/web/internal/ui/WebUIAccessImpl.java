/*
 * Copyright 2013-2016 consulo.io
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

import com.vaadin.flow.component.UI;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.component.store.impl.internal.ComponentStoreImpl;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.impl.BaseUIAccess;
import consulo.ui.impl.SingleUIAccessScheduler;
import consulo.util.concurrent.AsyncResult;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2016-06-16
 */
public class WebUIAccessImpl extends BaseUIAccess implements UIAccess {
    private static final Logger LOG = Logger.getInstance(WebUIAccessImpl.class);

    private final UI myUI;

    /**
     * Vaadin purges the session access queue on whichever thread calls {@link UI#access}, so calling it directly
     * from a thread that holds the read lock lets a queued write action block against that very thread. Both
     * give methods are asynchronous by contract, so the call is handed to a single background thread instead -
     * single, because the queue order still has to be preserved.
     */
    private volatile @Nullable ExecutorService myDispatcher;

    public WebUIAccessImpl(UI ui) {
        myUI = ui;
    }

    /**
     * Resolved on first use rather than in the constructor - the root layout is built as soon as the browser
     * asks for the page, which can be long before the platform has started, and reaching for the application
     * that early failed the whole route with an internal server error instead of showing the frame loading.
     */
    private ExecutorService dispatcher() {
        ExecutorService dispatcher = myDispatcher;
        if (dispatcher != null) {
            return dispatcher;
        }

        synchronized (this) {
            dispatcher = myDispatcher;
            if (dispatcher == null) {
                myDispatcher = dispatcher = Application.get()
                    .getInstance(ApplicationConcurrency.class)
                    .createSequentialApplicationPoolExecutor("WebUIAccess dispatcher");
            }
        }

        return dispatcher;
    }

    @Override
    public boolean isValid() {
        return myUI.isAttached() && myUI.getSession() != null;
    }

    @Override
    public <T> CompletableFuture<T> giveAsync(Supplier<T> supplier) {
        CompletableFuture<T> result = new CompletableFuture<>();
        if (isValid()) {
            dispatcher().execute(() -> {
                try {
                    myUI.access(() -> {
                        try {
                            result.complete(supplier.get());
                        }
                        catch (Throwable e) {
                            LOG.error(e);
                            result.completeExceptionally(e);
                        }
                    });
                }
                catch (Throwable e) {
                    // the ui can detach between the isValid check and the access call - the future must not be
                    // left incomplete then, a caller waiting on it would park forever
                    LOG.warn("giveAsync lost, ui detached mid-flight", e);
                    result.completeExceptionally(e);
                }
            });
        }
        else {
            result.completeExceptionally(new Exception("ui detached"));
        }
        return result;
    }

    @Override
    public <T> AsyncResult<T> give(Supplier<T> supplier) {
        AsyncResult<T> result = AsyncResult.undefined();
        if (isValid()) {
            dispatcher().execute(() -> {
                try {
                    myUI.access(() -> {
                        try {
                            result.setDone(supplier.get());
                        }
                        catch (Throwable e) {
                            LOG.error(e);
                            result.rejectWithThrowable(e);
                        }
                    });
                }
                catch (Throwable e) {
                    // the ui can detach between the isValid check and the access call - the result must not be
                    // left incomplete then, a caller waiting on it would park forever
                    LOG.warn("give lost, ui detached mid-flight", e);
                    result.rejectWithThrowable(e);
                }
            });
        }
        else {
            // answering an empty result rather than a value the caller asked for is already a loss, and a
            // sequence that quietly carries on with nothing is impossible to tell from one that never ran
            LOG.warn("give skipped, ui detached", new Throwable());
            result.setDone();
        }
        return result;
    }

    @Override
    public void giveAndWait(Runnable runnable) {
        ComponentStoreImpl.assertIfInsideSavingSession();

        if (isValid()) {
            myUI.accessSynchronously(runnable::run);
        }
        else {
            // dropping the work and returning as if it ran leaves whatever drives the caller - opening a
            // project runs through here - stopped with nothing in the log to say so
            LOG.warn("giveAndWait skipped, ui detached", new Throwable());
        }
    }

    public UI getUI() {
        return myUI;
    }

    @Override
    protected SingleUIAccessScheduler createScheduler() {
        Application application = Application.get();
        ApplicationConcurrency concurrency = application.getInstance(ApplicationConcurrency.class);
        return new SingleUIAccessScheduler(this, concurrency.getScheduledExecutorService()) {
            @Override
            public void runWithModalityState(Runnable runnable, ModalityState modalityState) {
                Application.get().invokeLater(runnable, modalityState);
            }
        };
    }
}