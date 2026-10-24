// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.http.client;

import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

final class BodyOverrideHttpResponse<T extends @Nullable Object> implements HttpResponse<T> {
    private final HttpResponse<?> myOriginal;
    private final T myBody;

    BodyOverrideHttpResponse(HttpResponse<?> original, T body) {
        myOriginal = original;
        myBody = body;
    }

    @Override
    public HttpRequest request() {
        return myOriginal.request();
    }

    @Override
    public URI uri() {
        return myOriginal.uri();
    }

    @Override
    public HttpClient.Version version() {
        return myOriginal.version();
    }

    @Override
    public int statusCode() {
        return myOriginal.statusCode();
    }

    @Override
    public HttpHeaders headers() {
        return myOriginal.headers();
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return myOriginal.sslSession();
    }

    /**
     * body() of previousResponse is always null according to {@link HttpResponse#body()} docs
     */
    @Override
    @SuppressWarnings("unchecked")
    public Optional<HttpResponse<T>> previousResponse() {
        return myOriginal.previousResponse().map(previous -> (HttpResponse<T>) HttpClientUtil.withNewBody(previous, null));
    }

    @Override
    public T body() {
        return myBody;
    }
}
