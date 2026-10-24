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
package consulo.http.json;

import consulo.http.client.HttpApiHelper;
import consulo.http.client.HttpClientUtil;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface JsonHttpApiHelper {
    /**
     * Fails with {@link HttpJsonSerializationException} when the body cannot be serialized
     */
    CompletableFuture<HttpRequest.Builder> putJson(URI uri, Object body);

    CompletableFuture<HttpRequest.Builder> postJson(URI uri, Object body);

    CompletableFuture<HttpRequest.Builder> sendJson(URI uri, String httpMethod, Object body);

    CompletableFuture<HttpRequest.BodyPublisher> jsonBodyPublisher(String httpMethod, URI uri, Object body);

    HttpRequest.Builder withJsonContent(HttpRequest.Builder builder);

    /**
     * Fails with {@link java.io.IOException}, {@link HttpJsonDeserializationException} or
     * {@link consulo.http.client.EmptyHttpResponseException}
     */
    <T> CompletableFuture<HttpResponse<T>> loadJsonValueByClass(HttpRequest request, Class<T> clazz);

    <T> CompletableFuture<HttpResponse<@Nullable T>> loadOptionalJsonValueByClass(HttpRequest request, Class<T> clazz);

    <T> CompletableFuture<HttpResponse<List<T>>> loadJsonListByClass(HttpRequest request, Class<T> clazz);

    <T> CompletableFuture<HttpResponse<@Nullable List<T>>> loadOptionalJsonListByClass(HttpRequest request, Class<T> clazz);

    static JsonHttpApiHelper create(Logger logger, HttpApiHelper httpHelper, JsonDataSerializer serializer, JsonDataDeserializer deserializer) {
        return new JsonHttpApiHelperImpl(logger, httpHelper,
            serializer, HttpClientUtil.MIME_TYPE_JSON,
            deserializer, HttpClientUtil.MIME_TYPE_JSON);
    }
}
