// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api;

import com.intellij.collaboration.api.httpclient.*;
import com.intellij.collaboration.api.httpclient.response.CancellableWrappingBodyHandler;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.future.FutureKt;

import javax.imageio.ImageIO;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class HttpApiHelperImpl implements HttpApiHelper {
    private final @Nonnull Logger logger;
    private final @Nonnull HttpClientFactory clientFactory;
    private final @Nonnull HttpRequestConfigurer requestConfigurer;

    HttpApiHelperImpl(
        @Nonnull Logger logger,
        @Nonnull HttpClientFactory clientFactory,
        @Nonnull HttpRequestConfigurer requestConfigurer
    ) {
        this.logger = logger;
        this.clientFactory = clientFactory;
        this.requestConfigurer = requestConfigurer;
    }

    @Nonnull
    HttpClient getClient() {
        return clientFactory.createClient();
    }

    @Override
    public HttpRequest.@Nonnull Builder request(@Nonnull String uri) {
        return request(URI.create(uri));
    }

    @Override
    public HttpRequest.@Nonnull Builder request(@Nonnull URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        requestConfigurer.configure(builder);
        return builder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable Object sendAndAwaitCancellable(
        @Nonnull HttpRequest request,
        HttpResponse.@Nonnull BodyHandler<T> bodyHandler,
        @Nonnull Continuation<? super HttpResponse<? extends T>> continuation
    ) {
        // Suspend function implementation - requires Kotlin coroutine infrastructure to invoke.
        // This mirrors the Kotlin implementation that uses CancellableWrappingBodyHandler + LazyBodyHandler + await().
        CancellableWrappingBodyHandler<T> cancellableBodyHandler =
            new CancellableWrappingBodyHandler<>(new LazyBodyHandler<>(bodyHandler));
        logger.debug(HttpApiHelper.logName(request));
        // The actual async send + await is done via Kotlin coroutine machinery.
        // Java callers should use the CompletableFuture-based sendAsync directly.
        var future = getClient().sendAsync(request, cancellableBodyHandler);
        return FutureKt.await(future, continuation);
    }

    @Override
    public @Nullable Object sendAndAwaitCancellable(
        @Nonnull HttpRequest request,
        @Nonnull Continuation<? super HttpResponse<? extends kotlin.Unit>> continuation
    ) {
        var handler = HttpClientUtil.inflateAndReadWithErrorHandlingAndLogging(logger, request, (reader, responseInfo) -> {
            return kotlin.Unit.INSTANCE;
        });
        return sendAndAwaitCancellable(request, handler, continuation);
    }

    @Override
    public @Nullable Object loadImage(
        @Nonnull HttpRequest request,
        @Nonnull Continuation<? super HttpResponse<? extends Image>> continuation
    ) {
        String requestLogName = HttpApiHelper.logName(request);
        InflatedStreamReadingBodyHandler<Image> bodyHandler = new InflatedStreamReadingBodyHandler<>((responseInfo, stream) -> {
            HttpClientUtil.checkStatusCodeWithLogging(logger, requestLogName, responseInfo.statusCode(), stream);
            return ImageIO.read(stream);
        });
        return sendAndAwaitCancellable(request, bodyHandler, continuation);
    }
}
