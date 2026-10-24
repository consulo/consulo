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
package consulo.review.util;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @param <T> list item type
 */
final class AsyncIncrementalListComputerImpl<T> implements AsyncIncrementalListComputer<T> {
    private final SequenceComputer<SequenceItem<List<T>>> myComputer;
    private final boolean myLoadAfterDone;
    private final MutableObservableValue<IncrementallyComputedValue<List<T>>> myState =
        MutableObservableValue.of(IncrementallyComputedValue.initial());

    private boolean myNeedMore;
    private boolean myLoading;
    private boolean myStopped;
    private @Nullable CompletableFuture<?> myCurrent;

    /**
     * @param loadAfterDone if this computer will compute more data after the {@code computer} returns {@link DoneComputationOutcome}
     */
    AsyncIncrementalListComputerImpl(Disposable parentDisposable, SequenceComputer<SequenceItem<List<T>>> computer, boolean loadAfterDone) {
        myComputer = computer;
        myLoadAfterDone = loadAfterDone;
        if (!Disposer.tryRegister(parentDisposable, this::cancel)) {
            myStopped = true;
        }
    }

    @Override
    public ObservableValue<IncrementallyComputedValue<List<T>>> getState() {
        return myState;
    }

    @Override
    public void requestMore() {
        synchronized (this) {
            if (myStopped) {
                return;
            }
            myNeedMore = true;
            if (myLoading) {
                return;
            }
            myNeedMore = false;
            myLoading = true;
        }
        loadMore();
    }

    private void loadMore() {
        myState.update(state -> state.withLoading(true));
        CompletableFuture<ComputationOutcome<SequenceItem<List<T>>>> next;
        try {
            next = myComputer.computeNext();
        }
        catch (Exception e) {
            next = CompletableFuture.failedFuture(e);
        }
        boolean stopped;
        synchronized (this) {
            myCurrent = next;
            stopped = myStopped;
        }
        if (stopped) {
            next.cancel(true);
        }
        next.whenComplete(this::onComputed);
    }

    private void onComputed(@Nullable ComputationOutcome<SequenceItem<List<T>>> outcome, @Nullable Throwable error) {
        boolean stopped;
        synchronized (this) {
            myCurrent = null;
            stopped = myStopped;
        }
        if (stopped || FutureUtil.isCancellation(error)) {
            synchronized (this) {
                myStopped = true;
                myLoading = false;
            }
            myState.set(IncrementallyComputedValue.initial());
            return;
        }

        if (error != null) {
            Exception exception = FutureUtil.unwrapException(error);
            myState.update(state -> state.withException(exception).withLoading(false));
        }
        else if (outcome instanceof ItemComputationOutcome<SequenceItem<List<T>>> item) {
            SequenceItem<List<T>> listPart = item.value();
            myState.update(state -> state.withLoading(false).appendPart(listPart.isLast(), currentList -> {
                if (currentList == null) {
                    return listPart.getValue();
                }
                List<T> result = new ArrayList<>(currentList);
                result.addAll(listPart.getValue());
                return result;
            }));
        }
        else {
            myState.update(state -> state.withLoading(false).complete(List::of));
        }

        boolean again;
        synchronized (this) {
            if (myState.get().isComplete() && !myLoadAfterDone) {
                myStopped = true;
            }
            again = myNeedMore && !myStopped;
            myNeedMore = false;
            myLoading = again;
        }
        if (again) {
            loadMore();
        }
    }

    private void cancel() {
        CompletableFuture<?> current;
        synchronized (this) {
            myStopped = true;
            current = myCurrent;
        }
        if (current != null) {
            current.cancel(true);
        }
    }
}
