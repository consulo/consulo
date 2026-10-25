// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.diff.FrameDiffTool;
import consulo.disposer.Disposable;
import consulo.review.editor.CodeReviewEditorModel;
import consulo.review.editor.CodeReviewInlayModel;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeReviewDiffViewerRenderer {
    /**
     * Create editor models for diff editors via {@code modelFactory} and show inlays and gutter controls
     *
     * @param <M> editor inlays and controls model
     * @param <I> inlay model
     */
    @RequiredUIAccess
    default <I extends CodeReviewInlayModel, M extends CodeReviewEditorModel<I>> void showCodeReview(Disposable parentDisposable,
                                                                                                     FrameDiffTool.DiffViewer viewer,
                                                                                                     EditorModelFactory<M> modelFactory) {
        renderCodeReview(parentDisposable, viewer, (scope, editor, side, mapper) -> {
            M model = modelFactory.create(scope, editor, side, mapper);
            showCodeReview(scope, editor, model);
        });
    }

    /**
     * Subscribe to {@code vms} and show inlays with components from {@code rendererFactory} on proper lines in viewer editors
     *
     * @param <VM> inlay view model
     */
    @RequiredUIAccess
    <VM extends DiffMapped> void controlInlaysIn(Disposable parentDisposable,
                                                 FrameDiffTool.DiffViewer viewer,
                                                 ObservableValue<? extends Collection<VM>> vms,
                                                 Function<VM, Object> vmKeyExtractor,
                                                 BiFunction<Disposable, VM, @Nullable Component> rendererFactory);

    /**
     * Runs {@code editorRenderer} for every editor of the viewer once its diff is computed, again after every rediff,
     * until {@code parentDisposable} goes away. Viewers without text editors are skipped.
     */
    @RequiredUIAccess
    void renderCodeReview(Disposable parentDisposable, FrameDiffTool.DiffViewer viewer, EditorCodeReviewRenderer editorRenderer);

    /**
     * The gutter controls and the inlays of {@code model} in {@code editor}, and the model in the editor data, until
     * {@code parentDisposable} goes away.
     */
    @RequiredUIAccess
    <I extends CodeReviewInlayModel> void showCodeReview(Disposable parentDisposable, Editor editor, CodeReviewEditorModel<I> model);
}
