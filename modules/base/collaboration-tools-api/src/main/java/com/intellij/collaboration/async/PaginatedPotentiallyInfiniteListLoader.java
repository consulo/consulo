// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

import java.util.*;
import java.util.function.Function;

/**
 * An abstract class that provides functionality for managing and loading a paginated
 * potentially infinite list of data items.
 *
 * @param <PI> The type representing page information, which tracks the pagination state.
 * @param <K>  The type representing keys extracted from data items.
 * @param <V>  The type of data items being loaded and managed by the loader.
 */
@ApiStatus.Internal
public abstract class PaginatedPotentiallyInfiniteListLoader<PI extends PaginatedPotentiallyInfiniteListLoader.PageInfo<PI>, K, V>
    extends MutableListLoader<V>
    implements ReloadablePotentiallyInfiniteListLoader<V> {

    public interface PageInfo<PI extends PageInfo<PI>> {
        @Nullable
        PI createNextPageInfo();
    }

    @FunctionalInterface
    public interface PageCreator<PI, V> {
        @Nullable
        Page<PI, V> create(@Nullable PI pageInfo, @Nullable List<V> results);
    }

    protected static final class Page<PageInfo, V> {
        private final @Nullable PageInfo info;
        private final @Nonnull List<V> list;

        public Page(@Nullable PageInfo info, @Nonnull List<V> list) {
            this.info = info;
            this.list = list;
        }

        public @Nullable PageInfo getInfo() {
            return info;
        }

        public @Nonnull List<V> getList() {
            return list;
        }

        public <NewPI> @Nonnull Page<NewPI, V> withInfo(@Nullable NewPI newInfo) {
            return new Page<>(newInfo, list);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Page<?, ?> page)) {
                return false;
            }
            return Objects.equals(info, page.info) && list.equals(page.list);
        }

        @Override
        public int hashCode() {
            return Objects.hash(info, list);
        }

        @Override
        public String toString() {
            return "Page(info=" + info + ", list=" + list + ")";
        }
    }

    private final @Nonnull PI initialPageInfo;
    private final @Nonnull Function<V, K> extractKey;
    private boolean shouldTryToLoadAll;

    private @Nonnull ListLoader.State<Page<PI, V>> pages = new ListLoader.State<>();
    private final @Nonnull Object operationLock = new Object();

    private final @Nonnull MutableStateFlow<Boolean> _isBusyFlow = StateFlowKt.MutableStateFlow(false);

    @Override
    public @Nonnull StateFlow<Boolean> getIsBusyFlow() {
        return FlowKt.asStateFlow(_isBusyFlow);
    }

    protected PaginatedPotentiallyInfiniteListLoader(
        @Nonnull PI initialPageInfo,
        @Nonnull Function<V, K> extractKey,
        boolean shouldTryToLoadAll
    ) {
        this.initialPageInfo = initialPageInfo;
        this.extractKey = extractKey;
        this.shouldTryToLoadAll = shouldTryToLoadAll;
    }

    /**
     * Performs the actual request and processes the response.
     *
     * @return {@code null} when the page was not loaded.
     */
    protected abstract @Nullable Page<PI, V> performRequestAndProcess(
        @Nonnull PI pageInfo,
        @Nonnull PageCreator<PI, V> createPage
    ) throws Exception;

    // Note: The suspend functions are represented here but actual coroutine suspension
    // is handled via Kotlin continuation-passing style at the call sites.

    public void reload() throws Exception {
        synchronized (operationLock) {
            try {
                _isBusyFlow.setValue(true);
                doEmitPages(new ListLoader.State<>());
                boolean loadedMoreData = loadMoreImpl();
                if (loadedMoreData && shouldTryToLoadAll) {
                    loadAllImpl();
                }
            }
            finally {
                _isBusyFlow.setValue(false);
            }
        }
    }

    public void refresh() throws Exception {
        synchronized (operationLock) {
            try {
                _isBusyFlow.setValue(true);
                doRefresh();
            }
            finally {
                _isBusyFlow.setValue(false);
            }
        }
    }

    public void loadMore() throws Exception {
        synchronized (operationLock) {
            try {
                _isBusyFlow.setValue(true);
                loadMoreImpl();
            }
            finally {
                _isBusyFlow.setValue(false);
            }
        }
    }

    public void loadAll() throws Exception {
        synchronized (operationLock) {
            try {
                _isBusyFlow.setValue(true);
                loadAllImpl();
            }
            finally {
                _isBusyFlow.setValue(false);
            }
        }
    }

    private void doRefresh() throws Exception {
        List<Page<PI, V>> currentPages = pages.getList() != null ? pages.getList() : List.of();
        ListLoader.State<Page<PI, V>> newState;
        try {
            List<Page<PI, V>> newPages = new ArrayList<>();
            for (Page<PI, V> page : currentPages) {
                if (page.getInfo() == null) {
                    continue;
                }
                Page<PI, V> refreshed = performRequestAndProcess(page.getInfo(), (pageInfo, results) ->
                    new Page<>(pageInfo, results != null ? results : page.getList()));
                if (refreshed != null) {
                    newPages.add(refreshed);
                }
            }
            newState = new ListLoader.State<>(newPages, null);
        }
        catch (Exception e) {
            newState = new ListLoader.State<>(currentPages, e);
        }
        doEmitPages(newState);

        if (shouldTryToLoadAll) {
            loadAllImpl();
        }
    }

    private boolean loadMoreImpl() throws Exception {
        List<Page<PI, V>> latestPages = pages.getList() != null ? pages.getList() : List.of();
        PI nextPageInfo;
        if (latestPages.isEmpty()) {
            nextPageInfo = initialPageInfo;
        }
        else {
            Page<PI, V> lastPage = latestPages.getLast();
            nextPageInfo = lastPage.getInfo() != null ? lastPage.getInfo().createNextPageInfo() : null;
        }
        if (nextPageInfo == null) {
            return false;
        }

        Page<PI, V> nextPage;
        try {
            nextPage = performRequestAndProcess(nextPageInfo, (pageInfo, results) -> {
                if (results == null) {
                    return null;
                }
                return new Page<>(pageInfo, results);
            });
            if (nextPage == null) {
                return false;
            }
        }
        catch (Exception e) {
            doEmitPages(new ListLoader.State<>(latestPages, e));
            return false;
        }

        List<Page<PI, V>> newPagesList = new ArrayList<>(latestPages);
        newPagesList.add(nextPage);
        doEmitPages(new ListLoader.State<>(newPagesList, pages.getError()));
        return true;
    }

    private void loadAllImpl() throws Exception {
        shouldTryToLoadAll = true;
        boolean loadedMoreData;
        do {
            loadedMoreData = loadMoreImpl();
        }
        while (loadedMoreData);
    }

    private void doEmitPages(@Nonnull ListLoader.State<Page<PI, V>> st) {
        pages = st;
        List<V> flatList = null;
        if (st.getList() != null) {
            SequencedSet<K> seenKeys = new LinkedHashSet<>();
            List<V> result = new ArrayList<>();
            for (Page<PI, V> page : st.getList()) {
                for (V item : page.getList()) {
                    K key = extractKey.apply(item);
                    if (seenKeys.add(key)) {
                        result.add(item);
                    }
                }
            }
            flatList = result;
        }
        mutableStateFlow.setValue(new ListLoader.State<>(flatList, st.getError()));
    }
}
