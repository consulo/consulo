// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient.response;

import jakarta.annotation.Nonnull;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.locks.ReentrantLock;

public final class CancellableWrappingBodyHandler<T> implements HttpResponse.BodyHandler<T> {
  private final @Nonnull HttpResponse.BodyHandler<T> handler;

  private volatile boolean cancelled = false;
  private volatile Flow.Subscription currentSubscription = null;

  private final ReentrantLock lock = new ReentrantLock();

  public CancellableWrappingBodyHandler(@Nonnull HttpResponse.BodyHandler<T> handler) {
    this.handler = handler;
  }

  @Override
  public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
    return new SubscriberWrapper<>(handler.apply(responseInfo));
  }

  public void cancel() {
    lock.lock();
    try {
      cancelled = true;
      Flow.Subscription sub = currentSubscription;
      if (sub != null) {
        sub.cancel();
      }
    }
    finally {
      lock.unlock();
    }
  }

  private final class SubscriberWrapper<O> implements HttpResponse.BodySubscriber<O> {
    private final @Nonnull HttpResponse.BodySubscriber<O> subscriber;

    SubscriberWrapper(@Nonnull HttpResponse.BodySubscriber<O> subscriber) {
      this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      subscriber.onSubscribe(subscription);
      lock.lock();
      try {
        if (cancelled) subscription.cancel();
        currentSubscription = subscription;
      }
      finally {
        lock.unlock();
      }
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
      subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
      subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
      subscriber.onComplete();
    }

    @Override
    public CompletionStage<O> getBody() {
      return subscriber.getBody();
    }
  }
}
