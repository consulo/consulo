// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.model;

import com.intellij.collaboration.util.ChangesSelection;
import com.intellij.collaboration.util.RefComparisonChange;
import consulo.project.Project;
import kotlinx.coroutines.flow.SharedFlow;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CodeReviewChangeListViewModel {
    @Nonnull
    Project getProject();

    @Nonnull
    List<RefComparisonChange> getChanges();

    /**
     * Flow of selection requests to be handled
     */
    @Nonnull
    SharedFlow<SelectionRequest> getSelectionRequests();

    /**
     * Uni-directional state of changelist selection (changelist presentation should not collect it)
     */
    @Nonnull
    StateFlow<ChangesSelection> getChangesSelection();

    /**
     * Publish changelist selection to {@link #getChangesSelection()}
     */
    void updateSelectedChanges(@Nullable ChangesSelection selection);

    /**
     * Show diff preview for {@link #getChangesSelection()}
     */
    void showDiffPreview();

    /**
     * Request standalone diff for {@link #getChangesSelection()}
     */
    void showDiff();

    interface WithDetails extends CodeReviewChangeListViewModel {
        /**
         * Map of additional details for changes
         */
        @Nonnull
        StateFlow<Map<RefComparisonChange, CodeReviewChangeDetails>> getDetailsByChange();
    }

    @ApiStatus.Experimental
    interface WithGrouping extends CodeReviewChangeListViewModel {
        /**
         * A set of enabled grouping policies
         */
        @Nonnull
        StateFlow<Set<String>> getGrouping();

        void setGrouping(@Nonnull Collection<String> grouping);
    }

    @ApiStatus.Experimental
    interface WithViewedState extends WithDetails {
        @RequiresEdt
        void setViewedState(@Nonnull Iterable<RefComparisonChange> changes, boolean viewed);
    }

    sealed interface SelectionRequest permits SelectionRequest.All, SelectionRequest.OneChange {
        final class All implements SelectionRequest {
            public static final All INSTANCE = new All();

            private All() {
            }
        }

        final class OneChange implements SelectionRequest {
            private final @Nonnull RefComparisonChange change;

            public OneChange(@Nonnull RefComparisonChange change) {
                this.change = change;
            }

            public @Nonnull RefComparisonChange getChange() {
                return change;
            }
        }
    }

    DataKey<CodeReviewChangeListViewModel> DATA_KEY = DataKey.create("Code.Review.Changes.List.ViewModel");
}
