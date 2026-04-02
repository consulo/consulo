// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api;

import consulo.collaboration.localize.CollaborationToolsLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class HttpStatusErrorException extends RuntimeException {
    private final @Nonnull String requestName;
    private final int statusCode;
    private final @Nullable String body;

    public HttpStatusErrorException(@Nonnull String requestName, int statusCode, @Nullable String body) {
        super("HTTP Request " + requestName + " failed with status code " + statusCode + " and response body: " + body);
        this.requestName = requestName;
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public @Nullable String getBody() {
        return body;
    }

    @Override
    public String getLocalizedMessage() {
        return CollaborationToolsLocalize.httpStatusError(requestName, String.valueOf(statusCode), body).get();
    }
}
