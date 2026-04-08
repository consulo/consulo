// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview;

import com.intellij.collaboration.async.AsyncUtilKt;
import com.intellij.collaboration.async.StateFlowKt;
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeListViewModel;
import com.intellij.collaboration.util.RefComparisonChange;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.FlowPreview;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

public final class CodeReviewProgressTreeModelFromDetails extends CodeReviewProgressTreeModel<RefComparisonChange> {
    private final StateFlow<Map<RefComparisonChange, CodeReviewChangeListViewModel.ChangeDetails>> details;
    private final StateFlow<Boolean> isLoading;

    @SuppressWarnings("unused")
    public CodeReviewProgressTreeModelFromDetails(
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewChangeListViewModel.WithDetails vm
    ) {
        details = StateFlowKt.stateInNow(vm.getDetailsByChange(), cs, null);
        isLoading = StateFlowKt.mapState(details, it -> it == null);

        AsyncUtilKt.launchNow(cs, continuation -> {
            @SuppressWarnings("unchecked")
            StateFlow<Map<RefComparisonChange, CodeReviewChangeListViewModel.ChangeDetails>> flow = details;
            FlowKt.debounce(flow, 100L);
            // Note: This is a simplified representation. The actual coroutine collection
            // would need to be done using Kotlin coroutines infrastructure.
            fireModelChanged();
            return null;
        });
    }

    @Override
    public @Nonnull StateFlow<Boolean> getIsLoading() {
        return isLoading;
    }

    @Override
    public @Nullable RefComparisonChange asLeaf(@Nonnull ChangesBrowserNode<?> node) {
        Object userObject = node.getUserObject();
        return userObject instanceof RefComparisonChange change ? change : null;
    }

    @Override
    public boolean isLoading(@Nonnull RefComparisonChange leafValue) {
        Map<RefComparisonChange, CodeReviewChangeListViewModel.ChangeDetails> map = details.getValue();
        return map == null || map.get(leafValue) == null;
    }

    @Override
    public boolean isRead(@Nonnull RefComparisonChange leafValue) {
        Map<RefComparisonChange, CodeReviewChangeListViewModel.ChangeDetails> map = details.getValue();
        if (map == null) {
            return true;
        }
        CodeReviewChangeListViewModel.ChangeDetails detail = map.get(leafValue);
        return detail == null || detail.isRead();
    }

    @Override
    public int getUnresolvedDiscussionsCount(@Nonnull RefComparisonChange leafValue) {
        Map<RefComparisonChange, CodeReviewChangeListViewModel.ChangeDetails> map = details.getValue();
        if (map == null) {
            return 0;
        }
        CodeReviewChangeListViewModel.ChangeDetails detail = map.get(leafValue);
        return detail == null ? 0 : detail.getDiscussions();
    }
}
