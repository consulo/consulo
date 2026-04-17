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
 * GraphQL connection type with edges/nodes and page info.
 *
 * @param <T> the node type
 */
public final class GraphQLConnectionDTO<T> {
    private @Nullable List<T> nodes;
    private @Nullable GraphQLCursorPageInfoDTO pageInfo;

    public @Nullable List<T> getNodes() {
        return nodes;
    }

    public void setNodes(@Nullable List<T> nodes) {
        this.nodes = nodes;
    }

    public @Nullable GraphQLCursorPageInfoDTO getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(@Nullable GraphQLCursorPageInfoDTO pageInfo) {
        this.pageInfo = pageInfo;
    }
}
