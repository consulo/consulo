// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import consulo.application.util.ListSelection;
import kotlinx.coroutines.flow.Flow;
import jakarta.annotation.Nonnull;

import java.util.Objects;

final class ViewModelsState<CVM> implements CodeReviewDiffProcessorViewModel.State<CVM>, DiffViewerScrollRequestProducer {
    private final @Nonnull ListSelection<CVM> selectedChanges;
    private final @Nonnull Flow<DiffViewerScrollRequest> scrollRequests;

    ViewModelsState(@Nonnull ListSelection<CVM> selectedChanges, @Nonnull Flow<DiffViewerScrollRequest> scrollRequests) {
        this.selectedChanges = selectedChanges;
        this.scrollRequests = scrollRequests;
    }

    @Override
    public @Nonnull ListSelection<CVM> getSelectedChanges() {
        return selectedChanges;
    }

    @Override
    public @Nonnull Flow<DiffViewerScrollRequest> getScrollRequests() {
        return scrollRequests;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ViewModelsState<?> that)) {
            return false;
        }
        return Objects.equals(selectedChanges, that.selectedChanges) &&
            Objects.equals(scrollRequests, that.scrollRequests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectedChanges, scrollRequests);
    }

    @Override
    public String toString() {
        return "ViewModelsState(selectedChanges=" + selectedChanges + ", scrollRequests=" + scrollRequests + ')';
    }
}
