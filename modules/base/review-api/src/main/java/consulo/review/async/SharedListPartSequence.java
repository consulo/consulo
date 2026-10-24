// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.async;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.review.util.ComputationOutcome;
import consulo.review.util.FutureUtil;
import consulo.review.util.ItemComputationOutcome;
import consulo.review.util.SequenceComputer;
import consulo.review.util.SequenceItem;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

final class SharedListPartSequence<T> {
    private final SequenceComputer<SequenceItem<List<T>>> myComputer;
    private final Disposable myDisposable = Disposable.newDisposable(SharedListPartSequence.class.getName());
    private final List<SequenceItem<List<T>>> myLoadedParts = new ArrayList<>();
    private @Nullable CompletableFuture<ComputationOutcome<SequenceItem<List<T>>>> myNextComputation;
    private @Nullable CompletableFuture<ComputationOutcome<SequenceItem<List<T>>>> myRunningComputation;
    private boolean myCancelled;

    SharedListPartSequence(Disposable parentDisposable, SequenceComputer<SequenceItem<List<T>>> computer) {
        myComputer = computer;
        Disposer.register(myDisposable, this::doCancel);
        if (!Disposer.tryRegister(parentDisposable, myDisposable)) {
            Disposer.dispose(myDisposable);
        }
    }

    void updateItem(Function<T, @Nullable T> updater) {
        synchronized (this) {
            if (myCancelled) {
                return;
            }
            ListIterator<SequenceItem<List<T>>> partIterator = myLoadedParts.listIterator();
            while (partIterator.hasNext()) {
                SequenceItem<List<T>> part = partIterator.next();
                List<T> list = part.getValue();
                List<T> updatedList = null;
                for (int idx = 0; idx < list.size(); idx++) {
                    T newValue = updater.apply(list.get(idx));
                    if (newValue == null) {
                        continue;
                    }
                    updatedList = new ArrayList<>(list);
                    updatedList.set(idx, newValue);
                    break;
                }
                if (updatedList == null) {
                    continue;
                }
                partIterator.set(SequenceItem.of(updatedList, part.isLast()));
                return;
            }
        }
    }

    SequenceComputer<SequenceItem<List<T>>> getComputer() {
        return new SequenceComputer<>() {
            private int myEmittedParts;

            @Override
            public CompletableFuture<ComputationOutcome<SequenceItem<List<T>>>> computeNext() {
                synchronized (SharedListPartSequence.this) {
                    if (myCancelled) {
                        return CompletableFuture.completedFuture(ComputationOutcome.done());
                    }
                }
                ComputationOutcome<SequenceItem<List<T>>> cached = grabCachedPart();
                if (cached != null) {
                    return CompletableFuture.completedFuture(cached);
                }
                // Nothing cached yet: wait for the next part to be computed and then serve it from the cache, so that
                // `myEmittedParts` is advanced exactly once. Returning the awaited outcome directly would leave `myEmittedParts`
                // behind and re-emit the same part from the cache on the following call.
                return nextComputation().handle((outcome, error) -> {
                    if (error != null && !FutureUtil.isCancellation(error)) {
                        throw new CompletionException(FutureUtil.unwrap(error));
                    }
                    ComputationOutcome<SequenceItem<List<T>>> part = grabCachedPart();
                    // don't cancel the caller
                    return part != null ? part : ComputationOutcome.done();
                });
            }

            private @Nullable ComputationOutcome<SequenceItem<List<T>>> grabCachedPart() {
                synchronized (SharedListPartSequence.this) {
                    if (myCancelled) {
                        return null;
                    }
                    if (myEmittedParts < myLoadedParts.size()) {
                        return ComputationOutcome.item(myLoadedParts.get(myEmittedParts++));
                    }
                    return null;
                }
            }
        };
    }

    private CompletableFuture<ComputationOutcome<SequenceItem<List<T>>>> nextComputation() {
        CompletableFuture<ComputationOutcome<SequenceItem<List<T>>>> computation;
        CompletableFuture<ComputationOutcome<SequenceItem<List<T>>>> stored;
        synchronized (this) {
            CompletableFuture<ComputationOutcome<SequenceItem<List<T>>>> next = myNextComputation;
            if (next != null) {
                return next;
            }
            if (myCancelled) {
                return CompletableFuture.completedFuture(ComputationOutcome.done());
            }
            stored = new CompletableFuture<>();
            myNextComputation = stored;
        }

        try {
            computation = myComputer.computeNext();
        }
        catch (Throwable e) {
            computation = CompletableFuture.failedFuture(e);
        }
        synchronized (this) {
            myRunningComputation = computation;
        }
        computation.whenComplete((result, error) -> {
            if (error != null) {
                synchronized (this) {
                    myRunningComputation = null;
                }
                stored.completeExceptionally(FutureUtil.unwrap(error));
                return;
            }
            synchronized (this) {
                myRunningComputation = null;
                if (myCancelled) {
                    stored.cancel(false);
                    return;
                }
                if (result instanceof ItemComputationOutcome<SequenceItem<List<T>>> item) {
                    myLoadedParts.add(item.value());
                }
                myNextComputation = null;
            }
            stored.complete(result);
        });
        return stored;
    }

    void cancel() {
        Disposer.dispose(myDisposable);
    }

    private void doCancel() {
        CompletableFuture<?> next;
        CompletableFuture<?> running;
        synchronized (this) {
            myCancelled = true;
            next = myNextComputation;
            running = myRunningComputation;
        }
        if (running != null) {
            running.cancel(true);
        }
        if (next != null) {
            next.cancel(true);
        }
    }
}
