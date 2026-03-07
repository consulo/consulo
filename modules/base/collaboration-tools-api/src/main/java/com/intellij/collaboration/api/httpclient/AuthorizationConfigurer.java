// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient;

import com.intellij.util.io.HttpSecurityUtil;
import jakarta.annotation.Nonnull;

import java.net.http.HttpRequest;

/**
 * Injects Authorization header
 */
public abstract class AuthorizationConfigurer implements HttpRequestConfigurer {
    protected abstract @Nonnull String getAuthorizationHeaderValue();

    @Override
    public final HttpRequest.@Nonnull Builder configure(HttpRequest.@Nonnull Builder builder) {
        return builder.header(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, getAuthorizationHeaderValue());
    }
}
