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
import consulo.codeEditor.markup.LineFrameEdge;
import consulo.codeEditor.markup.LineFramePresentation;
import consulo.codeEditor.markup.LineFrameResize;
import consulo.codeEditor.markup.LineFrameResizeListener;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.localize.LocalizeValue;
import consulo.review.editor.CodeReviewAdjustableInlayModel;
import consulo.review.editor.CodeReviewInlayAdjustmentDisabledReason;
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.ReviewInEditorUtil;
import consulo.review.impl.internal.CodeReviewColorUtil;
import consulo.review.impl.internal.ReviewUIUtil;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.IntPredicate;

final class ResizableOutlineHandler implements LineFrameResizeListener, DocumentListener, Disposable {
    private final Editor myEditor;
    private final CodeReviewAdjustableInlayModel myInlayModel;
    private final CodeReviewLineRange myCurrentRange;
    private final IntPredicate myCanCreateComment;
    private final Consumer<Boolean> myInlayVisibility;

    private @Nullable ResizableOutlineDragState myDragState;
    private @Nullable CodeReviewInlayAdjustmentDisabledReason myAdjustmentDisabledReason;
    private @Nullable RangeHighlighter myOutline;

    private ResizableOutlineHandler(Editor editor,
                                    CodeReviewAdjustableInlayModel inlayModel,
                                    CodeReviewLineRange currentRange,
                                    IntPredicate canCreateComment,
                                    Consumer<Boolean> inlayVisibility) {
        myEditor = editor;
        myInlayModel = inlayModel;
        myCurrentRange = currentRange;
        myCanCreateComment = canCreateComment;
        myInlayVisibility = inlayVisibility;

        editor.getDocument().addDocumentListener(this, this);
    }

    @RequiredUIAccess
    static void showResizableOutline(Disposable parentDisposable,
                                     Editor editor,
                                     CodeReviewAdjustableInlayModel inlayModel,
                                     Consumer<Boolean> inlayVisibility,
                                     CodeReviewLineRange initialRange,
                                     IntPredicate canCreateComment) {
        ResizableOutlineHandler handler = new ResizableOutlineHandler(editor, inlayModel, initialRange, canCreateComment, inlayVisibility);
        Disposer.register(parentDisposable, handler);

        ReviewUIUtil.bind(handler, inlayModel.getAdjustmentDisabledReason(), reason -> {
            handler.myAdjustmentDisabledReason = reason;
            handler.update();
        });
    }

    @Override
    public void dispose() {
        removeOutline();
    }

    @RequiredUIAccess
    @Override
    public void dragStarted(LineFrameEdge edge) {
        if (myInlayModel.getAdjustmentDisabledReason().get() != null) {
            return;
        }
        startDragging(edge);
    }

    @RequiredUIAccess
    @Override
    public void dragged(int line) {
        adjustDragRange(line);
    }

    @RequiredUIAccess
    @Override
    public void dragFinished() {
        finishDragging();
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        ResizableOutlineDragState currentState = myDragState;
        if (currentState == null) {
            return;
        }
        int lineCount = myEditor.getDocument().getLineCount();
        if ((currentState.edge() == LineFrameEdge.BOTTOM && currentState.line() >= lineCount) || myCurrentRange.end() >= lineCount) {
            myDragState = null;
            update();
        }
    }

    @RequiredUIAccess
    private void startDragging(LineFrameEdge edge) {
        myDragState = new ResizableOutlineDragState(edge, getLineAt(myCurrentRange, edge));
        update();
    }

    @RequiredUIAccess
    private boolean finishDragging() {
        ResizableOutlineDragState state = myDragState;
        if (state == null) {
            return false;
        }
        myDragState = null;
        update();
        if (getLineAt(myCurrentRange, state.edge()) != state.line()) {
            switch (state.edge()) {
                case TOP -> myInlayModel.adjustRange(state.line(), null);
                case BOTTOM -> myInlayModel.adjustRange(null, state.line());
            }
        }
        return true;
    }

    /**
     * Adjusts the ranges produced by dragging if dragging is in progress and {@code line} is a commentable line in the editor.
     */
    @RequiredUIAccess
    private void adjustDragRange(int line) {
        ResizableOutlineDragState currentState = myDragState;
        if (currentState == null) {
            return;
        }
        ResizableOutlineDragState newState = withLineUnderYIfCommentable(currentState, line);
        if (newState == null || newState.equals(currentState)) {
            return;
        }
        myDragState = newState;
        update();
    }

    /**
     * Adjusts the {@link ResizableOutlineDragState#line()} if {@code line} is a commentable line in the editor depending on the
     * {@link ResizableOutlineDragState#edge()}.
     *
     * @return null if the line is not commentable or the new range is invalid (start after end)
     */
    @RequiredUIAccess
    private @Nullable ResizableOutlineDragState withLineUnderYIfCommentable(ResizableOutlineDragState state, int line) {
        Document document = myEditor.getDocument();
        int lineUnderY = Math.clamp(line, 0, Math.max(document.getLineCount(), 1) - 1);
        boolean isCurrentBoundary = lineUnderY == state.line();
        if (ReviewInEditorUtil.isLastBlankLine(document, lineUnderY)) {
            return null;
        }
        if (!myCanCreateComment.test(lineUnderY) && !isCurrentBoundary) {
            return null;
        }

        return switch (state.edge()) {
            case TOP -> lineUnderY <= myCurrentRange.end() ? state.withLine(lineUnderY) : null;
            case BOTTOM -> lineUnderY >= myCurrentRange.start() ? state.withLine(lineUnderY) : null;
        };
    }

    @RequiredUIAccess
    private void update() {
        removeOutline();
        if (myEditor.isDisposed()) {
            return;
        }

        ResizableOutlineDragState dragState = myDragState;
        if (dragState == null) {
            if (myCurrentRange.end() > Math.max(myEditor.getDocument().getLineCount(), 1) - 1) {
                return;
            }
            myInlayVisibility.accept(true);
            myOutline = showOutline(myCurrentRange);
        }
        else {
            myInlayVisibility.accept(false);
            CodeReviewLineRange newRange = switch (dragState.edge()) {
                case TOP -> new CodeReviewLineRange(dragState.line(), myCurrentRange.end());
                case BOTTOM -> new CodeReviewLineRange(myCurrentRange.start(), dragState.line());
            };
            myOutline = showOutline(newRange);
        }
    }

    @RequiredUIAccess
    private RangeHighlighter showOutline(CodeReviewLineRange range) {
        CodeReviewInlayAdjustmentDisabledReason reason = myAdjustmentDisabledReason;
        LineFrameResize resize = new LineFrameResize(getTooltipMessage(reason), reason == null, this);
        return CodeReviewEditorInlayRangeOutlineUtils.addFrame(myEditor,
            range,
            new LineFramePresentation(CodeReviewColorUtil.REVIEW_LINE_FRAME_BORDER, resize));
    }

    private void removeOutline() {
        RangeHighlighter outline = myOutline;
        myOutline = null;
        if (outline != null) {
            outline.dispose();
        }
    }

    private static LocalizeValue getTooltipMessage(@Nullable CodeReviewInlayAdjustmentDisabledReason reason) {
        if (reason == null) {
            return ReviewLocalize.reviewCommentsCodeOutlineTooltipExplanation();
        }
        return switch (reason) {
            case SUGGESTED_CHANGE -> ReviewLocalize.reviewCommentsCodeOutlineTooltipSuggestionDisabling();
            case SINGLE_COMMIT_REVIEW -> ReviewLocalize.reviewCommentsCodeOutlineTooltipCommitReviewDisabling();
            case UNSUPPORTED_VERSION -> ReviewLocalize.reviewCommentsCodeOutlineTooltipVersionNotSupportedDisabling();
        };
    }

    private static int getLineAt(CodeReviewLineRange range, LineFrameEdge edge) {
        return switch (edge) {
            case TOP -> range.start();
            case BOTTOM -> range.end();
        };
    }
}
