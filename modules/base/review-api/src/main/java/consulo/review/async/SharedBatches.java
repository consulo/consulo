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

import consulo.review.util.FutureUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

final class SharedBatches<T> {
    private final List<List<T>> myPages = new ArrayList<>();
    private final List<BatchesSubscription<T>> mySubscriptions = new ArrayList<>();
    private final CompletableFuture<Object> myCompletion = new CompletableFuture<>();
    private CompletableFuture<?> myProduction = CompletableFuture.completedFuture(null);
    private boolean myStarted;

    void startIfNeeded(Function<Consumer<List<T>>, CompletableFuture<?>> batches) {
        synchronized (this) {
            if (myStarted || myCompletion.isDone()) {
                return;
            }
            myStarted = true;
        }
        CompletableFuture<?> production;
        try {
            production = batches.apply(this::onBatch);
        }
        catch (Throwable e) {
            production = CompletableFuture.failedFuture(e);
        }
        boolean cancelled;
        synchronized (this) {
            myProduction = production;
            cancelled = myCompletion.isDone();
        }
        if (cancelled) {
            production.cancel(true);
            return;
        }
        production.whenComplete((result, error) -> {
            if (error != null) {
                myCompletion.completeExceptionally(FutureUtil.unwrap(error));
            }
            else {
                myCompletion.complete(null);
            }
        });
    }

    CompletableFuture<?> subscribe(Consumer<List<T>> consumer, Runnable onUnsubscribe) {
        BatchesSubscription<T> subscription = new BatchesSubscription<>(this, consumer);
        synchronized (this) {
            mySubscriptions.add(subscription);
        }
        subscription.drain();

        CompletableFuture<Object> result = new CompletableFuture<>();
        myCompletion.whenComplete((value, error) -> {
            subscription.drain();
            if (error != null) {
                result.completeExceptionally(FutureUtil.unwrap(error));
            }
            else {
                result.complete(null);
            }
        });
        result.whenComplete((value, error) -> {
            synchronized (this) {
                mySubscriptions.remove(subscription);
            }
            onUnsubscribe.run();
        });
        return result;
    }

    synchronized boolean hasSubscribers() {
        return !mySubscriptions.isEmpty();
    }

    synchronized List<T> pagesFrom(int index) {
        List<T> batch = new ArrayList<>();
        for (int i = index; i < myPages.size(); i++) {
            batch.addAll(myPages.get(i));
        }
        return batch;
    }

    synchronized int pagesCount() {
        return myPages.size();
    }

    private void onBatch(List<T> batch) {
        List<BatchesSubscription<T>> subscriptions;
        synchronized (this) {
            if (myCompletion.isDone()) {
                return;
            }
            myPages.add(List.copyOf(batch));
            subscriptions = List.copyOf(mySubscriptions);
        }
        for (BatchesSubscription<T> subscription : subscriptions) {
            subscription.drain();
        }
    }

    void cancel() {
        CompletableFuture<?> production;
        synchronized (this) {
            production = myProduction;
        }
        production.cancel(true);
        myCompletion.completeExceptionally(new CancellationException());
    }
}
