// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.httpclient;

import com.intellij.execution.process.ProcessIOExecutorService;
import com.intellij.util.io.HttpRequests;
import jakarta.annotation.Nonnull;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

public class HttpClientFactoryBase implements HttpClientFactory {
    protected boolean getUseProxy() {
        return true;
    }

    protected long getConnectionTimeoutMillis() {
        return (long) HttpRequests.CONNECTION_TIMEOUT;
    }

    @Override
    public @Nonnull HttpClient createClient() {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .proxy(getUseProxy() ? ProxySelector.getDefault() : HttpClient.Builder.NO_PROXY)
            .connectTimeout(Duration.ofMillis(getConnectionTimeoutMillis()))
            .executor(ProcessIOExecutorService.INSTANCE)
            .build();
    }
}
