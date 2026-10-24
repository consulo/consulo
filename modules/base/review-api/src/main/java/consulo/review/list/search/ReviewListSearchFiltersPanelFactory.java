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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.review.util.ReviewUIUtil;
import consulo.review.util.popup.PopupItemPresentation;
import consulo.ui.Component;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ReviewListSearchFiltersPanelFactory {
    private static final String FILTER_TOOLBAR_PLACE = "Review.FilterToolbar";

    private ReviewListSearchFiltersPanelFactory() {
    }

    @RequiredUIAccess
    public static <S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> Component create(
        Disposable parentDisposable,
        ReviewListSearchFiltersPanelViewModel<S, Q> vm,
        Function<S, LocalizeValue> getShortText,
        Supplier<List<AnAction>> createFilters,
        Function<Q, LocalizeValue> getQuickFilterTitle
    ) {
        return create(parentDisposable, vm, getShortText, createFilters, getQuickFilterTitle, null);
    }

    @RequiredUIAccess
    public static <S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>> Component create(
        Disposable parentDisposable,
        ReviewListSearchFiltersPanelViewModel<S, Q> vm,
        Function<S, LocalizeValue> getShortText,
        Supplier<List<AnAction>> createFilters,
        Function<Q, LocalizeValue> getQuickFilterTitle,
        @Nullable Consumer<Q> quickFilterListener
    ) {
        ObservableValue<@Nullable String> searchQueryTextState = vm.getSearchQueryState().map(ReviewListSearchValue::getSearchQuery);
        TextBox searchField = ReviewListSearchFiltersTextFieldFactory.create(
            parentDisposable,
            searchQueryTextState,
            vm::setSearchText,
            text -> vm.submitSearchText(),
            field -> showHistory(field, vm.getSearchHistory(), getShortText, vm::setSearchQuery)
        );

        AnAction quickFilterButton = QuickFilterButtonFactory.create(
            vm.getSearchQueryState(),
            vm.getQuickFilters(),
            quickFilterListener,
            getQuickFilterTitle,
            vm::setSearchQuery,
            vm::clearSearchQuery
        );

        return createPanel(parentDisposable, searchField, quickFilterButton, createFilters.get(), vm.getSearchQueryState());
    }

    @RequiredUIAccess
    static <S> void showHistory(TextBox searchField,
                                List<S> history,
                                Function<S, LocalizeValue> getShortText,
                                Consumer<S> setSearchQuery) {
        List<S> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        ChooserPopup<@Nullable S> chooser = ChooserPopupUtil.createChooserPopup(
            reversed,
            value -> new PopupItemPresentation.Simple(getShortText.apply(value).get()),
            PopupConfig.DEFAULT
        );
        chooser.result().thenAccept(value -> {
            if (value != null) {
                setSearchQuery.accept(value);
            }
        });
        chooser.popup().showBy(searchField, null, PopupPosition.BOTTOM);
    }

    /**
     * The search field, and under it one toolbar with the quick filters button followed by the filters.
     */
    @RequiredUIAccess
    static Component createPanel(Disposable parentDisposable,
                                 TextBox searchField,
                                 AnAction quickFilterButton,
                                 List<AnAction> filters,
                                 ObservableValue<?> searchState) {
        DefaultActionGroup filtersGroup = new DefaultActionGroup();
        filtersGroup.add(quickFilterButton);
        filters.forEach(filtersGroup::add);

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(FILTER_TOOLBAR_PLACE, filtersGroup, ActionToolbar.Style.HORIZONTAL);

        DockLayout searchRow = DockLayout.create();
        searchRow.center(searchField);

        VerticalLayout searchPanel = VerticalLayout.create(Space.LARGE);
        searchPanel.add(searchRow);
        searchPanel.add(toolbar.getUIComponent());
        searchPanel.paddingBuilder().topSet(Space.LARGE).horizontalSet(Space.LARGE).apply();
        toolbar.setTargetUIComponent(searchPanel);

        ReviewUIUtil.bind(parentDisposable, searchState, state -> toolbar.updateActionsAsync());
        return searchPanel;
    }
}
