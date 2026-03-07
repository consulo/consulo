// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient;

import com.intellij.util.io.HttpRequests;
import jakarta.annotation.Nonnull;

import java.net.http.HttpRequest;
import java.time.Duration;

public class RequestTimeoutConfigurer implements HttpRequestConfigurer {
    protected long getReadTimeoutMillis() {
        return (long) HttpRequests.READ_TIMEOUT;
    }

    @Override
    public final HttpRequest.@Nonnull Builder configure(HttpRequest.@Nonnull Builder builder) {
        return builder.timeout(Duration.ofMillis(getReadTimeoutMillis()));
    }
}
