// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.json;

import jakarta.annotation.Nonnull;

public final class HttpJsonDeserializationException extends RuntimeException {
    public HttpJsonDeserializationException(@Nonnull String requestName, @Nonnull Throwable cause) {
        super("Deserialization of " + requestName + " response to JSON failed - " + cause.getLocalizedMessage(), cause);
    }
}
