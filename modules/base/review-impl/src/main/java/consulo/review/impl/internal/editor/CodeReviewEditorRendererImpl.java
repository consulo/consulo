/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.impl.internal.editor;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.review.editor.CodeReviewEditorGutterChangesModel;
import consulo.review.editor.CodeReviewEditorGutterControlsModel;
import consulo.review.editor.CodeReviewEditorRenderer;
import consulo.review.editor.CodeReviewInlayModel;
import consulo.review.editor.CodeReviewNewCommentInlayModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@Singleton
@ServiceImpl
public class CodeReviewEditorRendererImpl implements CodeReviewEditorRenderer {
    @RequiredUIAccess
    @Override
    public void renderGutterChanges(Disposable parentDisposable, Editor editor, CodeReviewEditorGutterChangesModel model) {
        CodeReviewEditorGutterChangesRenderer.render(parentDisposable, editor, model);
    }

    @RequiredUIAccess
    @Override
    public void renderGutterControls(Disposable parentDisposable, Editor editor, CodeReviewEditorGutterControlsModel model) {
        CodeReviewEditorGutterControlsRenderer.setupIn(parentDisposable, editor, model);
    }

    @RequiredUIAccess
    @Override
    public <I extends CodeReviewInlayModel> void renderInlays(Disposable parentDisposable,
                                                              Editor editor,
                                                              ObservableValue<? extends Collection<I>> inlays,
                                                              BiFunction<Disposable, I, @Nullable Component> componentFactory) {
        CodeReviewEditorInlaysRenderer.renderInlays(parentDisposable, editor, inlays, componentFactory);
    }

    @RequiredUIAccess
    @Override
    public Component createThreadInlayComponent(Disposable parentDisposable, CodeReviewThreadViewModel thread) {
        return CodeReviewEditorComponentsFactory.createThreadIn(parentDisposable, thread);
    }

    @RequiredUIAccess
    @Override
    public Component createNewCommentInlayComponent(Disposable parentDisposable, CodeReviewNewCommentInlayModel inlay) {
        return CodeReviewEditorComponentsFactory.createNewCommentIn(parentDisposable, inlay);
    }
}
