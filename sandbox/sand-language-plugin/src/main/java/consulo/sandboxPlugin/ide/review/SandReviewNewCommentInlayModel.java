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
package consulo.sandboxPlugin.ide.review;

import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.editor.CodeReviewAdjustableInlayModel;
import consulo.review.editor.CodeReviewInlayAdjustmentDisabledReason;
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.CodeReviewNewCommentInlayModel;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestionSyntax;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewNewCommentInlayModel implements CodeReviewNewCommentInlayModel, CodeReviewAdjustableInlayModel {
    private final Object myKey = "new-comment:" + UUID.randomUUID();
    private final MutableObservableValue<CodeReviewLineRange> myRange;
    private final MutableObservableValue<Integer> myLine;
    private final ObservableValue<@Nullable CodeReviewInlayAdjustmentDisabledReason> myAdjustmentDisabledReason;
    private final SandReviewNewCommentViewModel myEditor;
    private final Runnable myCancel;
    private final Runnable myRangeChanged;

    /**
     * @param range        the lines of the comment, changed here when its frame is dragged
     * @param rangeChanged told once the frame was dragged to other lines
     */
    public SandReviewNewCommentInlayModel(MutableObservableValue<CodeReviewLineRange> range,
                                          SandReviewNewCommentViewModel editor,
                                          CodeReviewSuggestionSyntax syntax,
                                          Runnable cancel,
                                          Runnable rangeChanged) {
        myRange = range;
        myLine = MutableObservableValue.of(range.get().end());
        myAdjustmentDisabledReason = editor.getText()
            .map(text -> syntax.containsSuggestedChange(text) ? CodeReviewInlayAdjustmentDisabledReason.SUGGESTED_CHANGE : null);
        myEditor = editor;
        myCancel = cancel;
        myRangeChanged = rangeChanged;
    }

    @Override
    public Object getKey() {
        return myKey;
    }

    @Override
    public ObservableValue<Integer> getLine() {
        return myLine;
    }

    @Override
    public ObservableValue<CodeReviewLineRange> getRange() {
        return myRange;
    }

    @Override
    public ObservableValue<@Nullable CodeReviewInlayAdjustmentDisabledReason> getAdjustmentDisabledReason() {
        return myAdjustmentDisabledReason;
    }

    @Override
    public void adjustRange(@Nullable Integer newStart, @Nullable Integer newEnd) {
        if (newStart == null && newEnd == null) {
            return;
        }
        CodeReviewLineRange current = myRange.get();
        int start = newStart == null ? current.start() : newStart;
        int end = newEnd == null ? current.end() : newEnd;
        if (start > end) {
            return;
        }
        myRange.set(new CodeReviewLineRange(start, end));
        myLine.set(end);
        myRangeChanged.run();
        myEditor.requestFocus();
    }

    @Override
    public ObservableValue<Boolean> isVisible() {
        return ObservableValue.of(true);
    }

    @Override
    public CodeReviewNewCommentViewModel getEditor() {
        return myEditor;
    }

    @Override
    public void cancel() {
        myCancel.run();
    }
}
