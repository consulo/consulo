// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff;

import com.intellij.collaboration.async.CoroutineUtilKt;
import com.intellij.collaboration.ui.codereview.diff.model.AsyncDiffViewModel;
import com.intellij.collaboration.ui.codereview.diff.model.CodeReviewDiffProcessorViewModel;
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest;
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequestProducer;
import com.intellij.collaboration.util.KeyValuePair;
import consulo.application.util.ListSelection;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Function;

import static com.intellij.diff.tools.combined.CombinedDiffViewerKt.COMBINED_DIFF_VIEWER_KEY;

public final class AsyncDiffRequestProcessorFactory {
    private AsyncDiffRequestProcessorFactory() {
    }

    private static final CombinedPathBlockId CONSTANT_BLOCK_ID = new CombinedPathBlockId(new LocalFilePath("/", false), null);

    //region Classic Diff

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <VM extends CodeReviewDiffProcessorViewModel<C> & Object, C extends AsyncDiffViewModel>
    @Nonnull DiffRequestProcessor createIn(
        @Nonnull CoroutineScope cs,
        @Nonnull Project project,
        @Nonnull Flow<@Nullable VM> diffVmFlow,
        @Nonnull @RequiresEdt Function<VM, List<KeyValuePair<?>>> createContext,
        @Nonnull @RequiresEdt Function<C, PresentableChange> changePresenter
    ) {
        MutableDiffRequestProcessor processor = new MutableDiffRequestProcessor(project);
        var job = CoroutineUtilKt.launchNow(cs, "Code Review Diff UI", (scope, cont) -> {
            CoroutineUtilKt.collectScoped(diffVmFlow, vm -> {
                if (vm != null) {
                    List<KeyValuePair<?>> context = createContext.apply(vm);
                    try {
                        for (KeyValuePair<?> kv : context) {
                            putData(processor, kv);
                        }
                        handleChanges(vm, processor, changePresenter, cont);
                    }
                    finally {
                        processor.applyRequest(NoDiffRequest.INSTANCE);
                        for (KeyValuePair<?> kv : context) {
                            clearData(processor, kv);
                        }
                    }
                }
                return Unit.INSTANCE;
            }, cont);
            return Unit.INSTANCE;
        });
        com.intellij.util.CancelOnDisposeKt.cancelOnDispose(job, processor);
        return processor;
    }

    @SuppressWarnings("unchecked")
    private static <C extends AsyncDiffViewModel> @Nullable Object handleChanges(
        @Nonnull CodeReviewDiffProcessorViewModel<C> diffVm,
        @Nonnull MutableDiffRequestProcessor processor,
        @Nonnull Function<C, PresentableChange> changePresenter,
        @Nonnull Continuation<?> $completion
    ) {
        return CoroutineUtilKt.collectScoped(diffVm.getChanges(), result -> {
                if (result != null) {
                    var r = result.getResult();
                    if (r == null) {
                        // in progress
                        kotlinx.coroutines.DelayKt.delay(DiffUIUtil.PROGRESS_DISPLAY_DELAY, $completion);
                        processor.applyRequest(new LoadingDiffRequest());
                    }
                    else if (r.isFailure()) {
                        processor.applyRequest(new ErrorDiffRequest(r.exceptionOrNull()));
                    }
                    else {
                        CodeReviewDiffProcessorViewModel.State<C> state =
                            (CodeReviewDiffProcessorViewModel.State<C>) r.getOrNull();
                        handleState(diffVm, processor, state, changePresenter);
                    }
                }
                else {
                    processor.applyRequest(NoDiffRequest.INSTANCE);
                }
                return Unit.INSTANCE;
            },
            $completion
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <C extends AsyncDiffViewModel> void handleState(
        @Nonnull CodeReviewDiffProcessorViewModel<C> processorVm,
        @Nonnull MutableDiffRequestProcessor processor,
        @Nonnull CodeReviewDiffProcessorViewModel.State<C> state,
        @Nonnull Function<C, PresentableChange> changePresenter
    ) {
        ListSelection<C> selectedChanges = state.getSelectedChanges();
        C diffVm = selectedChanges.getSelectedIndex() >= 0 && selectedChanges.getSelectedIndex() < selectedChanges.getList().size()
            ? selectedChanges.getList().get(selectedChanges.getSelectedIndex()) : null;
        if (diffVm == null) {
            processor.applyRequest(NoDiffRequest.INSTANCE);
            return;
        }
        try {
            processor.setNavigator(new StateNavigator<>(processorVm, state, changePresenter));
            AsyncDiffViewModel vm;
            if (state instanceof DiffViewerScrollRequestProducer scrollProducer) {
                vm = new ScrollableAsyncDiffViewModel(diffVm, scrollProducer);
            }
            else {
                vm = diffVm;
            }
            // processor.showDiff(vm) - calls the extension function
            MutableDiffRequestProcessorUtil.showDiff(processor, vm, null);
        }
        finally {
            processor.setNavigator(MutableDiffRequestProcessor.Navigator.empty());
        }
    }

    private static final class StateNavigator<C> implements MutableDiffRequestProcessor.Navigator<C> {
        private final @Nonnull CodeReviewDiffProcessorViewModel<C> processorVm;
        private final @Nonnull CodeReviewDiffProcessorViewModel.State<C> state;
        private final @Nonnull Function<C, PresentableChange> changePresenter;

        StateNavigator(
            @Nonnull CodeReviewDiffProcessorViewModel<C> processorVm,
            @Nonnull CodeReviewDiffProcessorViewModel.State<C> state,
            @Nonnull Function<C, PresentableChange> changePresenter
        ) {
            this.processorVm = processorVm;
            this.state = state;
            this.changePresenter = changePresenter;
        }

        @Override
        public @Nonnull ListSelection<C> getCurrentList() {
            return state.getSelectedChanges();
        }

        @Override
        public void selectPrev(boolean fromDifferences) {
            int selectedIndex = state.getSelectedChanges().getSelectedIndex();
            Integer newIdx = selectedIndex <= 0 ? null : selectedIndex - 1;
            if (newIdx != null) {
                DiffViewerScrollRequest scrollCommand = fromDifferences ? DiffViewerScrollRequest.toLastChange() : null;
                processorVm.showChange(newIdx, scrollCommand);
            }
        }

        @Override
        public void selectNext(boolean fromDifferences) {
            ListSelection<C> sel = state.getSelectedChanges();
            int selectedIndex = sel.getSelectedIndex();
            Integer newIdx = (selectedIndex >= 0 && selectedIndex < sel.getList().size() - 1) ? selectedIndex + 1 : null;
            if (newIdx != null) {
                DiffViewerScrollRequest scrollCommand = fromDifferences ? DiffViewerScrollRequest.toFirstChange() : null;
                processorVm.showChange(newIdx, scrollCommand);
            }
        }

        @Override
        public void select(@Nonnull C change) {
            processorVm.showChange(change);
        }

        @Override
        public @Nullable PresentableChange getChangePresentation(@Nonnull C change) {
            return changePresenter.apply(change);
        }
    }

    private static final class ScrollableAsyncDiffViewModel implements AsyncDiffViewModel, DiffViewerScrollRequestProducer {
        private final @Nonnull AsyncDiffViewModel original;
        private final @Nonnull DiffViewerScrollRequestProducer scrollRequestProducer;

        ScrollableAsyncDiffViewModel(
            @Nonnull AsyncDiffViewModel original,
            @Nonnull DiffViewerScrollRequestProducer scrollRequestProducer
        ) {
            this.original = original;
            this.scrollRequestProducer = scrollRequestProducer;
        }

        @Override
        public @Nonnull StateFlow getRequest() {
            return original.getRequest();
        }

        @Override
        public void reloadRequest() {
            original.reloadRequest();
        }

        @Override
        public @Nonnull Flow<DiffViewerScrollRequest> getScrollRequests() {
            return scrollRequestProducer.getScrollRequests();
        }
    }
    //endregion

    //region Combined Diff

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <VM extends CodeReviewDiffProcessorViewModel<C>, C extends AsyncDiffViewModel>
    @Nonnull CombinedDiffComponentProcessor createCombinedIn(
        @Nonnull CoroutineScope cs,
        @Nonnull Project project,
        @Nonnull Flow<@Nullable VM> reviewDiffVm,
        @Nonnull @RequiresEdt Function<VM, List<KeyValuePair<?>>> createContext,
        @Nonnull @RequiresEdt Function<C, PresentableChange> changeVmPresenter
    ) {
        CombinedDiffComponentProcessor processor = CombinedDiffManager.getInstance(project).createProcessor();
        var job = CoroutineUtilKt.launchNow(cs, "Code Review Combined Diff UI", (scope, cont) -> {
            FlowKt.collectLatest(reviewDiffVm, (diffVm, cont2) -> {
                if (diffVm != null) {
                    List<KeyValuePair<?>> context = createContext.apply(diffVm);
                    try {
                        for (KeyValuePair<?> kv : context) {
                            putDataOnContext(processor.getContext(), kv);
                        }
                        handleCombinedChanges(diffVm, processor, changeVmPresenter, cont2);
                        kotlinx.coroutines.AwaitKt.awaitCancellation(cont2);
                    }
                    finally {
                        for (KeyValuePair<?> kv : context) {
                            clearDataOnContext(processor.getContext(), kv);
                        }
                        processor.cleanBlocks();
                    }
                }
                return Unit.INSTANCE;
            }, cont);
            return Unit.INSTANCE;
        });
        com.intellij.util.CancelOnDisposeKt.cancelOnDispose(job, processor.getDisposable());
        return processor;
    }

    @SuppressWarnings("unchecked")
    private static <C extends AsyncDiffViewModel> @Nullable Object handleCombinedChanges(
        @Nonnull CodeReviewDiffProcessorViewModel<C> diffVm,
        @Nonnull CombinedDiffComponentProcessor processor,
        @Nonnull Function<C, PresentableChange> changePresenter,
        @Nonnull Continuation<?> $completion
    ) {
        return FlowKt.collectLatest(diffVm.getChanges(), (result, cont) -> {
            if (result != null) {
                var r = result.getResult();
                if (r == null) {
                    // in progress
                    kotlinx.coroutines.DelayKt.delay(DiffUIUtil.PROGRESS_DISPLAY_DELAY, cont);
                    setNewBlocks(processor, List.of(new CombinedBlockProducer(CONSTANT_BLOCK_ID, DiffUIUtil.LOADING_PRODUCER)));
                }
                else if (r.isFailure()) {
                    setNewBlocks(
                        processor,
                        List.of(new CombinedBlockProducer(CONSTANT_BLOCK_ID, DiffUIUtil.createErrorProducer(r.exceptionOrNull())))
                    );
                }
                else {
                    CodeReviewDiffProcessorViewModel.State<C> state =
                        (CodeReviewDiffProcessorViewModel.State<C>) r.getOrNull();
                    handleCombinedState(processor, state, changePresenter);
                }
            }
            else {
                setNewBlocks(processor, List.of());
            }
            return Unit.INSTANCE;
        }, $completion);
    }

    private static void setNewBlocks(
        @Nonnull CombinedDiffComponentProcessor processor,
        @Nullable List<CombinedBlockProducer> blocks
    ) {
        processor.cleanBlocks();
        processor.setBlocks(blocks != null ? blocks : List.of());
    }

    private static <C extends AsyncDiffViewModel> void handleCombinedState(
        @Nonnull CombinedDiffComponentProcessor processor,
        @Nonnull CodeReviewDiffProcessorViewModel.State<C> state,
        @Nonnull Function<C, PresentableChange> changePresenter
    ) {
        LinkedHashMap<CombinedPathBlockId, C> vms = new LinkedHashMap<>();
        for (C item : state.getSelectedChanges().getList()) {
            PresentableChange presentation = changePresenter.apply(item);
            CombinedPathBlockId id = new CombinedPathBlockId(presentation.getFilePath(), presentation.getFileStatus());
            vms.put(id, item);
        }

        List<CombinedPathBlockId> current = new ArrayList<>();
        for (var block : processor.getBlocks()) {
            current.add(block.getId());
        }

        if (current.size() != vms.size() || !new HashSet<>(current).containsAll(vms.keySet())) {
            List<CombinedBlockProducer> blocks = new ArrayList<>();
            for (var entry : vms.entrySet()) {
                blocks.add(new CombinedBlockProducer(
                    entry.getKey(),
                    new AsyncDiffViewModelRequestProducer(entry.getValue(), entry.getKey())
                ));
            }
            setNewBlocks(processor, blocks);
        }

        // fixme: fix after selection rework
        ListSelection<C> sel = state.getSelectedChanges();
        C selectedChange = sel.getSelectedIndex() >= 0 && sel.getSelectedIndex() < sel.getList().size()
            ? sel.getList().get(sel.getSelectedIndex()) : null;
        if (selectedChange != null) {
            PresentableChange presentation = changePresenter.apply(selectedChange);
            CombinedPathBlockId selectedBlock = new CombinedPathBlockId(presentation.getFilePath(), presentation.getFileStatus());
            var combinedViewer = processor.getContext().getUserData(COMBINED_DIFF_VIEWER_KEY);
            if (combinedViewer != null) {
                combinedViewer.selectDiffBlock(selectedBlock, false);
            }
        }
    }

    private static final class AsyncDiffViewModelRequestProducer implements DiffRequestProducer, PresentableChange {
        private final @Nonnull AsyncDiffViewModel model;
        private final @Nonnull CombinedPathBlockId id;

        AsyncDiffViewModelRequestProducer(@Nonnull AsyncDiffViewModel model, @Nonnull CombinedPathBlockId id) {
            this.model = model;
            this.id = id;
        }

        @Override
        public @Nonnull FilePath getFilePath() {
            return id.getPath();
        }

        @Override
        public @Nonnull FileStatus getFileStatus() {
            FileStatus status = id.getFileStatus();
            return status != null ? status : FileStatus.UNKNOWN;
        }

        @Override
        public @Nonnull String getName() {
            return id.getPath().getPath();
        }

        @Override
        public @Nonnull DiffRequest process(@Nonnull UserDataHolder context, @Nonnull ProgressIndicator indicator) {
            return RunBlockingCancellableKt.runBlockingCancellable(indicator, (scope, cont) -> {
                return FlowKt.first(
                    FlowKt.mapNotNull(model.getRequest(), r -> r != null ? r.getResult() : null, cont),
                    cont
                );
            });
        }
    }
    //endregion

    @SuppressWarnings("unchecked")
    private static <T> void putData(@Nonnull DiffRequestProcessor processor, @Nonnull KeyValuePair<T> keyValue) {
        processor.putContextUserData(keyValue.getKey(), keyValue.getValue());
    }

    private static void clearData(@Nonnull DiffRequestProcessor processor, @Nonnull KeyValuePair<?> keyValue) {
        processor.putContextUserData(keyValue.getKey(), null);
    }

    @SuppressWarnings("unchecked")
    private static <T> void putDataOnContext(@Nonnull UserDataHolder context, @Nonnull KeyValuePair<T> keyValue) {
        context.putUserData(keyValue.getKey(), keyValue.getValue());
    }

    private static void clearDataOnContext(@Nonnull UserDataHolder context, @Nonnull KeyValuePair<?> keyValue) {
        context.putUserData(keyValue.getKey(), null);
    }
}
