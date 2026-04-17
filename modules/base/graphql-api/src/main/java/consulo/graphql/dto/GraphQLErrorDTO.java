/*
 * Copyright 2013-2025 consulo.io
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
package consulo.graphql.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A single GraphQL error entry.
 */
public final class GraphQLErrorDTO {
    private @Nullable String message;
    private @Nullable List<Map<String, Object>> locations;
    private @Nullable List<Object> path;
    private @Nullable Map<String, Object> extensions;

    public @Nullable String getMessage() {
        return message;
    }

    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    public @Nullable List<Map<String, Object>> getLocations() {
        return locations;
    }

    public void setLocations(@Nullable List<Map<String, Object>> locations) {
        this.locations = locations;
    }

    public @Nullable List<Object> getPath() {
        return path;
    }

    public void setPath(@Nullable List<Object> path) {
        this.path = path;
    }

    public @Nullable Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(@Nullable Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        return message != null ? message : "Unknown GraphQL error";
    }
}
