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

import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class GraphQLRequestPagination {
    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final GraphQLRequestPagination DEFAULT = new GraphQLRequestPagination((String) null);

    private final @Nullable String myAfterCursor;
    private final @Nullable Date mySince;
    private final int myPageSize;

    private GraphQLRequestPagination(@Nullable String afterCursor, @Nullable Date since, int pageSize) {
        myAfterCursor = afterCursor;
        mySince = since;
        myPageSize = pageSize;
    }

    public GraphQLRequestPagination() {
        this(null, null, DEFAULT_PAGE_SIZE);
    }

    public GraphQLRequestPagination(int pageSize) {
        this(null, null, pageSize);
    }

    public GraphQLRequestPagination(@Nullable String afterCursor) {
        this(afterCursor, null, DEFAULT_PAGE_SIZE);
    }

    public GraphQLRequestPagination(@Nullable String afterCursor, int pageSize) {
        this(afterCursor, null, pageSize);
    }

    public GraphQLRequestPagination(@Nullable Date since) {
        this(null, since, DEFAULT_PAGE_SIZE);
    }

    public GraphQLRequestPagination(@Nullable Date since, int pageSize) {
        this(null, since, pageSize);
    }

    public @Nullable String getAfterCursor() {
        return myAfterCursor;
    }

    public @Nullable Date getSince() {
        return mySince;
    }

    public int getPageSize() {
        return myPageSize;
    }

    public Map<String, @Nullable Object> asParameters() {
        Map<String, @Nullable Object> parameters = new HashMap<>();
        parameters.put("pageSize", myPageSize);
        parameters.put("cursor", myAfterCursor);
        return parameters;
    }

    public static GraphQLRequestPagination orDefault(@Nullable GraphQLRequestPagination pagination) {
        return pagination == null ? DEFAULT : pagination;
    }

    @Override
    public String toString() {
        return "afterCursor=" + myAfterCursor + "&since=" + mySince + "&pageSize=" + myPageSize;
    }
}
