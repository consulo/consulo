// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

final class PreLoadingCodeReviewAsyncDiffViewModelDelegateImpl<D, C, CVM extends AsyncDiffViewModel>
    implements PreLoadingCodeReviewAsyncDiffViewModelDelegate<C, CVM> {

    private final ObservableValue<Function<List<C>, List<C>>> myChangesPreProcessor;
    private final CodeReviewDiffViewModelFactory<D, C, CVM> myCreateViewModel;

    private final CodeReviewAsyncDiffViewModelDelegate<C> myDelegate = CodeReviewAsyncDiffViewModelDelegate.create();
    private final MutableObservableValue<@Nullable ComputedResult<CodeReviewDiffProcessorState<CVM>>> myChanges = MutableObservableValue.of(null);

    private final Object myLock = new Object();
    private @Nullable Disposable myDataScope;

    PreLoadingCodeReviewAsyncDiffViewModelDelegateImpl(Disposable parentDisposable,
                                                       ObservableValue<@Nullable ComputedResult<D>> preloadedData,
                                                       ObservableValue<Function<List<C>, List<C>>> changesPreProcessor,
                                                       CodeReviewDiffViewModelFactory<D, C, CVM> createViewModel) {
        myChangesPreProcessor = changesPreProcessor;
        myCreateViewModel = createViewModel;

        Consumer<@Nullable ComputedResult<D>> onData = dataLoadingResult -> handleData(parentDisposable, dataLoadingResult);
        onData.accept(preloadedData.get());
        Runnable unsubscribe = preloadedData.addListener(onData);
        Disposer.register(parentDisposable, () -> {
            unsubscribe.run();
            synchronized (myLock) {
                disposeDataScope();
            }
        });
    }

    @Override
    public ObservableValue<@Nullable ComputedResult<CodeReviewDiffProcessorState<CVM>>> getChanges() {
        return myChanges;
    }

    private void handleData(Disposable parentDisposable, @Nullable ComputedResult<D> dataLoadingResult) {
        synchronized (myLock) {
            disposeDataScope();
            if (dataLoadingResult == null) {
                myChanges.set(null);
                return;
            }
            if (dataLoadingResult.isInProgress()) {
                myChanges.set(ComputedResult.loading());
                return;
            }
            Throwable error = dataLoadingResult.exceptionOrNull();
            if (error != null) {
                myChanges.set(ComputedResult.failure(error));
                return;
            }
            Disposable scope = Disposable.newDisposable("PreLoadingCodeReviewAsyncDiffViewModelDelegate.data");
            Disposer.register(parentDisposable, scope);
            myDataScope = scope;
            D data = Objects.requireNonNull(dataLoadingResult.getOrNull());
            handleVmsState(scope, data);
        }
    }

    private void disposeDataScope() {
        Disposable scope = myDataScope;
        myDataScope = null;
        if (scope != null) {
            Disposer.dispose(scope);
        }
    }

    private void handleVmsState(Disposable scope, D preloadedData) {
        MappingScopedItemsContainer<C, CVM> vmsContainer = MappingScopedItemsContainer.byEquality(scope,
            (changeScope, change) -> myCreateViewModel.create(changeScope, preloadedData, change));

        Object[] last = {null, null, List.of()};
        Runnable recompute = () -> {
            synchronized (last) {
                Function<List<C>, List<C>> preProcessor = myChangesPreProcessor.get();
                CodeReviewDiffChangesState<C> changesState = myDelegate.getChangesToShow().get();
                try {
                    List<C> inputList = changesState.getSelectedChanges().getList();
                    if (!inputList.equals(last[0]) || preProcessor != last[1]) {
                        last[0] = inputList;
                        last[1] = preProcessor;
                        List<C> processed = preProcessor.apply(inputList);
                        last[2] = processed;
                        myChanges.set(ComputedResult.loading());
                        vmsContainer.update(processed);
                    }
                    @SuppressWarnings("unchecked")
                    List<C> lastProcessedList = (List<C>) last[2];
                    List<CVM> vms = vmsContainer.getMappedState().get();
                    ListSelection<C> selection = changesState.getSelectedChanges();
                    @Nullable C selectedItem = selection.getSelectedIndex() >= 0 && selection.getSelectedIndex() < inputList.size()
                        ? inputList.get(selection.getSelectedIndex())
                        : null;
                    int selectedVmIdx = selectedItem == null ? -1 : lastProcessedList.indexOf(selectedItem);
                    myChanges.set(ComputedResult.success(new CodeReviewDiffViewModelsState<>(ListSelection.createAt(vms, selectedVmIdx), changesState)));
                }
                catch (Exception e) {
                    myChanges.set(ComputedResult.failure(e));
                }
            }
        };
        recompute.run();
        Runnable changesUnsubscribe = myDelegate.getChangesToShow().addListener(it -> recompute.run());
        Runnable preProcessorUnsubscribe = myChangesPreProcessor.addListener(it -> recompute.run());
        Disposer.register(scope, () -> {
            changesUnsubscribe.run();
            preProcessorUnsubscribe.run();
        });
    }

    @Override
    public void showChanges(ListSelection<C> changes, @Nullable DiffViewerScrollRequest scrollRequest) {
        myDelegate.showChanges(changes, scrollRequest);
    }

    @Override
    public void showChange(C change, @Nullable DiffViewerScrollRequest scrollRequest) {
        myDelegate.showChange(change, scrollRequest);
    }

    @Override
    public void handleSelection(Disposable parentDisposable, Consumer<ListSelection<C>> listener) {
        myDelegate.handleSelection(parentDisposable, listener);
    }
}
