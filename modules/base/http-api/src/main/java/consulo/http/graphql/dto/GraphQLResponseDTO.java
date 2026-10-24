// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.http.graphql.dto;

import consulo.http.graphql.GraphQLErrorException;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * <a href="https://spec.graphql.org/June2018/#sec-Response">specification</a>
 */
public class GraphQLResponseDTO<D extends @Nullable Object, E extends GraphQLErrorDTO> {
    private final @Nullable D data;
    private final @Nullable List<E> errors;

    public GraphQLResponseDTO(@Nullable D data, @Nullable List<E> errors) {
        this.data = data;
        this.errors = errors;
    }

    public @Nullable D getData() {
        return data;
    }

    public @Nullable List<E> getErrors() {
        return errors;
    }

    public @Nullable D getOrThrow() {
        if (data != null) {
            return data;
        }
        if (errors != null) {
            throw new GraphQLErrorException(errors);
        }
        return null;
    }
}
