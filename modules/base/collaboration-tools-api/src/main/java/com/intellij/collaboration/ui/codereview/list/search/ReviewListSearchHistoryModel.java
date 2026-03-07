// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public interface ReviewListSearchHistoryModel<S extends ReviewListSearchValue> {
    /**
     * Represent last filter selected by user.
     * It can be missed in {@link #getHistory()}, since not all filters can be added to history
     */
    @Nullable
    S getLastFilter();

    void setLastFilter(@Nullable S filter);

    @Nonnull
    List<S> getHistory();

    void add(@Nonnull S search);
}
