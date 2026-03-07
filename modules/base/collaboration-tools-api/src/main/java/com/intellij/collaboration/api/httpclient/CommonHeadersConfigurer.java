// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient;

import jakarta.annotation.Nonnull;

import java.net.http.HttpRequest;
import java.util.Map;

public class CommonHeadersConfigurer implements HttpRequestConfigurer {
    protected @Nonnull Map<String, String> getCommonHeaders() {
        return Map.of(
            HttpClientUtil.ACCEPT_ENCODING_HEADER, HttpClientUtil.CONTENT_ENCODING_GZIP,
            HttpClientUtil.USER_AGENT_HEADER, "JetBrains IDE"
        );
    }

    @Override
    public final HttpRequest.@Nonnull Builder configure(HttpRequest.@Nonnull Builder builder) {
        getCommonHeaders().forEach(builder::header);
        return builder;
    }
}
