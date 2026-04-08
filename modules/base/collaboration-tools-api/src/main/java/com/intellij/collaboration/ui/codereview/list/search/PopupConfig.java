// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * @param isAutoPackHeightOnFiltering If turned on, the popup height will automatically be minimized when a filter is installed
 *                                    and when the number of items in the list changes. This should be turned off for popups that are shown above some point to
 *                                    prevent the popup getting detached from the anchorpoint.
 */
public final class PopupConfig {
    public static final PopupConfig DEFAULT = new PopupConfig();

    private final @Nullable
    @NlsContexts.PopupTitle String title;
    private final @Nullable
    @NlsContexts.StatusText String searchTextPlaceHolder;
    private final boolean alwaysShowSearchField;
    private final boolean isMovable;
    private final boolean isResizable;
    private final boolean isAutoPackHeightOnFiltering;
    private final @Nonnull ShowDirection showDirection;
    private final @Nullable ErrorStatusPresenter.Text<Throwable> errorPresenter;

    public PopupConfig() {
        this(null, null, true, true, true, false, ShowDirection.BELOW, null);
    }

    public PopupConfig(
        @Nullable @NlsContexts.PopupTitle String title,
        @Nullable @NlsContexts.StatusText String searchTextPlaceHolder,
        boolean alwaysShowSearchField,
        boolean isMovable,
        boolean isResizable,
        boolean isAutoPackHeightOnFiltering,
        @Nonnull ShowDirection showDirection,
        @Nullable ErrorStatusPresenter.Text<Throwable> errorPresenter
    ) {
        this.title = title;
        this.searchTextPlaceHolder = searchTextPlaceHolder;
        this.alwaysShowSearchField = alwaysShowSearchField;
        this.isMovable = isMovable;
        this.isResizable = isResizable;
        this.isAutoPackHeightOnFiltering = isAutoPackHeightOnFiltering;
        this.showDirection = showDirection;
        this.errorPresenter = errorPresenter;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public @Nullable String getSearchTextPlaceHolder() {
        return searchTextPlaceHolder;
    }

    public boolean isAlwaysShowSearchField() {
        return alwaysShowSearchField;
    }

    public boolean isMovable() {
        return isMovable;
    }

    public boolean isResizable() {
        return isResizable;
    }

    public boolean isAutoPackHeightOnFiltering() {
        return isAutoPackHeightOnFiltering;
    }

    public @Nonnull ShowDirection getShowDirection() {
        return showDirection;
    }

    public @Nullable ErrorStatusPresenter.Text<Throwable> getErrorPresenter() {
        return errorPresenter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PopupConfig that = (PopupConfig) o;
        return alwaysShowSearchField == that.alwaysShowSearchField &&
            isMovable == that.isMovable &&
            isResizable == that.isResizable &&
            isAutoPackHeightOnFiltering == that.isAutoPackHeightOnFiltering &&
            Objects.equals(title, that.title) &&
            Objects.equals(searchTextPlaceHolder, that.searchTextPlaceHolder) &&
            showDirection == that.showDirection &&
            Objects.equals(errorPresenter, that.errorPresenter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, searchTextPlaceHolder, alwaysShowSearchField, isMovable, isResizable,
            isAutoPackHeightOnFiltering, showDirection, errorPresenter
        );
    }

    @Override
    public String toString() {
        return "PopupConfig(title=" + title + ", searchTextPlaceHolder=" + searchTextPlaceHolder +
            ", alwaysShowSearchField=" + alwaysShowSearchField + ", isMovable=" + isMovable +
            ", isResizable=" + isResizable + ", isAutoPackHeightOnFiltering=" + isAutoPackHeightOnFiltering +
            ", showDirection=" + showDirection + ", errorPresenter=" + errorPresenter + ")";
    }
}
