// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.application.util.ListSelection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.review.async.MappingScopedItemsContainer;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @param <C> change type
 */
public interface CodeReviewAsyncDiffViewModelDelegate<C> {
    ObservableValue<CodeReviewDiffChangesState<C>> getChangesToShow();

    void showChanges(ListSelection<C> changes, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChanges(ListSelection<C> changes) {
        showChanges(changes, null);
    }

    void showChange(C change, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChange(C change) {
        showChange(change, null);
    }

    /**
     * Calls {@code listener} with the current selection and every later one until {@code parentDisposable} goes away.
     */
    void handleSelection(Disposable parentDisposable, Consumer<ListSelection<C>> listener);

    static <C> CodeReviewAsyncDiffViewModelDelegate<C> create() {
        return new CodeReviewAsyncDiffViewModelDelegateImpl<>();
    }

    static <C, CVM extends AsyncDiffViewModel> ObservableValue<@Nullable ComputedResult<CodeReviewDiffProcessorState<CVM>>> mapChangesToVms(
        Disposable parentDisposable,
        ObservableValue<CodeReviewDiffChangesState<C>> changes,
        BiFunction<Disposable, C, CVM> createViewModel
    ) {
        MappingScopedItemsContainer<C, CVM> vmsContainer = MappingScopedItemsContainer.byEquality(parentDisposable, createViewModel);
        MutableObservableValue<@Nullable ComputedResult<CodeReviewDiffProcessorState<CVM>>> result = MutableObservableValue.of(null);
        Object lock = new Object();
        List<?>[] lastList = {List.of()};
        Consumer<CodeReviewDiffChangesState<C>> collector = changesState -> {
            synchronized (lock) {
                result.set(ComputedResult.compute(() -> {
                    List<C> list = changesState.getSelectedChanges().getList();
                    if (!list.equals(lastList[0])) {
                        vmsContainer.update(list);
                        lastList[0] = list;
                    }
                    List<CVM> vms = vmsContainer.getMappedState().get();
                    int selectedVmIdx = changesState.getSelectedChanges().getSelectedIndex();
                    return new CodeReviewDiffViewModelsState<>(ListSelection.createAt(vms, selectedVmIdx), changesState);
                }));
            }
        };
        collector.accept(changes.get());
        Runnable unsubscribe = changes.addListener(collector);
        Disposer.register(parentDisposable, unsubscribe::run);
        return result;
    }
}
