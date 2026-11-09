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

import java.util.List;
import java.util.Map;

/**
 * A response whose body was read - what is left of an exchange once its connection is closed.
 *
 * @author VISTALL
 * @since 2026-11-09
 */
public record HttpApiResponse<T extends @Nullable Object>(String url, int statusCode, Map<String, List<String>> headers, T body) {
    /**
     * The first value of the header, the name compared ignoring case.
     */
    public @Nullable String header(String name) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey()) && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    public <R extends @Nullable Object> HttpApiResponse<R> withBody(R newBody) {
        return new HttpApiResponse<>(url, statusCode, headers, newBody);
    }
}
