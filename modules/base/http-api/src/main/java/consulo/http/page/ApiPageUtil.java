// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.http.page;

import consulo.http.client.URIUtil;
import consulo.http.graphql.GraphQLRequestPagination;
import consulo.http.graphql.dto.GraphQLCursorPageInfoDTO;
import consulo.http.graphql.dto.GraphQLPagedResponseDataDTO;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Loads the pages of a paged API one after another. Every page goes to a consumer as soon as it arrives; the returned future
 * completes after the last one, and cancelling it stops loading further pages.
 */
public final class ApiPageUtil {
    private ApiPageUtil() {
    }

    public static <T> CompletableFuture<?> collectGQLPages(
        Function<GraphQLRequestPagination, CompletableFuture<? extends @Nullable GraphQLPagedResponseDataDTO<T>>> loader,
        Consumer<GraphQLPagedResponseDataDTO<T>> consumer
    ) {
        return collectGQLPages(false, GraphQLRequestPagination.DEFAULT, loader, consumer);
    }

    public static <T> CompletableFuture<?> collectGQLPages(
        boolean reversed,
        GraphQLRequestPagination startPage,
        Function<GraphQLRequestPagination, CompletableFuture<? extends @Nullable GraphQLPagedResponseDataDTO<T>>> loader,
        Consumer<GraphQLPagedResponseDataDTO<T>> consumer
    ) {
        CompletableFuture<Object> result = new CompletableFuture<>();
        loadGQLPage(reversed, startPage, loader, consumer, result);
        return result;
    }

    /**
     * Loads every page and folds their nodes into one list.
     */
    public static <T> CompletableFuture<List<T>> foldGQLPagesToList(
        Function<GraphQLRequestPagination, CompletableFuture<? extends @Nullable GraphQLPagedResponseDataDTO<T>>> loader
    ) {
        List<T> nodes = new ArrayList<>();
        return collectGQLPages(loader, page -> nodes.addAll(page.getNodes())).thenApply(unused -> nodes);
    }

    public static <T> CompletableFuture<?> collectPagesByLinkHeader(URI initialURI,
                                                                    Function<URI, CompletableFuture<HttpResponse<T>>> request,
                                                                    Consumer<HttpResponse<T>> consumer) {
        CompletableFuture<Object> result = new CompletableFuture<>();
        loadPageByLinkHeader(initialURI, initialURI, request, consumer, result);
        return result;
    }

    /**
     * Prefer not to use this method!
     * This is a last resort if Link headers are missing and you need paginated info.
     * It's recommended to use the Link header in all other cases though.
     * <p>
     * Careful: starts pagination at page=1 and increments from there.
     */
    public static <T> CompletableFuture<?> collectPagesByPagination(IntFunction<CompletableFuture<HttpResponse<List<T>>>> loader,
                                                                    Consumer<HttpResponse<List<T>>> consumer) {
        CompletableFuture<Object> result = new CompletableFuture<>();
        loadPageByNumber(1, loader, consumer, result);
        return result;
    }

    private static <T> void loadGQLPage(boolean reversed,
                                        GraphQLRequestPagination pagination,
                                        Function<GraphQLRequestPagination, CompletableFuture<? extends @Nullable GraphQLPagedResponseDataDTO<T>>> loader,
                                        Consumer<GraphQLPagedResponseDataDTO<T>> consumer,
                                        CompletableFuture<Object> result) {
        if (result.isDone()) {
            return;
        }
        loader.apply(pagination).whenComplete((response, error) -> {
            if (error != null) {
                result.completeExceptionally(error);
                return;
            }
            if (result.isDone()) {
                return;
            }
            if (response == null) {
                result.complete(null);
                return;
            }
            try {
                consumer.accept(response);
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
                return;
            }
            GraphQLRequestPagination next = nextPage(response.getPageInfo(), reversed);
            if (next == null) {
                result.complete(null);
            }
            else {
                loadGQLPage(reversed, next, loader, consumer, result);
            }
        });
    }

    private static @Nullable GraphQLRequestPagination nextPage(GraphQLCursorPageInfoDTO pageInfo, boolean reversed) {
        if (!reversed) {
            String endCursor = pageInfo.getEndCursor();
            return pageInfo.getHasNextPage() && endCursor != null ? new GraphQLRequestPagination(endCursor) : null;
        }
        String startCursor = pageInfo.getStartCursor();
        return pageInfo.getHasPreviousPage() && startCursor != null ? new GraphQLRequestPagination(startCursor) : null;
    }

    private static <T> void loadPageByLinkHeader(URI initialURI,
                                                 URI uri,
                                                 Function<URI, CompletableFuture<HttpResponse<T>>> request,
                                                 Consumer<HttpResponse<T>> consumer,
                                                 CompletableFuture<Object> result) {
        if (result.isDone()) {
            return;
        }
        request.apply(uri).whenComplete((response, error) -> {
            if (error != null) {
                result.completeExceptionally(error);
                return;
            }
            if (result.isDone()) {
                return;
            }
            String nextLink;
            try {
                nextLink = response.headers().firstValue(LinkHttpHeaderValue.HEADER_NAME)
                    .map(LinkHttpHeaderValue::parse)
                    .map(LinkHttpHeaderValue::nextLink)
                    .orElse(null);
                consumer.accept(response);
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
                return;
            }
            if (nextLink == null) {
                result.complete(null);
            }
            else {
                loadPageByLinkHeader(initialURI, URIUtil.createUriWithCustomScheme(nextLink, initialURI.getScheme()), request, consumer, result);
            }
        });
    }

    private static <T> void loadPageByNumber(int page,
                                             IntFunction<CompletableFuture<HttpResponse<List<T>>>> loader,
                                             Consumer<HttpResponse<List<T>>> consumer,
                                             CompletableFuture<Object> result) {
        if (result.isDone()) {
            return;
        }
        loader.apply(page).whenComplete((data, error) -> {
            if (error != null) {
                result.completeExceptionally(error);
                return;
            }
            if (result.isDone()) {
                return;
            }
            if (data.body().isEmpty()) {
                result.complete(null);
                return;
            }
            try {
                consumer.accept(data);
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
                return;
            }
            loadPageByNumber(page + 1, loader, consumer, result);
        });
    }
}
