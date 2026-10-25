// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.application.util.ListSelection;
import consulo.disposer.Disposable;

import java.util.function.Consumer;

/**
 * @param <CVM> change view model type
 */
public final class CodeReviewDiffViewModelsState<CVM> implements CodeReviewDiffProcessorState<CVM>, DiffViewerScrollRequestProducer {
    private final ListSelection<CVM> mySelectedChanges;
    private final DiffViewerScrollRequestProducer myScrollRequests;

    public CodeReviewDiffViewModelsState(ListSelection<CVM> selectedChanges, DiffViewerScrollRequestProducer scrollRequests) {
        mySelectedChanges = selectedChanges;
        myScrollRequests = scrollRequests;
    }

    @Override
    public ListSelection<CVM> getSelectedChanges() {
        return mySelectedChanges;
    }

    @Override
    public void handleScrollRequests(Disposable parentDisposable, Consumer<DiffViewerScrollRequest> listener) {
        myScrollRequests.handleScrollRequests(parentDisposable, listener);
    }
}
