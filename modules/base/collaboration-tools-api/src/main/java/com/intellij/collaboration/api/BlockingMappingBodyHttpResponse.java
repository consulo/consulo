// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.net.ssl.SSLSession;

final class BlockingMappingBodyHttpResponse<T, R> implements HttpResponse<R> {
    private final @Nonnull HttpResponse<T> response;
    private final @Nullable R body;

    BlockingMappingBodyHttpResponse(@Nonnull HttpResponse<T> response, @Nullable R body) {
        this.response = response;
        this.body = body;
    }

    @Override
    public int statusCode() {
        return response.statusCode();
    }

    @Override
    public HttpRequest request() {
        return response.request();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<HttpResponse<R>> previousResponse() {
        return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return response.sslSession();
    }

    @Override
    public URI uri() {
        return response.uri();
    }

    @Override
    public HttpClient.Version version() {
        return response.version();
    }

    @Override
    public @Nullable R body() {
        return body;
    }
}
