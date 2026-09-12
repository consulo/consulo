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

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AppUIExecutor;
import consulo.application.Application;
import consulo.application.AsyncExecutionService;
import consulo.application.NonBlockingReadAction;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.CancellablePromise;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The production {@code AsyncExecutionServiceImpl} lives in {@code consulo.ide.impl}; the commit machinery
 * ({@code DocumentCommitThread#commitAsynchronously}) drives every unsaved edit through
 * {@link ReadAction#nonBlocking}, so a headless run needs a faithful small implementation: expiration,
 * coalescing (a newer submission cancels the pending one with an equal key), unsatisfied constraints
 * re-queue with a delay, and the UI finish hops onto the headless UI thread.
 *
 * @author VISTALL
 */
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
@Singleton
public class HeadlessAsyncExecutionService extends AsyncExecutionService {
    private static final ConcurrentMap<List<Object>, CancellablePromise<?>> ourCoalesced = new ConcurrentHashMap<>();

    private final Application myApplication;

    @Inject
    public HeadlessAsyncExecutionService(Application application) {
        myApplication = application;
    }

    @Override
    protected AppUIExecutor createUIExecutor(ModalityState modalityState) {
        return new HeadlessAppUIExecutor();
    }

    @Override
    protected <T> NonBlockingReadAction<T> buildNonBlockingReadAction(Callable<T> computation) {
        return new HeadlessNonBlockingReadAction<>(computation);
    }

    private class HeadlessAppUIExecutor implements AppUIExecutor {
        @Override
        public AppUIExecutor later() {
            return this;
        }

        @Override
        public AppUIExecutor withDocumentsCommitted(ComponentManager project) {
            return this;
        }

        @Override
        public AppUIExecutor inSmartMode(ComponentManager project) {
            return this;
        }

        @Override
        public AppUIExecutor expireWith(Disposable parentDisposable) {
            return this;
        }

        @Override
        public void execute(Runnable command) {
            myApplication.invokeLater(command);
        }

        @Override
        public <T> CancellablePromise<T> submit(Callable<T> task) {
            AsyncPromise<T> promise = new AsyncPromise<>();
            myApplication.invokeLater(() -> {
                try {
                    promise.setResult(task.call());
                }
                catch (Throwable t) {
                    promise.setError(t);
                }
            });
            return promise;
        }

        @Override
        public CancellablePromise<?> submit(Runnable task) {
            return submit(() -> {
                task.run();
                return null;
            });
        }
    }

    private class HeadlessNonBlockingReadAction<T> implements NonBlockingReadAction<T> {
        private final Callable<T> myComputation;
        private final List<BooleanSupplier> myExpireConditions = new ArrayList<>();
        private final List<BooleanSupplier> myConstraints = new ArrayList<>();
        private @Nullable List<Object> myCoalesceKey;
        private @Nullable Consumer<? super T> myUiThreadAction;
        private @Nullable ProgressIndicator myProgressIndicator;

        HeadlessNonBlockingReadAction(Callable<T> computation) {
            myComputation = computation;
        }

        @Override
        public NonBlockingReadAction<T> inSmartMode(ComponentManager project) {
            myConstraints.add(() -> !DumbService.getInstance((Project) project).isDumb());
            return this;
        }

        @Override
        public NonBlockingReadAction<T> withDocumentsCommitted(ComponentManager project) {
            myConstraints.add(() -> !PsiDocumentManager.getInstance((Project) project).hasUncommitedDocuments());
            return this;
        }

        @Override
        public NonBlockingReadAction<T> expireWhen(BooleanSupplier expireCondition) {
            myExpireConditions.add(expireCondition);
            return this;
        }

        @Override
        public NonBlockingReadAction<T> expireWith(Disposable parentDisposable) {
            myExpireConditions.add(() -> Disposer.isDisposed(parentDisposable));
            return this;
        }

        @Override
        public NonBlockingReadAction<T> wrapProgress(ProgressIndicator progressIndicator) {
            myProgressIndicator = progressIndicator;
            return this;
        }

        @Override
        public NonBlockingReadAction<T> finishOnUiThread(Function<Application, ModalityState> modalityGetter, Consumer<? super T> uiThreadAction) {
            myUiThreadAction = uiThreadAction;
            return this;
        }

        @Override
        public NonBlockingReadAction<T> coalesceBy(Object... equality) {
            myCoalesceKey = List.of(equality);
            return this;
        }

        @Override
        public CancellablePromise<T> submit(Executor backgroundThreadExecutor) {
            AsyncPromise<T> promise = new AsyncPromise<>();
            List<Object> coalesceKey = myCoalesceKey;
            if (coalesceKey != null) {
                CancellablePromise<?> previous = ourCoalesced.put(coalesceKey, promise);
                if (previous != null) {
                    previous.cancel();
                }
                promise.onProcessed(value -> ourCoalesced.remove(coalesceKey, promise));
            }
            backgroundThreadExecutor.execute(() -> attempt(promise, backgroundThreadExecutor));
            return promise;
        }

        private void attempt(AsyncPromise<T> promise, Executor backgroundThreadExecutor) {
            if (promise.isCancelled()) {
                return;
            }
            if (isExpired()) {
                promise.cancel();
                return;
            }
            if (!constraintsSatisfied()) {
                CompletableFuture.delayedExecutor(10, TimeUnit.MILLISECONDS, backgroundThreadExecutor)
                    .execute(() -> attempt(promise, backgroundThreadExecutor));
                return;
            }

            T value;
            try {
                value = compute();
            }
            catch (ProcessCanceledException e) {
                promise.cancel();
                return;
            }
            catch (Throwable t) {
                promise.setError(t);
                return;
            }

            if (promise.isCancelled()) {
                return;
            }
            if (isExpired()) {
                promise.cancel();
                return;
            }

            Consumer<? super T> uiThreadAction = myUiThreadAction;
            if (uiThreadAction == null) {
                promise.setResult(value);
            }
            else {
                myApplication.invokeLater(() -> {
                    if (promise.isCancelled()) {
                        return;
                    }
                    if (isExpired()) {
                        promise.cancel();
                        return;
                    }
                    try {
                        uiThreadAction.accept(value);
                        promise.setResult(value);
                    }
                    catch (Throwable t) {
                        promise.setError(t);
                    }
                });
            }
        }

        @Override
        public T executeSynchronously() throws ProcessCanceledException {
            if (isExpired()) {
                throw new ProcessCanceledException();
            }
            try {
                return compute();
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (RuntimeException | Error e) {
                throw e;
            }
            catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        private T compute() throws Exception {
            ProgressIndicator indicator = myProgressIndicator;
            if (indicator == null) {
                return ReadAction.compute(myComputation::call);
            }

            List<T> result = new ArrayList<>(1);
            List<Exception> failure = new ArrayList<>(1);
            ProgressManager.getInstance().runProcess(() -> {
                try {
                    result.add(ReadAction.compute(myComputation::call));
                }
                catch (Exception e) {
                    failure.add(e);
                }
            }, indicator);
            if (!failure.isEmpty()) {
                throw failure.get(0);
            }
            return result.get(0);
        }

        private boolean isExpired() {
            for (BooleanSupplier condition : myExpireConditions) {
                if (condition.getAsBoolean()) {
                    return true;
                }
            }
            return false;
        }

        private boolean constraintsSatisfied() {
            for (BooleanSupplier constraint : myConstraints) {
                if (!constraint.getAsBoolean()) {
                    return false;
                }
            }
            return true;
        }
    }
}
