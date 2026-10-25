// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.impl.internal.diff;

import consulo.application.util.ListSelection;
import consulo.versionControlSystem.change.PresentableChange;
import consulo.versionControlSystem.impl.internal.diff.MutableDiffRequestProcessorNavigator;
import consulo.versionControlSystem.review.diff.model.CodeReviewDiffProcessorState;
import consulo.versionControlSystem.review.diff.model.CodeReviewDiffProcessorViewModel;
import consulo.versionControlSystem.review.diff.model.DiffViewerScrollRequest;

import java.util.function.Function;

final class StateNavigator<C> implements MutableDiffRequestProcessorNavigator<C> {
    private final CodeReviewDiffProcessorViewModel<C> myProcessorVm;
    private final CodeReviewDiffProcessorState<C> myState;
    private final Function<C, PresentableChange> myChangePresenter;

    StateNavigator(CodeReviewDiffProcessorViewModel<C> processorVm,
                   CodeReviewDiffProcessorState<C> state,
                   Function<C, PresentableChange> changePresenter) {
        myProcessorVm = processorVm;
        myState = state;
        myChangePresenter = changePresenter;
    }

    @Override
    public ListSelection<C> getCurrentList() {
        return myState.getSelectedChanges();
    }

    @Override
    public void selectPrev(boolean fromDifferences) {
        int selectedIndex = myState.getSelectedChanges().getSelectedIndex();
        if (selectedIndex > 0) {
            myProcessorVm.showChange(selectedIndex - 1, fromDifferences ? DiffViewerScrollRequest.toLastChange() : null);
        }
    }

    @Override
    public void selectNext(boolean fromDifferences) {
        ListSelection<C> selection = myState.getSelectedChanges();
        int selectedIndex = selection.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < selection.getList().size() - 1) {
            myProcessorVm.showChange(selectedIndex + 1, fromDifferences ? DiffViewerScrollRequest.toFirstChange() : null);
        }
    }

    @Override
    public void select(C change) {
        myProcessorVm.showChange(change);
    }

    @Override
    public PresentableChange getChangePresentation(C change) {
        return myChangePresenter.apply(change);
    }
}
