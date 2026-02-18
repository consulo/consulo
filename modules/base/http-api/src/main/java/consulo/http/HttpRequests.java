/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.http;

import consulo.application.Application;
import jakarta.annotation.Nonnull;

/**
 * Handy class for reading data from HTTP connections with built-in support for HTTP redirects and gzipped content and automatic cleanup.
 * Usage: <pre>{@code
 * int firstByte = HttpRequests.request(url).connect(new HttpRequests.RequestProcessor<Integer>() {
 *   public Integer process(@NotNull Request request) throws IOException {
 *     return request.getInputStream().read();
 *   }
 * });
 * }</pre>
 */
@Deprecated
public final class HttpRequests {
    private HttpRequests() {
    }

    @Nonnull
    @Deprecated
    public static HttpRequestBuilder request(@Nonnull String url) {
        HttpRequestBuilderFactory factory = Application.get().getInstance(HttpRequestBuilderFactory.class);
        return factory.newBuilder(url, HttpMethod.GET);
    }
}