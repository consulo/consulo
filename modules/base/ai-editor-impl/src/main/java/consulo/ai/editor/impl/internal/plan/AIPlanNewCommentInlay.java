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
package consulo.ai.editor.impl.internal.plan;

import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.CodeReviewNewCommentInlayModel;
import consulo.review.editor.CodeReviewRangedInlayModel;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * The input of a new comment on lines of a plan, under the last of them.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AIPlanNewCommentInlay implements CodeReviewNewCommentInlayModel, CodeReviewRangedInlayModel {
    private final Object myKey = new Object();
    private final CodeReviewLineRange myRange;
    private final AIPlanNewCommentViewModel myEditor;
    private final Runnable myCancel;

    AIPlanNewCommentInlay(CodeReviewLineRange range, AIPlanNewCommentViewModel editor, Runnable cancel) {
        myRange = range;
        myEditor = editor;
        myCancel = cancel;
    }

    CodeReviewLineRange getLines() {
        return myRange;
    }

    @Override
    public Object getKey() {
        return myKey;
    }

    @Override
    public ObservableValue<@Nullable Integer> getLine() {
        return ObservableValue.of(myRange.end());
    }

    @Override
    public ObservableValue<Boolean> isVisible() {
        return ObservableValue.of(true);
    }

    @Override
    public ObservableValue<@Nullable CodeReviewLineRange> getRange() {
        return ObservableValue.of(myRange);
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
