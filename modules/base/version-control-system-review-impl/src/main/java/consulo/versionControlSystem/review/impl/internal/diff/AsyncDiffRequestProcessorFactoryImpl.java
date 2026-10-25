// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.impl.internal.diff;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.application.util.ListSelection;
import consulo.diff.DiffContext;
import consulo.diff.DiffEditorViewer;
import consulo.diff.FrameDiffTool;
import consulo.diff.combined.CombinedBlockId;
import consulo.diff.combined.CombinedBlockProducer;
import consulo.diff.combined.CombinedDiffComponentProcessor;
import consulo.diff.combined.CombinedDiffManager;
import consulo.diff.combined.CombinedPathBlockId;
import consulo.diff.impl.internal.combined.CombinedDiffViewer;
import consulo.diff.impl.internal.combined.CombinedDiffViewerKeys;
import consulo.diff.impl.internal.viewer.DiffViewerBase;
import consulo.diff.impl.internal.viewer.DiffViewerListener;
import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.ErrorDiffRequest;
import consulo.diff.request.LoadingDiffRequest;
import consulo.diff.request.NoDiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.review.util.ComputedResult;
import consulo.review.util.KeyValuePair;
import consulo.review.util.ReviewUIUtil;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.PresentableChange;
import consulo.versionControlSystem.impl.internal.diff.MutableDiffRequestProcessorNavigator;
import consulo.versionControlSystem.review.diff.AsyncDiffRequestProcessorFactory;
import consulo.versionControlSystem.review.diff.model.AsyncDiffViewModel;
import consulo.versionControlSystem.review.diff.model.CodeReviewDiffProcessorState;
import consulo.versionControlSystem.review.diff.model.CodeReviewDiffProcessorViewModel;
import consulo.versionControlSystem.review.diff.model.DiffViewerScrollRequest;
import consulo.versionControlSystem.review.diff.model.DiffViewerScrollRequestProducer;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Singleton
@ServiceImpl
public class AsyncDiffRequestProcessorFactoryImpl implements AsyncDiffRequestProcessorFactory {
    private static final long PROGRESS_DISPLAY_DELAY = 100;

    private static final CombinedPathBlockId CONSTANT_BLOCK_ID = new CombinedPathBlockId(VcsUtil.getFilePath("/", false), null);

    @RequiredUIAccess
    @Override
    public <VM extends CodeReviewDiffProcessorViewModel<C>, C extends AsyncDiffViewModel> DiffEditorViewer createIn(
        Disposable parentDisposable,
        Project project,
        ObservableValue<@Nullable VM> diffVm,
        Function<VM, List<KeyValuePair<?>>> createContext,
        Function<C, PresentableChange> changePresenter
    ) {
        MutableDiffRequestProcessor processor = new MutableDiffRequestProcessor(project);
        Disposable lifetime = Disposable.newDisposable("Code Review Diff UI");
        Disposer.register(processor, lifetime);
        Disposer.register(parentDisposable, () -> Disposer.dispose(lifetime));

        SequentialDisposable vmSession = new SequentialDisposable(lifetime);
        ReviewUIUtil.bind(lifetime, diffVm, vm -> vmSession.restart(session -> {
            if (vm == null) {
                return;
            }
            List<KeyValuePair<?>> context = createContext.apply(vm);
            for (KeyValuePair<?> keyValue : context) {
                putData(processor, keyValue);
            }
            Disposer.register(session, () -> {
                // needs to run on disposal because editor release happens on viewer disposal
                processor.applyRequest(NoDiffRequest.INSTANCE);
                for (KeyValuePair<?> keyValue : context) {
                    processor.putContextUserData(keyValue.key(), null);
                }
            });
            handleChanges(session, vm, processor, changePresenter);
        }));
        return processor;
    }

    @RequiredUIAccess
    @Override
    public <VM extends CodeReviewDiffProcessorViewModel<C>, C extends AsyncDiffViewModel> CombinedDiffComponentProcessor createCombinedIn(
        Disposable parentDisposable,
        Project project,
        ObservableValue<@Nullable VM> reviewDiffVm,
        Function<VM, List<KeyValuePair<?>>> createContext,
        Function<C, PresentableChange> changeVmPresenter
    ) {
        CombinedDiffComponentProcessor processor = CombinedDiffManager.getInstance(project).createProcessor();
        Disposable lifetime = Disposable.newDisposable("Code Review Combined Diff UI");
        Disposer.register(processor, lifetime);
        Disposer.register(parentDisposable, () -> Disposer.dispose(lifetime));

        SequentialDisposable vmSession = new SequentialDisposable(lifetime);
        ReviewUIUtil.bind(lifetime, reviewDiffVm, diffVm -> vmSession.restart(session -> {
            if (diffVm == null) {
                return;
            }
            List<KeyValuePair<?>> context = createContext.apply(diffVm);
            for (KeyValuePair<?> keyValue : context) {
                putData(processor.getContext(), keyValue);
            }
            Disposer.register(session, () -> {
                for (KeyValuePair<?> keyValue : context) {
                    processor.getContext().putUserData(keyValue.key(), null);
                }
                processor.cleanBlocks();
            });
            handleCombinedChanges(session, diffVm, processor, changeVmPresenter);
        }));
        return processor;
    }

    @RequiredUIAccess
    private static <C extends AsyncDiffViewModel> void handleCombinedChanges(Disposable session,
                                                                             CodeReviewDiffProcessorViewModel<C> diffVm,
                                                                             CombinedDiffComponentProcessor processor,
                                                                             Function<C, PresentableChange> changePresenter) {
        SequentialDisposable stateSession = new SequentialDisposable(session);
        ReviewUIUtil.bind(session, diffVm.getChanges(), result -> stateSession.restart(scope -> {
            if (result == null) {
                setNewBlocks(processor, List.of());
                return;
            }
            if (result.isInProgress()) {
                applyDelayed(scope, () -> setNewBlocks(processor, List.of(new CombinedBlockProducer(
                    CONSTANT_BLOCK_ID,
                    new ConstantDiffRequestProducer("Loading", LoadingDiffRequest::new)
                ))));
                return;
            }
            Throwable error = result.exceptionOrNull();
            if (error != null) {
                setNewBlocks(processor, List.of(new CombinedBlockProducer(
                    CONSTANT_BLOCK_ID,
                    new ConstantDiffRequestProducer("Error", () -> new ErrorDiffRequest(error))
                )));
                return;
            }
            CodeReviewDiffProcessorState<C> state = result.getOrNull();
            if (state != null) {
                handleCombinedState(processor, state, changePresenter);
            }
        }));
    }

    @RequiredUIAccess
    private static void setNewBlocks(CombinedDiffComponentProcessor processor, List<CombinedBlockProducer> blocks) {
        processor.cleanBlocks();
        processor.setBlocks(blocks);
    }

    @RequiredUIAccess
    private static <C extends AsyncDiffViewModel> void handleCombinedState(CombinedDiffComponentProcessor processor,
                                                                           CodeReviewDiffProcessorState<C> state,
                                                                           Function<C, PresentableChange> changePresenter) {
        Map<CombinedPathBlockId, C> vms = new LinkedHashMap<>();
        for (C vm : state.getSelectedChanges().getList()) {
            PresentableChange presentation = changePresenter.apply(vm);
            vms.put(new CombinedPathBlockId(presentation.getFilePath(), presentation.getFileStatus()), vm);
        }
        List<CombinedBlockId> current = new ArrayList<>();
        for (CombinedBlockProducer block : processor.getBlocks()) {
            current.add(block.getId());
        }
        if (current.size() != vms.size() || !current.containsAll(vms.keySet())) {
            List<CombinedBlockProducer> blocks = new ArrayList<>();
            for (Map.Entry<CombinedPathBlockId, C> entry : vms.entrySet()) {
                blocks.add(new CombinedBlockProducer(entry.getKey(), new AsyncDiffViewModelRequestProducer(entry.getValue(), entry.getKey())));
            }
            setNewBlocks(processor, blocks);
        }

        // fixme: fix after selection rework
        ListSelection<C> selection = state.getSelectedChanges();
        int selectedIndex = selection.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < selection.getList().size()) {
            PresentableChange presentation = changePresenter.apply(selection.getList().get(selectedIndex));
            CombinedPathBlockId selectedBlock = new CombinedPathBlockId(presentation.getFilePath(), presentation.getFileStatus());
            CombinedDiffViewer viewer = processor.getContext().getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY);
            if (viewer != null && vms.containsKey(selectedBlock)) {
                viewer.selectDiffBlock(selectedBlock, false);
            }
        }
    }

    @RequiredUIAccess
    private static <C extends AsyncDiffViewModel> void handleChanges(Disposable session,
                                                                     CodeReviewDiffProcessorViewModel<C> diffVm,
                                                                     MutableDiffRequestProcessor processor,
                                                                     Function<C, PresentableChange> changePresenter) {
        SequentialDisposable stateSession = new SequentialDisposable(session);
        ReviewUIUtil.bind(session, diffVm.getChanges(), result -> stateSession.restart(scope -> {
            if (result == null) {
                processor.applyRequest(NoDiffRequest.INSTANCE);
                return;
            }
            if (result.isInProgress()) {
                applyDelayed(scope, () -> processor.applyRequest(new LoadingDiffRequest()));
                return;
            }
            Throwable error = result.exceptionOrNull();
            if (error != null) {
                processor.applyRequest(new ErrorDiffRequest(error));
                return;
            }
            CodeReviewDiffProcessorState<C> state = result.getOrNull();
            if (state != null) {
                handleState(scope, diffVm, processor, state, changePresenter);
            }
        }));
    }

    @RequiredUIAccess
    private static <C extends AsyncDiffViewModel> void handleState(Disposable scope,
                                                                   CodeReviewDiffProcessorViewModel<C> processorVm,
                                                                   MutableDiffRequestProcessor processor,
                                                                   CodeReviewDiffProcessorState<C> state,
                                                                   Function<C, PresentableChange> changePresenter) {
        ListSelection<C> selection = state.getSelectedChanges();
        int selectedIndex = selection.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= selection.getList().size()) {
            processor.applyRequest(NoDiffRequest.INSTANCE);
            return;
        }
        C diffVm = selection.getList().get(selectedIndex);

        processor.setNavigator(new StateNavigator<>(processorVm, state, changePresenter));
        Disposer.register(scope, () -> processor.setNavigator(MutableDiffRequestProcessorNavigator.empty()));

        showDiff(scope, processor, diffVm, state instanceof DiffViewerScrollRequestProducer producer ? producer : null);
    }

    @RequiredUIAccess
    private static void showDiff(Disposable scope,
                                 MutableDiffRequestProcessor processor,
                                 AsyncDiffViewModel diffVm,
                                 @Nullable DiffViewerScrollRequestProducer scrollRequests) {
        SequentialDisposable requestSession = new SequentialDisposable(scope);
        ReviewUIUtil.bind(scope, diffVm.getRequest(), requestResult -> requestSession.restart(requestScope -> {
            if (requestResult == null) {
                applyRequestUpdateable(requestScope, processor, NoDiffRequest.INSTANCE, diffVm, null);
                return;
            }
            if (requestResult.isInProgress()) {
                applyDelayed(requestScope, () -> applyRequestUpdateable(requestScope, processor, new LoadingDiffRequest(), diffVm, null));
                return;
            }
            Throwable error = requestResult.exceptionOrNull();
            DiffRequest request = requestResult.getOrNull();
            if (error != null || request == null) {
                applyRequestUpdateable(requestScope, processor, new ErrorDiffRequest(error != null ? error : new IllegalStateException()), diffVm, null);
                return;
            }
            applyRequestUpdateable(requestScope, processor, request, diffVm, scrollRequests);
        }));
    }

    /**
     * Apply the request and re-apply it on every forced update, until the request scope ends; a reload asks the view
     * model for a new request
     */
    @RequiredUIAccess
    private static void applyRequestUpdateable(Disposable requestScope,
                                               MutableDiffRequestProcessor processor,
                                               DiffRequest request,
                                               AsyncDiffViewModel diffVm,
                                               @Nullable DiffViewerScrollRequestProducer scrollRequests) {
        SequentialDisposable applySession = new SequentialDisposable(requestScope);
        Runnable apply = () -> applySession.restart(applyScope -> {
            processor.applyRequest(request, true);
            if (scrollRequests != null) {
                handleScrolling(applyScope, processor, request, scrollRequests);
            }
        });
        apply.run();
        processor.addUpdateSignalListener(requestScope, new MutableDiffRequestProcessorUpdateListener() {
            /**
             * scrollToChangePolicy is NOT null only when invoked from navigation popup, but in this case we handle scrolling asynchronously,
             * so it's fine to ignore this parameter altogether
             * Also since the VM now controls the current request, there's not much sense to react to non-forced update
             * since it will be handled by state collector
             */
            @Override
            public void onUpdateRequested(boolean force, @Nullable ScrollToPolicy scrollToChangePolicy) {
                if (force) {
                    apply.run();
                }
            }

            @Override
            public void onReloadRequested() {
                diffVm.reloadRequest();
            }
        });
    }

    @RequiredUIAccess
    private static void handleScrolling(Disposable applyScope,
                                        MutableDiffRequestProcessor processor,
                                        DiffRequest request,
                                        DiffViewerScrollRequestProducer producer) {
        UIAccess uiAccess = UIAccess.current();
        producer.handleScrollRequests(applyScope, cmd -> uiAccess.give(() -> {
            if (Disposer.isDisposed(applyScope)) {
                return;
            }
            ApplicationIdeFocusManager.getInstance().doWhenFocusSettlesDown(() -> {
                if (!Disposer.isDisposed(applyScope) && processor.getActiveRequest() == request) {
                    executeScroll(applyScope, processor.getActiveViewer(), cmd);
                }
            });
        }));
    }

    @RequiredUIAccess
    private static void executeScroll(Disposable applyScope, FrameDiffTool.@Nullable DiffViewer viewer, DiffViewerScrollRequest cmd) {
        if (!(viewer instanceof DiffViewerBase viewerBase)) {
            return;
        }
        if (!viewerBase.hasPendingRediff()) {
            DiffViewerScrollRequestProcessor.scroll(viewerBase, cmd);
            return;
        }
        Disposable waiting = Disposable.newDisposable("DiffViewerScrollRequest");
        Disposer.register(applyScope, waiting);
        DiffViewerListener listener = new DiffViewerListener() {
            @Override
            protected void onAfterRediff() {
                if (!viewerBase.hasPendingRediff()) {
                    Disposer.dispose(waiting);
                    DiffViewerScrollRequestProcessor.scroll(viewerBase, cmd);
                }
            }
        };
        viewerBase.addListener(listener);
        Disposer.register(waiting, () -> viewerBase.removeListener(listener));
    }

    @RequiredUIAccess
    private static void applyDelayed(Disposable scope, @RequiredUIAccess Runnable action) {
        UIAccess uiAccess = UIAccess.current();
        Future<?> future = uiAccess.getScheduler().schedule(() -> uiAccess.give(() -> {
            if (!Disposer.isDisposed(scope)) {
                action.run();
            }
        }), PROGRESS_DISPLAY_DELAY, TimeUnit.MILLISECONDS);
        Disposer.register(scope, () -> future.cancel(false));
    }

    private static <T> void putData(MutableDiffRequestProcessor processor, KeyValuePair<T> keyValue) {
        processor.putContextUserData(keyValue.key(), keyValue.value());
    }

    private static <T> void putData(DiffContext context, KeyValuePair<T> keyValue) {
        context.putUserData(keyValue.key(), keyValue.value());
    }
}
