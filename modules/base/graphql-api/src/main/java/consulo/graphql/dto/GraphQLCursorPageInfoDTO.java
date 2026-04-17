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

/**
 * Cursor-based pagination info for GraphQL connections.
 */
public final class GraphQLCursorPageInfoDTO {
    private boolean hasNextPage;
    private @Nullable String endCursor;

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public @Nullable String getEndCursor() {
        return endCursor;
    }

    public void setEndCursor(@Nullable String endCursor) {
        this.endCursor = endCursor;
    }
}
