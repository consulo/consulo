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

/**
 * GraphQL response body DTO: {@code {data, errors}}.
 *
 * @param <T> the type of the {@code data} field
 */
public final class GraphQLResponseDTO<T> {
    private @Nullable T data;
    private @Nullable List<GraphQLErrorDTO> errors;

    public @Nullable T getData() {
        return data;
    }

    public void setData(@Nullable T data) {
        this.data = data;
    }

    public @Nullable List<GraphQLErrorDTO> getErrors() {
        return errors;
    }

    public void setErrors(@Nullable List<GraphQLErrorDTO> errors) {
        this.errors = errors;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
