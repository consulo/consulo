// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public abstract class PersistingReviewListSearchHistoryModel<S extends ReviewListSearchValue>
    implements ReviewListSearchHistoryModel<S> {

    private final int historySizeLimit;

    protected PersistingReviewListSearchHistoryModel() {
        this(10);
    }

    protected PersistingReviewListSearchHistoryModel(int historySizeLimit) {
        this.historySizeLimit = historySizeLimit;
    }

    @Override
    public @Nonnull List<S> getHistory() {
        return getPersistentHistory();
    }

    protected abstract @Nonnull List<S> getPersistentHistory();

    protected abstract void setPersistentHistory(@Nonnull List<S> history);

    @Override
    public void add(@Nonnull S search) {
        List<S> updated = new ArrayList<>(getPersistentHistory());
        updated.remove(search);
        updated.add(search);
        int fromIndex = Math.max(0, updated.size() - historySizeLimit);
        setPersistentHistory(updated.subList(fromIndex, updated.size()));
    }
}
