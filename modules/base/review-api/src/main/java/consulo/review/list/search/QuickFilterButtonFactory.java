// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.list.search;

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.BadgeIconSupplier;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.Toggleable;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

final class QuickFilterButtonFactory {
    private static final BadgeIconSupplier FILTER_ICON = new BadgeIconSupplier(PlatformIconGroup.generalFilter());

    private QuickFilterButtonFactory() {
    }

    static <S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> AnAction create(
        ObservableValue<S> searchState,
        List<Q> quickFilters,
        @Nullable Consumer<Q> filterListener,
        Function<Q, LocalizeValue> quickFilterTitleProvider,
        List<AnAction> filters,
        Consumer<S> setSearchQuery,
        Runnable clearSearchQuery
    ) {
        return new FilterPopupMenuAction<>(
            searchState,
            quickFilters,
            filterListener,
            quickFilterTitleProvider,
            filters,
            setSearchQuery,
            clearSearchQuery
        );
    }


    @RequiredUIAccess
    private static <S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> void showQuickFiltersPopup(
        AnActionEvent e,
        ObservableValue<S> searchState,
        List<Q> quickFilters,
        @Nullable Consumer<Q> filterListener,
        Function<Q, LocalizeValue> quickFilterTitleProvider,
        List<AnAction> filters,
        Consumer<S> setSearchQuery,
        Runnable clearSearchQuery
    ) {
        List<AnAction> quickFiltersActions = new ArrayList<>();
        quickFiltersActions.add(AnSeparator.create(ReviewLocalize.reviewListFilterQuickTitle()));
        for (Q filter : quickFilters) {
            quickFiltersActions.add(new QuickFilterAction<>(searchState, quickFilterTitleProvider.apply(filter), filter, filterListener, setSearchQuery));
        }
        quickFiltersActions.addAll(filters);
        quickFiltersActions.add(AnSeparator.create());
        quickFiltersActions.add(new ClearFiltersAction<>(searchState, clearSearchQuery));

        ListPopup popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(null, ActionGroup.of(quickFiltersActions.toArray(AnAction[]::new)),
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false);
        popup.showUnderneathOf(e);
    }

    private static final class FilterPopupMenuAction<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> extends AnAction
        implements DumbAware, AnActionWithSyncUpdate {
        private final ObservableValue<S> mySearchState;
        private final List<Q> myQuickFilters;
        private final @Nullable Consumer<Q> myFilterListener;
        private final Function<Q, LocalizeValue> myQuickFilterTitleProvider;
        private final List<AnAction> myFilters;
        private final Consumer<S> mySetSearchQuery;
        private final Runnable myClearSearchQuery;

        private FilterPopupMenuAction(ObservableValue<S> searchState,
                                      List<Q> quickFilters,
                                      @Nullable Consumer<Q> filterListener,
                                      Function<Q, LocalizeValue> quickFilterTitleProvider,
                                      List<AnAction> filters,
                                      Consumer<S> setSearchQuery,
                                      Runnable clearSearchQuery) {
            super(ReviewLocalize.reviewListFilterQuickTitle(), LocalizeValue.empty(), PlatformIconGroup.generalFilter());
            mySearchState = searchState;
            myQuickFilters = quickFilters;
            myFilterListener = filterListener;
            myQuickFilterTitleProvider = quickFilterTitleProvider;
            myFilters = filters;
            mySetSearchQuery = setSearchQuery;
            myClearSearchQuery = clearSearchQuery;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setIcon(FILTER_ICON.getLiveIndicatorIcon(mySearchState.get().getFilterCount() != 0));
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            showQuickFiltersPopup(
                e,
                mySearchState,
                myQuickFilters,
                myFilterListener,
                myQuickFilterTitleProvider,
                myFilters,
                mySetSearchQuery,
                myClearSearchQuery
            );
        }
    }

    private static final class QuickFilterAction<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> extends AnAction
        implements DumbAware, AnActionWithSyncUpdate, Toggleable {
        private final ObservableValue<S> mySearchState;
        private final Q myFilter;
        private final @Nullable Consumer<Q> myFilterListener;
        private final Consumer<S> mySetSearchQuery;

        private QuickFilterAction(ObservableValue<S> searchState,
                                  LocalizeValue name,
                                  Q filter,
                                  @Nullable Consumer<Q> filterListener,
                                  Consumer<S> setSearchQuery) {
            super(name);
            mySearchState = searchState;
            myFilter = filter;
            myFilterListener = filterListener;
            mySetSearchQuery = setSearchQuery;
        }

        @Override
        public void update(AnActionEvent e) {
            Toggleable.setSelected(e.getPresentation(), mySearchState.get().equals(myFilter.getFilter()));
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            if (myFilterListener != null) {
                myFilterListener.accept(myFilter);
            }
            mySetSearchQuery.accept(myFilter.getFilter());
        }
    }

    private static final class ClearFiltersAction<S extends ReviewListSearchValue> extends AnAction
        implements DumbAware, AnActionWithSyncUpdate {
        private final ObservableValue<S> mySearchState;
        private final Runnable myClearSearchQuery;

        private ClearFiltersAction(ObservableValue<S> searchState, Runnable clearSearchQuery) {
            super(ReviewLocalize.reviewListFilterQuickClear(searchState.get().getFilterCount()));
            mySearchState = searchState;
            myClearSearchQuery = clearSearchQuery;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(mySearchState.get().getFilterCount() > 0);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            myClearSearchQuery.run();
        }
    }
}
