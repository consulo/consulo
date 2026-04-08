// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api;

import com.intellij.collaboration.api.httpclient.*;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlin.coroutines.Continuation;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@ApiStatus.Experimental
public interface HttpApiHelper {
    /**
     * Creates a request builder from the given URI String.
     */
    HttpRequest.@Nonnull Builder request(@Nonnull String uri);

    /**
     * Creates a request builder from the given URI.
     */
    HttpRequest.@Nonnull Builder request(@Nonnull URI uri);

    /**
     * Sends the given request and awaits a response in a suspended cancellable way.
     * The body handler is used to fully handle the body, no additional handling is done by this method.
     */
    <T> @Nullable Object sendAndAwaitCancellable(
        @Nonnull HttpRequest request,
        HttpResponse.@Nonnull BodyHandler<T> bodyHandler,
        @Nonnull Continuation<? super HttpResponse<? extends T>> continuation
    );

    /**
     * Sends the given request and awaits a response in a suspended cancellable way.
     * Different from the overloaded function with a {@link java.net.http.HttpResponse.BodyHandler} parameter,
     * this function provides an inflating, logging body handler with no additional mapping of the body.
     */
    @Nullable
    Object sendAndAwaitCancellable(
        @Nonnull HttpRequest request,
        @Nonnull Continuation<? super HttpResponse<? extends kotlin.Unit>> continuation
    );

    /**
     * Sends the given request and awaits a response in a suspended cancellable way.
     * Maps the body of the response to an {@link Image} object.
     */
    @Nullable
    Object loadImage(
        @Nonnull HttpRequest request,
        @Nonnull Continuation<? super HttpResponse<? extends Image>> continuation
    );

    @Nonnull
    static String logName(@Nonnull HttpRequest request) {
        return "Request " + request.method() + " " + request.uri();
    }

    @Nonnull
    static HttpApiHelper create() {
        return create(Logger.getInstance(HttpApiHelper.class));
    }

    @Nonnull
    static HttpApiHelper create(@Nonnull Logger logger) {
        return create(logger, new HttpClientFactoryBase());
    }

    @Nonnull
    static HttpApiHelper create(@Nonnull Logger logger, @Nonnull HttpClientFactory clientFactory) {
        return create(logger, clientFactory, new CompoundRequestConfigurer(List.of(
            new RequestTimeoutConfigurer(),
            new CommonHeadersConfigurer()
        )));
    }

    @Nonnull
    static HttpApiHelper create(
        @Nonnull Logger logger,
        @Nonnull HttpClientFactory clientFactory,
        @Nonnull HttpRequestConfigurer requestConfigurer
    ) {
        return new HttpApiHelperImpl(logger, clientFactory, requestConfigurer);
    }
}
