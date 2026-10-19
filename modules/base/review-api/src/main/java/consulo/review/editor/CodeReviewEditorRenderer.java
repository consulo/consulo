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
package consulo.review.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiFunction;

/**
 * Shows a review inside an editor: the lines it changed, the gutter controls (the comment mark of every discussed line, the "+" of the
 * hovered or last selected line, the cancel mark of an open new comment), a block inlay under the line of every
 * discussion, and the outline of the lines a focused discussion is about. Each frontend draws them natively.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeReviewEditorRenderer {
    /**
     * The gutter controls and the inlays of {@code model}, until {@code parentDisposable} goes away. Threads and new
     * comments get the platform components; other inlay models are skipped.
     */
    @RequiredUIAccess
    default <I extends CodeReviewInlayModel> void setupIn(Disposable parentDisposable, Editor editor, CodeReviewEditorModel<I> model) {
        if (model instanceof CodeReviewEditorGutterChangesModel changesModel) {
            renderGutterChanges(parentDisposable, editor, changesModel);
        }
        renderGutterControls(parentDisposable, editor, model);
        renderInlays(parentDisposable,
            editor,
            model.getInlays(),
            model instanceof CodeReviewMultilineCommentableEditorModel multiline ? multiline : null,
            this::createInlayComponent);
    }

    /**
     * The lines the review changed, marked in the gutter where the version control marks its own changes.
     */
    @RequiredUIAccess
    void renderGutterChanges(Disposable parentDisposable, Editor editor, CodeReviewEditorGutterChangesModel model);

    /**
     * The gutter controls of {@code model}; they appear once its state is known and go away when it turns {@code null}.
     */
    @RequiredUIAccess
    void renderGutterControls(Disposable parentDisposable, Editor editor, CodeReviewEditorGutterControlsModel model);

    /**
     * A block inlay under the line of every visible inlay model, keyed by {@link CodeReviewInlayModel#getKey()} so an
     * inlay survives updates of the collection; a {@code null} component skips the model.
     *
     * @param editorModel what says which lines can be commented, so the frame of a
     *                    {@link CodeReviewAdjustableInlayModel} can be dragged over them; {@code null} keeps every frame
     *                    where it is
     */
    @RequiredUIAccess
    <I extends CodeReviewInlayModel> void renderInlays(Disposable parentDisposable,
                                                       Editor editor,
                                                       ObservableValue<? extends Collection<I>> inlays,
                                                       @Nullable CodeReviewMultilineCommentableEditorModel editorModel,
                                                       BiFunction<Disposable, I, @Nullable Component> componentFactory);

    /**
     * The platform component of a thread or new comment inlay model, or {@code null} for any other model.
     */
    @RequiredUIAccess
    default @Nullable Component createInlayComponent(Disposable parentDisposable, CodeReviewInlayModel inlay) {
        if (inlay instanceof CodeReviewThreadInlayModel thread) {
            return createThreadInlayComponent(parentDisposable, thread.getThread());
        }
        if (inlay instanceof CodeReviewNewCommentInlayModel newComment) {
            return createNewCommentInlayComponent(parentDisposable, newComment);
        }
        return null;
    }

    /**
     * A thread in the editor: its compact comments, the reply and resolve links, the reply input once unfolded - in a
     * rounded frame.
     */
    @RequiredUIAccess
    Component createThreadInlayComponent(Disposable parentDisposable, CodeReviewThreadViewModel thread);

    /**
     * The input of a new line comment with its cancel action, in a rounded frame.
     */
    @RequiredUIAccess
    Component createNewCommentInlayComponent(Disposable parentDisposable, CodeReviewNewCommentInlayModel inlay);
}
