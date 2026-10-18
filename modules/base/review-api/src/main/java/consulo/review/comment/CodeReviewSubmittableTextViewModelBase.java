// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.comment;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.review.util.ComputedResult;
import consulo.review.util.SingleCoroutineLauncher;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class CodeReviewSubmittableTextViewModelBase implements CodeReviewSubmittableTextViewModel, Disposable {
    private final Project myProject;
    private final SingleCoroutineLauncher myTaskLauncher = new SingleCoroutineLauncher();
    private final MutableObservableValue<String> myText;
    private final MutableObservableValue<@Nullable ComputedResult<?>> myState = MutableObservableValue.of(null);
    private final List<Runnable> myFocusRequestListeners = new CopyOnWriteArrayList<>();

    protected CodeReviewSubmittableTextViewModelBase(Project project, Disposable parentDisposable, String initialText) {
        myProject = project;
        myText = MutableObservableValue.of(initialText);
        Disposer.register(parentDisposable, this);
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public final MutableObservableValue<String> getText() {
        return myText;
    }

    @Override
    public final ObservableValue<@Nullable ComputedResult<?>> getState() {
        return myState;
    }

    @Override
    public final void addFocusRequestListener(Disposable parentDisposable, Runnable listener) {
        myFocusRequestListeners.add(listener);
        Disposer.register(parentDisposable, () -> myFocusRequestListeners.remove(listener));
    }

    @Override
    public final void requestFocus() {
        for (Runnable listener : myFocusRequestListeners) {
            listener.run();
        }
    }

    protected void launchTask(Supplier<? extends CompletableFuture<?>> task) {
        myTaskLauncher.launch(() -> {
            myState.set(ComputedResult.loading());
            CompletableFuture<?> future = task.get();
            future.whenComplete((result, error) -> myState.set(null));
            return future;
        });
    }

    protected void submit(Function<String, ? extends CompletableFuture<?>> submitter) {
        myTaskLauncher.launch(() -> {
            String newText = myText.get();
            myState.set(ComputedResult.loading());
            CompletableFuture<?> future = submitter.apply(newText);
            future.whenComplete((result, error) -> {
                if (error == null) {
                    myState.set(ComputedResult.success(result));
                }
                else if (unwrap(error) instanceof CancellationException) {
                    myState.set(null);
                }
                else {
                    myState.set(ComputedResult.failure(unwrap(error)));
                }
            });
            return future;
        });
    }

    @Override
    public void dispose() {
        myTaskLauncher.cancel();
        myFocusRequestListeners.clear();
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }
}
