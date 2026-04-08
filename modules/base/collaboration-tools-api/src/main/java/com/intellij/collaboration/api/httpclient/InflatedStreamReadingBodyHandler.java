// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.util.function.BiFunction;
import java.util.zip.GZIPInputStream;

public final class InflatedStreamReadingBodyHandler<T> implements HttpResponse.BodyHandler<T> {
    private final @Nonnull BiFunction<HttpResponse.ResponseInfo, InputStream, T> streamReader;

    public InflatedStreamReadingBodyHandler(@Nonnull BiFunction<HttpResponse.ResponseInfo, InputStream, T> streamReader) {
        this.streamReader = streamReader;
    }

    @Override
    public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
        HttpResponse.BodySubscriber<InputStream> inputStreamSubscriber = HttpResponse.BodySubscribers.ofInputStream();

        boolean isGzipContent = responseInfo.headers()
            .allValues(HttpClientUtil.CONTENT_ENCODING_HEADER)
            .contains(HttpClientUtil.CONTENT_ENCODING_GZIP);

        HttpResponse.BodySubscriber<InputStream> subscriber;
        if (isGzipContent) {
            subscriber = HttpResponse.BodySubscribers.mapping(inputStreamSubscriber, is -> {
                try {
                    return new GZIPInputStream(is);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        else {
            subscriber = inputStreamSubscriber;
        }

        return HttpResponse.BodySubscribers.mapping(subscriber, is -> streamReader.apply(responseInfo, is));
    }
}
