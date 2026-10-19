// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.LineFramePresentation;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.review.editor.CodeReviewAdjustableInlayModel;
import consulo.review.editor.CodeReviewInlayModel;
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.CodeReviewMultilineCommentableEditorModel;
import consulo.review.editor.CodeReviewNewCommentInlayModel;
import consulo.review.editor.CodeReviewRangedInlayModel;
import consulo.review.editor.CodeReviewThreadInlayModel;
import consulo.review.impl.internal.CodeReviewColorUtil;
import consulo.review.impl.internal.ReviewUIUtil;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * The frame around the lines a ranged inlay is about.
 * <p>
 * An adjustable inlay is always framed, and the edges of its frame can be dragged over the lines the editor model can
 * comment; otherwise a new comment is always framed and a discussion is framed while its replies are unfolded.
 * The frame is a {@link LineFramePresentation} every frontend draws.
 */
public final class CodeReviewEditorInlayRangeOutlineUtils {
    private CodeReviewEditorInlayRangeOutlineUtils() {
    }

    @RequiredUIAccess
    public static void showOutlineIn(Disposable parentDisposable,
                                     Editor editor,
                                     CodeReviewInlayModel vm,
                                     @Nullable CodeReviewMultilineCommentableEditorModel editorModel,
                                     Consumer<Boolean> inlayVisibility) {
        if (!(vm instanceof CodeReviewRangedInlayModel ranged)) {
            return;
        }

        if (vm instanceof CodeReviewAdjustableInlayModel adjustable && editorModel != null) {
            AtomicReference<@Nullable Disposable> handler = new AtomicReference<>();
            Disposer.register(parentDisposable, () -> disposeHandler(handler));
            ReviewUIUtil.bind(parentDisposable, adjustable.getRange(), range -> {
                disposeHandler(handler);
                if (range != null && !editor.isDisposed()) {
                    Disposable handlerDisposable = Disposable.newDisposable("Resizable outline of " + vm.getKey());
                    handler.set(handlerDisposable);
                    ResizableOutlineHandler.showResizableOutline(handlerDisposable,
                        editor,
                        adjustable,
                        inlayVisibility,
                        range,
                        editorModel::canCreateComment);
                }
            });
            return;
        }

        AtomicReference<@Nullable RangeHighlighter> frame = new AtomicReference<>();
        Disposer.register(parentDisposable, () -> removeFrame(frame));

        if (vm instanceof CodeReviewThreadInlayModel thread) {
            ReviewUIUtil.bind(parentDisposable, ranged.getRange(), thread.getThread().getRepliesFolded(),
                (range, folded) -> updateFrame(editor, frame, folded ? null : range));
        }
        else {
            boolean newComment = vm instanceof CodeReviewNewCommentInlayModel;
            ReviewUIUtil.bind(parentDisposable, ranged.getRange(), range -> updateFrame(editor, frame, newComment ? range : null));
        }
    }

    @RequiredUIAccess
    private static void updateFrame(Editor editor, AtomicReference<@Nullable RangeHighlighter> frame, @Nullable CodeReviewLineRange range) {
        removeFrame(frame);
        if (range != null && !editor.isDisposed()) {
            frame.set(addFrame(editor, range, new LineFramePresentation(CodeReviewColorUtil.REVIEW_LINE_FRAME_BORDER)));
        }
    }

    private static void disposeHandler(AtomicReference<@Nullable Disposable> handler) {
        Disposable disposable = handler.getAndSet(null);
        if (disposable != null) {
            Disposer.dispose(disposable);
        }
    }

    @RequiredUIAccess
    static RangeHighlighter addFrame(Editor editor, CodeReviewLineRange range, LineFramePresentation presentation) {
        Document document = editor.getDocument();
        int lastLine = Math.max(0, document.getLineCount() - 1);
        int startOffset = document.getLineStartOffset(Math.min(range.start(), lastLine));
        int endOffset = document.getLineStartOffset(Math.min(range.end(), lastLine));
        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(startOffset,
            endOffset,
            HighlighterLayer.LAST,
            null,
            HighlighterTargetArea.LINES_IN_RANGE);
        highlighter.setLineFramePresentation(presentation);
        return highlighter;
    }

    private static void removeFrame(AtomicReference<@Nullable RangeHighlighter> frame) {
        RangeHighlighter highlighter = frame.getAndSet(null);
        if (highlighter != null) {
            highlighter.dispose();
        }
    }
}
