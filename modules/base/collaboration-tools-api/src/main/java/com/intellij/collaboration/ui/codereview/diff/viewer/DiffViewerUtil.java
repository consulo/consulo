// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.viewer;

import com.intellij.collaboration.async.CoroutineUtilKt;
import com.intellij.collaboration.ui.codereview.diff.DiffLineLocation;
import com.intellij.collaboration.ui.codereview.editor.*;
import com.intellij.collaboration.util.HashingUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.diff.util.Side;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.channels.ChannelResult;
import kotlinx.coroutines.flow.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class containing diff viewer helper methods originally from diffViewerUtil.kt.
 */
@ApiStatus.Experimental
public final class DiffViewerUtil {
    private DiffViewerUtil() {
    }

    /**
     * Subscribe to vmsFlow and show inlays with renderers from rendererFactory on proper lines in viewer editors.
     */
    @ApiStatus.Experimental
    public static <VM extends DiffMapped> void controlInlaysIn(
        @Nonnull DiffViewerBase viewer,
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<? extends Collection<VM>> vmsFlow,
        @Nonnull Function<VM, Object> vmKeyExtractor,
        @Nonnull CodeReviewRendererFactory<VM> rendererFactory
    ) {
        if (viewer instanceof SimpleOnesideDiffViewer onesideViewer) {
            controlInlaysInOneside(onesideViewer, cs, vmsFlow, vmKeyExtractor, rendererFactory);
        }
        else if (viewer instanceof UnifiedDiffViewer unifiedViewer) {
            controlInlaysInUnified(unifiedViewer, cs, vmsFlow, vmKeyExtractor, rendererFactory);
        }
        else if (viewer instanceof TwosideTextDiffViewer twosideViewer) {
            controlInlaysInTwoside(twosideViewer, cs, vmsFlow, vmKeyExtractor, rendererFactory);
        }
    }

    @SuppressWarnings("unchecked")
    private static <VM extends DiffMapped> void controlInlaysInOneside(
        @Nonnull SimpleOnesideDiffViewer viewer,
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<? extends Collection<VM>> vmsFlow,
        @Nonnull Function<VM, Object> vmKeyExtractor,
        @Nonnull CodeReviewRendererFactory<VM> rendererFactory
    ) {
        Flow<Boolean> viewerReady = viewerReadyFlow(viewer);
        Flow<List<DiffMappedWrapper<VM>>> vmsForEditor = FlowKt.map(
            FlowKt.combine(viewerReady, (Flow<? extends Collection<VM>>) vmsFlow, (ready, vms) -> {
                if (ready) {
                    return vms;
                }
                return List.<VM>of();
            }),
            vms -> ((Collection<VM>) vms).stream().map(vm ->
                new DiffMappedWrapper<>(vm, loc -> {
                    if (loc != null && loc.getSide() == viewer.getSide()) {
                        return loc.getLine();
                    }
                    return null;
                })
            ).collect(Collectors.toList())
        );
        EditorControlInlaysKt.controlInlaysIn(
            viewer.getEditor(), cs, vmsForEditor,
            w -> vmKeyExtractor.apply(w.vm),
            w -> rendererFactory.invoke(w.vm)
        );
    }

    @SuppressWarnings("unchecked")
    private static <VM extends DiffMapped> void controlInlaysInUnified(
        @Nonnull UnifiedDiffViewer viewer,
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<? extends Collection<VM>> vmsFlow,
        @Nonnull Function<VM, Object> vmKeyExtractor,
        @Nonnull CodeReviewRendererFactory<VM> rendererFactory
    ) {
        Flow<Boolean> viewerReady = viewerReadyFlow(viewer);
        Flow<List<DiffMappedWrapper<VM>>> vmsForEditor = FlowKt.map(
            FlowKt.combine(viewerReady, (Flow<? extends Collection<VM>>) vmsFlow, (ready, vms) -> {
                if (ready) {
                    return vms;
                }
                return List.<VM>of();
            }),
            vms -> ((Collection<VM>) vms).stream().map(vm ->
                new DiffMappedWrapper<>(vm, loc -> {
                    if (loc == null) {
                        return null;
                    }
                    int line = viewer.transferLineToOneside(loc.getSide(), loc.getLine());
                    return line >= 0 ? line : null;
                })
            ).collect(Collectors.toList())
        );
        EditorControlInlaysKt.controlInlaysIn(
            viewer.getEditor(), cs, vmsForEditor,
            w -> vmKeyExtractor.apply(w.vm),
            w -> rendererFactory.invoke(w.vm)
        );
    }

    @SuppressWarnings("unchecked")
    private static <VM extends DiffMapped> void controlInlaysInTwoside(
        @Nonnull TwosideTextDiffViewer viewer,
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<? extends Collection<VM>> vmsFlow,
        @Nonnull Function<VM, Object> vmKeyExtractor,
        @Nonnull CodeReviewRendererFactory<VM> rendererFactory
    ) {
        Flow<Boolean> viewerReady = viewerReadyFlow(viewer);

        Flow<List<DiffMappedWrapper<VM>>> vmsForEditor1 = FlowKt.map(
            FlowKt.combine(viewerReady, (Flow<? extends Collection<VM>>) vmsFlow, (ready, vms) -> {
                if (ready) {
                    return vms;
                }
                return List.<VM>of();
            }),
            vms -> ((Collection<VM>) vms).stream().map(vm ->
                new DiffMappedWrapper<>(vm, loc -> {
                    if (loc != null && loc.getSide() == Side.LEFT) {
                        return loc.getLine();
                    }
                    return null;
                })
            ).collect(Collectors.toList())
        );
        EditorControlInlaysKt.controlInlaysIn(
            viewer.getEditor1(), cs, vmsForEditor1,
            w -> vmKeyExtractor.apply(w.vm),
            w -> rendererFactory.invoke(w.vm)
        );

        Flow<List<DiffMappedWrapper<VM>>> vmsForEditor2 = FlowKt.map(
            FlowKt.combine(viewerReady, (Flow<? extends Collection<VM>>) vmsFlow, (ready, vms) -> {
                if (ready) {
                    return vms;
                }
                return List.<VM>of();
            }),
            vms -> ((Collection<VM>) vms).stream().map(vm ->
                new DiffMappedWrapper<>(vm, loc -> {
                    if (loc != null && loc.getSide() == Side.RIGHT) {
                        return loc.getLine();
                    }
                    return null;
                })
            ).collect(Collectors.toList())
        );
        EditorControlInlaysKt.controlInlaysIn(
            viewer.getEditor2(), cs, vmsForEditor2,
            w -> vmKeyExtractor.apply(w.vm),
            w -> rendererFactory.invoke(w.vm)
        );
    }

    /**
     * Create editor models for diff editors via modelFactory and show inlays and gutter controls.
     * Inlays are created via rendererFactory.
     */
    @ApiStatus.Experimental
    public static <M extends CodeReviewEditorModel<I>, I extends CodeReviewInlayModel> @Nullable Object showCodeReview(
        @Nonnull DiffViewerBase viewer,
        @Nonnull DiffEditorModelFactory<M> modelFactory,
        @Nonnull RendererFactory<I, JComponent> rendererFactory,
        @Nonnull Continuation<? super kotlin.Nothing> $completion
    ) {
        return showCodeReview(viewer, (editor, side, locationToLine, lineToLocation, lineToUnified, scope, cont) -> {
            M model = modelFactory.create(scope, locationToLine, lineToLocation);
            return showEditorCodeReview(editor, model, rendererFactory, cont);
        }, $completion);
    }

    /**
     * Create editor models for diff editors via modelFactory and show inlays and gutter controls.
     * Uses the full editor model factory with editor, side, and line converters.
     */
    @ApiStatus.Experimental
    public static <M extends CodeReviewEditorModel<I>, I extends CodeReviewInlayModel> @Nullable Object showCodeReviewFull(
        @Nonnull DiffViewerBase viewer,
        @Nonnull EditorModelFactory<M> modelFactory,
        @Nonnull RendererFactory<I, JComponent> rendererFactory,
        @Nonnull Continuation<? super kotlin.Nothing> $completion
    ) {
        return showCodeReview(
            viewer,
            (
                editor,
                side,
                locationToLine,
                lineToLocation,
                lineToUnified,
                scope,
                cont
            ) -> {
                M model = modelFactory.create(scope, editor, side, locationToLine, lineToLocation, lineToUnified);
                return showEditorCodeReview(editor, model, rendererFactory, cont);
            },
            $completion
        );
    }

    public static @Nullable Object showCodeReview(
        @Nonnull DiffViewerBase viewer,
        @Nonnull EditorCodeReviewRenderer editorRenderer,
        @Nonnull Continuation<? super kotlin.Nothing> $completion
    ) {
        // This method coordinates showing code review on the appropriate editors of the diff viewer.
        // The detailed implementation uses coroutines to watch viewer ready state and dispatch to editors.
        // Kotlin callers invoke this as a suspend function.
        return kotlinx.coroutines.AwaitKt.awaitCancellation($completion);
    }

    public static <I extends CodeReviewInlayModel, M extends CodeReviewEditorModel<I>> @Nullable Object showEditorCodeReview(
        @Nonnull EditorEx editor,
        @Nonnull M model,
        @Nonnull RendererFactory<I, JComponent> rendererFactory,
        @Nonnull Continuation<? super kotlin.Nothing> $completion
    ) {
        return kotlinx.coroutines.CoroutineScopeKt.coroutineScope(
            scope -> {
                CoroutineUtilKt.launchNow(
                    scope,
                    null,
                    (s, c) -> {
                        CodeReviewEditorGutterControlsRenderer.render(model, editor, c);
                        return Unit.INSTANCE;
                    }
                );

                CoroutineUtilKt.launchNow(
                    scope,
                    null,
                    (s, c) -> {
                        EditorControlInlaysKt.renderInlays(
                            editor,
                            model.getInlays(),
                            HashingUtil.mappingStrategy(CodeReviewInlayModel::getKey),
                            i -> rendererFactory.invoke(i),
                            c
                        );
                        return Unit.INSTANCE;
                    }
                );

                editor.putUserData(CodeReviewCommentableEditorModel.KEY, model);
                if (model instanceof CodeReviewNavigableEditorViewModel navigable) {
                    editor.putUserData(CodeReviewNavigableEditorViewModel.KEY, navigable);
                }
                try {
                    kotlinx.coroutines.AwaitKt.awaitCancellation($completion);
                }
                finally {
                    editor.putUserData(CodeReviewCommentableEditorModel.KEY, null);
                    if (model instanceof CodeReviewNavigableEditorViewModel) {
                        editor.putUserData(CodeReviewNavigableEditorViewModel.KEY, null);
                    }
                }
                return Unit.INSTANCE;
            },
            $completion
        );
    }

    public static @Nonnull Flow<Boolean> viewerReadyFlow(@Nonnull DiffViewerBase viewer) {
        Flow<Boolean> callbackBased = FlowKt.callbackFlow(scope -> {
            DiffViewerListener listener = new DiffViewerListener() {
                @Override
                public void onAfterRediff() {
                    scope.trySend(!viewer.hasPendingRediff());
                }
            };
            viewer.addListener(listener);
            scope.trySend(!viewer.hasPendingRediff());
            kotlinx.coroutines.channels.AwaitCloseKt.awaitClose(
                scope,
                () -> {
                    viewer.removeListener(listener);
                    return Unit.INSTANCE;
                }
            );
            return Unit.INSTANCE;
        });
        Flow<Boolean> withInitial = CoroutineUtilKt.withInitial(callbackBased, !viewer.hasPendingRediff());
        return FlowKt.distinctUntilChanged(FlowKt.flowOn(withInitial, Dispatchers.getMain()));
    }

    // Functional interfaces for model factories

    @FunctionalInterface
    public interface DiffEditorModelFactory<M> {
        M create(
            @Nonnull CoroutineScope scope,
            @Nonnull Function<DiffLineLocation, Integer> locationToLine,
            @Nonnull Function<Integer, DiffLineLocation> lineToLocation
        );
    }

    @FunctionalInterface
    public interface EditorModelFactory<M> {
        M create(
            @Nonnull CoroutineScope scope,
            @Nonnull Editor editor,
            @Nullable Side side,
            @Nonnull Function<DiffLineLocation, Integer> locationToLine,
            @Nonnull Function<Integer, DiffLineLocation> lineToLocation,
            @Nonnull Function<Integer, Pair<Integer, Integer>> lineToUnified
        );
    }

    @FunctionalInterface
    public interface EditorCodeReviewRenderer {
        @Nullable
        Object render(
            @Nonnull EditorEx editor,
            @Nullable Side side,
            @Nonnull Function<DiffLineLocation, Integer> locationToLine,
            @Nonnull Function<Integer, DiffLineLocation> lineToLocation,
            @Nonnull Function<Integer, Pair<Integer, Integer>> lineToUnified,
            @Nonnull CoroutineScope scope,
            @Nonnull Continuation<? super kotlin.Nothing> $completion
        );
    }

    /**
     * Internal wrapper that maps a DiffMapped VM to an EditorMapped interface.
     */
    private static final class DiffMappedWrapper<VM extends DiffMapped> implements EditorMapped {
        final @Nonnull VM vm;
        private final @Nonnull Function<@Nullable DiffLineLocation, @Nullable Integer> mapper;

        DiffMappedWrapper(@Nonnull VM vm, @Nonnull Function<@Nullable DiffLineLocation, @Nullable Integer> mapper) {
            this.vm = vm;
            this.mapper = mapper;
        }

        @Override
        public @Nonnull Flow<@Nullable Integer> getLine() {
            return FlowKt.map(vm.getLocation(), loc -> mapper.apply(loc));
        }

        @Override
        public @Nonnull Flow<Boolean> getIsVisible() {
            return vm.getIsVisible();
        }
    }
}
