// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.application.util.ListSelection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

final class CodeReviewAsyncDiffViewModelDelegateImpl<C> implements CodeReviewAsyncDiffViewModelDelegate<C> {
    private static final Logger LOG = Logger.getInstance(CodeReviewAsyncDiffViewModelDelegateImpl.class);

    private final MutableObservableValue<CodeReviewDiffChangesState<C>> myChangesToShow = MutableObservableValue.of(new CodeReviewDiffChangesState<>());
    private final List<Consumer<ListSelection<C>>> mySelectionListeners = new CopyOnWriteArrayList<>();

    @Override
    public ObservableValue<CodeReviewDiffChangesState<C>> getChangesToShow() {
        return myChangesToShow;
    }

    @Override
    public void showChanges(ListSelection<C> changes, @Nullable DiffViewerScrollRequest scrollRequest) {
        CodeReviewDiffChangesState<C> state = myChangesToShow.updateAndGet(it -> sameSelection(it.getSelectedChanges(), changes)
            ? it
            : new CodeReviewDiffChangesState<>(changes));
        notifySelection(state.getSelectedChanges());
        if (scrollRequest != null) {
            state.scroll(scrollRequest);
        }
    }

    @Override
    public void showChange(C change, @Nullable DiffViewerScrollRequest scrollRequest) {
        CodeReviewDiffChangesState<C> current = myChangesToShow.get();
        int newIdx = current.getSelectedChanges().getList().indexOf(change);
        if (newIdx < 0) {
            return;
        }
        CodeReviewDiffChangesState<C> state = current;
        ListSelection<C> newChanges = ListSelection.createAt(current.getSelectedChanges().getList(), newIdx);
        if (!sameSelection(newChanges, current.getSelectedChanges())) {
            state = new CodeReviewDiffChangesState<>(newChanges);
            if (!myChangesToShow.compareAndSet(current, state)) {
                return;
            }
            notifySelection(state.getSelectedChanges());
        }
        if (scrollRequest != null) {
            state.scroll(scrollRequest);
        }
    }

    private void notifySelection(ListSelection<C> selection) {
        for (Consumer<ListSelection<C>> listener : mySelectionListeners) {
            try {
                listener.accept(selection);
            }
            catch (Exception e) {
                LOG.warn(e);
            }
        }
    }

    @Override
    public void handleSelection(Disposable parentDisposable, Consumer<ListSelection<C>> listener) {
        mySelectionListeners.add(listener);
        Disposer.register(parentDisposable, () -> mySelectionListeners.remove(listener));
        listener.accept(myChangesToShow.get().getSelectedChanges());
    }

    static <C> boolean sameSelection(ListSelection<C> first, ListSelection<C> second) {
        return first.getSelectedIndex() == second.getSelectedIndex() && first.getList().equals(second.getList());
    }
}
