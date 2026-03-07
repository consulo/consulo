// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient;

import jakarta.annotation.Nonnull;

import java.net.http.HttpRequest;
import java.util.List;

public final class CompoundRequestConfigurer implements HttpRequestConfigurer {
    private final @Nonnull List<HttpRequestConfigurer> configurers;

    public CompoundRequestConfigurer(@Nonnull List<HttpRequestConfigurer> configurers) {
        this.configurers = configurers;
    }

    public CompoundRequestConfigurer(@Nonnull HttpRequestConfigurer... configurers) {
        this(List.of(configurers));
    }

    @Override
    public HttpRequest.@Nonnull Builder configure(HttpRequest.@Nonnull Builder builder) {
        for (HttpRequestConfigurer configurer : configurers) {
            configurer.configure(builder);
        }
        return builder;
    }
}
