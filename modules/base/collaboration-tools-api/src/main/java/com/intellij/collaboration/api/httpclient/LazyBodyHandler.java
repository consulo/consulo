// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.future.FutureKt;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;

// Look here or elsewhere in this file if you're having trouble fixing "chunked transfer encoding, state: READING_LENGTH" errors ;)
@ApiStatus.Internal
public final class LazyBodyHandler<T> implements HttpResponse.BodyHandler<Function<Continuation<? super T>, Object>> {
    private final @Nonnull HttpResponse.BodyHandler<T> delegate;

    public LazyBodyHandler(@Nonnull HttpResponse.BodyHandler<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public HttpResponse.BodySubscriber<Function<Continuation<? super T>, Object>> apply(HttpResponse.ResponseInfo responseInfo) {
        HttpResponse.BodySubscriber<T> delegateSubscriber = delegate.apply(responseInfo);

        return new HttpResponse.BodySubscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                delegateSubscriber.onSubscribe(subscription);
            }

            @Override
            public void onNext(List<ByteBuffer> item) {
                delegateSubscriber.onNext(item);
            }

            @Override
            public void onError(Throwable throwable) {
                delegateSubscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                delegateSubscriber.onComplete();
            }

            @Override
            public CompletionStage<Function<Continuation<? super T>, Object>> getBody() {
                return CompletableFuture.completedFuture(
                    continuation -> FutureKt.await(delegateSubscriber.getBody(), continuation)
                );
            }
        };
    }
}
