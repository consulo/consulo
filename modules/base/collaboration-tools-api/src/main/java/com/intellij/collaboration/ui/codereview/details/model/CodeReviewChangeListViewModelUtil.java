// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import com.intellij.collaboration.util.RefComparisonChange;
import jakarta.annotation.Nonnull;

public final class CodeReviewChangeListViewModelUtil {
    private CodeReviewChangeListViewModelUtil() {
    }

    public static boolean isViewedStateForAllChanges(
        @Nonnull CodeReviewChangeListViewModel.WithDetails vm,
        @Nonnull Iterable<RefComparisonChange> changes,
        boolean viewed
    ) {
        var detailsByChange = vm.getDetailsByChange().getValue();
        for (RefComparisonChange change : changes) {
            CodeReviewChangeDetails details = detailsByChange.get(change);
            if (details == null || details.isRead() != viewed) {
                return false;
            }
        }
        return true;
    }
}
