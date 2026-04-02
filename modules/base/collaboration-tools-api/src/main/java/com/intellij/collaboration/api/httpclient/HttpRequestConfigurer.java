// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient;

import jakarta.annotation.Nonnull;

import java.net.http.HttpRequest;

public interface HttpRequestConfigurer {
    HttpRequest.@Nonnull Builder configure(HttpRequest.@Nonnull Builder builder);
}
