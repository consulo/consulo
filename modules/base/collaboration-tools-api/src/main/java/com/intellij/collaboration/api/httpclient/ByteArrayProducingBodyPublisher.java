// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient;

import jakarta.annotation.Nonnull;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

public final class ByteArrayProducingBodyPublisher implements HttpRequest.BodyPublisher {
    private final @Nonnull Supplier<byte[]> producer;

    public ByteArrayProducingBodyPublisher(@Nonnull Supplier<byte[]> producer) {
        this.producer = producer;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        HttpRequest.BodyPublishers.ofByteArray(producer.get()).subscribe(subscriber);
    }

    @Override
    public long contentLength() {
        return -1;
    }
}
