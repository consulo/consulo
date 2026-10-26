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
package consulo.review.list.search;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public abstract class ReviewListSearchPanelFactory<S extends ReviewListSearchValue, Q extends ReviewListQuickFilter<S>, VM extends ReviewListSearchPanelViewModel<S, Q>> {
    protected final VM myVm;

    protected ReviewListSearchPanelFactory(VM vm) {
        myVm = vm;
    }

    @RequiredUIAccess
    public Component create(Disposable viewDisposable) {
        return create(viewDisposable, null);
    }

    @RequiredUIAccess
    public Component create(Disposable viewDisposable, @Nullable Consumer<Q> quickFilterListener) {
        TextBox searchField = ReviewListSearchFiltersTextFieldFactory.create(
            viewDisposable,
            myVm.getQueryState(),
            null,
            text -> myVm.getQueryState().set(text),
            field -> ReviewListSearchFiltersPanelFactory.showHistory(field, myVm.getSearchHistory(), this::getShortText,
                myVm.getSearchState()::set)
        );

        List<AnAction> filters = createFilters(viewDisposable);

        AnAction quickFilterButton = QuickFilterButtonFactory.create(
            myVm.getSearchState(),
            myVm.getQuickFilters(),
            quickFilterListener,
            this::getQuickFilterTitle,
            filters,
            newQuery -> myVm.getSearchState().set(newQuery),
            () -> myVm.getSearchState().set(myVm.getEmptySearch())
        );

        return ReviewListSearchFiltersPanelFactory.createPanel(viewDisposable, searchField, quickFilterButton, myVm.getSearchState());
    }

    protected abstract LocalizeValue getShortText(S searchValue);

    protected abstract List<AnAction> createFilters(Disposable viewDisposable);

    protected abstract LocalizeValue getQuickFilterTitle(Q quickFilter);
}
