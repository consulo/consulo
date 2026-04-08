// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import app.cash.turbine.TurbineTestKt;
import io.mockk.MockKKt;
import io.mockk.MockKStubScope;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.time.Duration;
import kotlinx.coroutines.test.TestBuildersKt;
import org.assertj.core.api.Assertions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

final class PaginatedPotentiallyInfiniteListLoaderTest {

  private static final int PAGE_SIZE = 20;
  private static final List<DummyData> ALL_TEST_DATA =
    IntStream.range(0, 100).mapToObj(DummyData::new).toList();

  private record DummyData(int key) {
  }

  private record DummyPageInfo(int offset, boolean hasNext)
    implements PaginatedPotentiallyInfiniteListLoader.PageInfo<DummyPageInfo> {
    @Override
    public @Nullable DummyPageInfo createNextPageInfo() {
      return hasNext ? new DummyPageInfo(offset + PAGE_SIZE, true) : null;
    }
  }

  private record DummyPage(@Nullable DummyPageInfo pageInfo, @Nullable List<DummyData> data) {
  }

  @FunctionalInterface
  private interface PageLoader {
    @Nullable DummyPage computePage(int offset);
  }

  private static PageLoader pageLoaderMock() {
    return pageLoaderMock(() -> ALL_TEST_DATA.size());
  }

  private static PageLoader pageLoaderMock(java.util.function.IntSupplier sizeLimiter) {
    return offset -> {
      int virtualSize = Math.min(ALL_TEST_DATA.size(), sizeLimiter.getAsInt());
      int endIndex = Math.min(virtualSize, offset + PAGE_SIZE);

      if (offset >= virtualSize) return null;

      List<DummyData> data = ALL_TEST_DATA.subList(offset, endIndex);
      boolean hasNext = endIndex < virtualSize;
      return new DummyPage(new DummyPageInfo(offset, hasNext), data);
    };
  }

  private static final class TestLoader
    extends PaginatedPotentiallyInfiniteListLoader<DummyPageInfo, Integer, DummyData> {

    private final PageLoader pageLoader;

    TestLoader(boolean shouldTryToLoadAll, PageLoader pageLoader) {
      super(new DummyPageInfo(0, true), DummyData::key, shouldTryToLoadAll);
      this.pageLoader = pageLoader;
    }

    @Override
    protected @Nullable Page<DummyPageInfo, DummyData> performRequestAndProcess(
      @Nonnull DummyPageInfo pageInfo,
      @Nonnull kotlin.jvm.functions.Function2<@Nullable DummyPageInfo, @Nullable List<DummyData>,
        @Nullable Page<DummyPageInfo, DummyData>> createPage,
      @Nonnull Continuation<? super Page<DummyPageInfo, DummyData>> continuation
    ) {
      DummyPage response = pageLoader.computePage(pageInfo.offset());
      if (response == null) return null;
      return createPage.invoke(response.pageInfo(), response.data());
    }
  }

  @Test
  public void noPagesAreLoadedInitially() {
    TestBuildersKt.runTest((scope, continuation) -> {
      PageLoader pageLookupMock = pageLoaderMock();
      TestLoader listLoader = new TestLoader(false, pageLookupMock);

      TurbineTestKt.test(listLoader.getStateFlow(), Duration.Companion.seconds(1), (turbine, cont) -> {
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList()).isNull();
        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void onePageIsLoadedInitiallyWithStartingReload() {
    TestBuildersKt.runTest((scope, continuation) -> {
      PageLoader pageLoaderMock = pageLoaderMock();
      TestLoader listLoader = new TestLoader(false, pageLoaderMock);

      TurbineTestKt.test(listLoader.getStateFlow(), Duration.Companion.seconds(1), (turbine, cont) -> {
        listLoader.reload(cont);

        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList()).isNull();
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .containsExactlyElementsOf(ALL_TEST_DATA.subList(0, PAGE_SIZE));
        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void allPagesAreLoadedInitiallyWithStartingReload() {
    TestBuildersKt.runTest((scope, continuation) -> {
      PageLoader pageLoaderMock = pageLoaderMock();
      TestLoader listLoader = new TestLoader(true, pageLoaderMock);

      TurbineTestKt.test(listLoader.getStateFlow(), Duration.Companion.seconds(1), (turbine, cont) -> {
        listLoader.reload(cont);

        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList()).isNull();
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .containsExactlyElementsOf(ALL_TEST_DATA.subList(0, PAGE_SIZE));
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .containsExactlyElementsOf(ALL_TEST_DATA.subList(0, PAGE_SIZE * 2));
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .containsExactlyElementsOf(ALL_TEST_DATA.subList(0, PAGE_SIZE * 3));
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .containsExactlyElementsOf(ALL_TEST_DATA.subList(0, PAGE_SIZE * 4));
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .containsExactlyElementsOf(ALL_TEST_DATA);
        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void onlyPreviouslyLoadedPagesAreReFetchedUponRefresh() {
    TestBuildersKt.runTest((scope, continuation) -> {
      PageLoader pageLoaderMock = pageLoaderMock();
      TestLoader listLoader = new TestLoader(false, pageLoaderMock);

      TurbineTestKt.test(listLoader.getStateFlow(), Duration.Companion.seconds(1), (turbine, cont) -> {
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList()).isNull();
        turbine.expectNoEvents(cont);

        listLoader.loadMore(cont);

        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .isEqualTo(ALL_TEST_DATA.subList(0, PAGE_SIZE));
        turbine.expectNoEvents(cont);

        listLoader.refresh(cont);
        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void loadMoreDoesntUpdateStateWhenNoNewDataIsAvailable() {
    TestBuildersKt.runTest((scope, continuation) -> {
      PageLoader pageLoaderMock = pageLoaderMock();
      TestLoader listLoader = new TestLoader(true, pageLoaderMock);

      TurbineTestKt.test(listLoader.getStateFlow(), Duration.Companion.seconds(1), (turbine, cont) -> {
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList()).isNull();
        turbine.expectNoEvents(cont);

        listLoader.reload(cont);
        while (!Objects.equals(
          ((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList(),
          ALL_TEST_DATA)) {
        }
        turbine.expectNoEvents(cont);

        listLoader.loadMore(cont);
        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void loadMoreDoesUpdateStateWhenNewDataIsAvailable() {
    TestBuildersKt.runTest((scope, continuation) -> {
      PageLoader pageLoaderMock = pageLoaderMock();
      TestLoader listLoader = new TestLoader(false, pageLoaderMock);

      TurbineTestKt.test(listLoader.getStateFlow(), Duration.Companion.seconds(1), (turbine, cont) -> {
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList()).isNull();
        turbine.expectNoEvents(cont);

        listLoader.loadMore(cont);
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .isEqualTo(ALL_TEST_DATA.subList(0, PAGE_SIZE));
        turbine.expectNoEvents(cont);

        listLoader.loadMore(cont);
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .isEqualTo(ALL_TEST_DATA.subList(0, PAGE_SIZE * 2));
        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void allPagesIncludingNewOnesAreLoadedDuringRefreshWhenFullLoadingIsRequested() {
    TestBuildersKt.runTest((scope, continuation) -> {
      int[] dynamicSize = {PAGE_SIZE};

      PageLoader pageLoaderMock = pageLoaderMock(() -> dynamicSize[0]);
      TestLoader listLoader = new TestLoader(true, pageLoaderMock);

      TurbineTestKt.test(listLoader.getStateFlow(), Duration.Companion.seconds(1), (turbine, cont) -> {
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList()).isNull();
        turbine.expectNoEvents(cont);

        listLoader.loadAll(cont);
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .isEqualTo(ALL_TEST_DATA.subList(0, PAGE_SIZE));
        turbine.expectNoEvents(cont);

        dynamicSize[0] = PAGE_SIZE * 3;

        listLoader.refresh(cont);
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .isEqualTo(ALL_TEST_DATA.subList(0, PAGE_SIZE * 2));
        assertThat(((PaginatedPotentiallyInfiniteListLoader.State<?>)turbine.awaitItem(cont)).getList())
          .isEqualTo(ALL_TEST_DATA.subList(0, PAGE_SIZE * 3));
        turbine.expectNoEvents(cont);
        return Unit.INSTANCE;
      }, continuation);
      return Unit.INSTANCE;
    });
  }

  // TODO: Tests for (1) update function, (2) throwing an error inside fetch, (3) tracking reload/refresh flows maybe
  // For more info, run and check coverage inside com.intellij.collaboration.async
}
