// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.list.search;

import org.jspecify.annotations.Nullable;

import java.util.List;

public interface ReviewListSearchHistoryModel<S extends ReviewListSearchValue> {
    /**
     * Represent last filter selected by user.
     * It can be missed in {@link #getHistory()}, since not all filters can be added to history
     */
    @Nullable
    S getLastFilter();

    void setLastFilter(@Nullable S lastFilter);

    List<S> getHistory();

    void add(S search);
}
