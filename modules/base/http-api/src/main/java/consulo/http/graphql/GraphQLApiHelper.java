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
package consulo.http.graphql;

import consulo.http.client.HttpApiHelper;
import consulo.http.json.JsonDataSerializer;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface GraphQLApiHelper {
    /**
     * Fails with {@link consulo.http.json.HttpJsonSerializationException} when the request cannot be serialized
     */
    CompletableFuture<HttpRequest> query(URI uri, Supplier<String> loadQuery, @Nullable Object variablesObject);

    default CompletableFuture<HttpRequest> query(URI uri, Supplier<String> loadQuery) {
        return query(uri, loadQuery, null);
    }

    /**
     * Fails with {@link java.io.IOException}, {@link consulo.http.client.HttpStatusErrorException},
     * {@link consulo.http.json.HttpJsonDeserializationException} or {@link GraphQLErrorException}
     */
    <T> CompletableFuture<HttpResponse<@Nullable T>> loadResponseByClass(HttpRequest request, Class<T> clazz, String... pathFromData);

    static GraphQLApiHelper create(Logger logger, HttpApiHelper httpHelper, JsonDataSerializer serializer, GraphQLDataDeserializer deserializer) {
        return new GraphQLApiHelperImpl(logger, httpHelper, serializer, deserializer);
    }
}
