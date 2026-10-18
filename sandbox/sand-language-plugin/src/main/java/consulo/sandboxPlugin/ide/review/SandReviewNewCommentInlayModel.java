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
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.CodeReviewNewCommentInlayModel;
import consulo.review.editor.CodeReviewRangedInlayModel;
import consulo.util.concurrent.coroutine.ObservableValue;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewNewCommentInlayModel implements CodeReviewNewCommentInlayModel, CodeReviewRangedInlayModel {
    private final int myLine;
    private final ObservableValue<Integer> myLineValue;
    private final ObservableValue<CodeReviewLineRange> myRange;
    private final SandReviewNewCommentViewModel myEditor;
    private final Runnable myCancel;

    public SandReviewNewCommentInlayModel(CodeReviewLineRange range, SandReviewNewCommentViewModel editor, Runnable cancel) {
        myLine = range.end();
        myLineValue = ObservableValue.of(range.end());
        myRange = ObservableValue.of(range);
        myEditor = editor;
        myCancel = cancel;
    }

    @Override
    public Object getKey() {
        return "new-comment:" + myLine;
    }

    @Override
    public ObservableValue<Integer> getLine() {
        return myLineValue;
    }

    @Override
    public ObservableValue<CodeReviewLineRange> getRange() {
        return myRange;
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
