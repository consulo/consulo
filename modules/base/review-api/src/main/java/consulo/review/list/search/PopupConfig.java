// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.localize.LocalizeValue;
import consulo.review.list.error.TextErrorStatusPresenter;
import org.jspecify.annotations.Nullable;

/**
 * @param isAutoPackHeightOnFiltering If turned on, the popup height will automatically be minimized when a filter is installed
 *                                    and when the number of items in the list changes. This should be turned off for popups that are shown above some point to
 *                                    prevent the popup getting detached from the anchorpoint.
 */
public record PopupConfig(
    LocalizeValue title,
    LocalizeValue searchTextPlaceHolder,
    boolean alwaysShowSearchField,
    boolean isMovable,
    boolean isResizable,
    boolean isAutoPackHeightOnFiltering,
    ShowDirection showDirection,
    @Nullable TextErrorStatusPresenter<Throwable> errorPresenter
) {
    public static final PopupConfig DEFAULT = new PopupConfig(
        LocalizeValue.empty(),
        LocalizeValue.empty(),
        true,
        true,
        true,
        false,
        ShowDirection.BELOW,
        null
    );

    public PopupConfig withTitle(LocalizeValue title) {
        return new PopupConfig(title, searchTextPlaceHolder, alwaysShowSearchField, isMovable, isResizable,
            isAutoPackHeightOnFiltering, showDirection, errorPresenter);
    }

    public PopupConfig withSearchTextPlaceHolder(LocalizeValue searchTextPlaceHolder) {
        return new PopupConfig(title, searchTextPlaceHolder, alwaysShowSearchField, isMovable, isResizable,
            isAutoPackHeightOnFiltering, showDirection, errorPresenter);
    }

    public PopupConfig withAlwaysShowSearchField(boolean alwaysShowSearchField) {
        return new PopupConfig(title, searchTextPlaceHolder, alwaysShowSearchField, isMovable, isResizable,
            isAutoPackHeightOnFiltering, showDirection, errorPresenter);
    }

    public PopupConfig withMovable(boolean isMovable) {
        return new PopupConfig(title, searchTextPlaceHolder, alwaysShowSearchField, isMovable, isResizable,
            isAutoPackHeightOnFiltering, showDirection, errorPresenter);
    }

    public PopupConfig withResizable(boolean isResizable) {
        return new PopupConfig(title, searchTextPlaceHolder, alwaysShowSearchField, isMovable, isResizable,
            isAutoPackHeightOnFiltering, showDirection, errorPresenter);
    }

    public PopupConfig withAutoPackHeightOnFiltering(boolean isAutoPackHeightOnFiltering) {
        return new PopupConfig(title, searchTextPlaceHolder, alwaysShowSearchField, isMovable, isResizable,
            isAutoPackHeightOnFiltering, showDirection, errorPresenter);
    }

    public PopupConfig withShowDirection(ShowDirection showDirection) {
        return new PopupConfig(title, searchTextPlaceHolder, alwaysShowSearchField, isMovable, isResizable,
            isAutoPackHeightOnFiltering, showDirection, errorPresenter);
    }

    public PopupConfig withErrorPresenter(@Nullable TextErrorStatusPresenter<Throwable> errorPresenter) {
        return new PopupConfig(title, searchTextPlaceHolder, alwaysShowSearchField, isMovable, isResizable,
            isAutoPackHeightOnFiltering, showDirection, errorPresenter);
    }
}
