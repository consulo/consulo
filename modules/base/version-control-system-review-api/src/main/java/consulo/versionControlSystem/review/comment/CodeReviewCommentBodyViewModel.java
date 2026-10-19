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
package consulo.versionControlSystem.review.comment;

import consulo.review.comment.CodeReviewCommentBodyBlock;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.patch.TextFilePatch;

import java.util.List;

/**
 * The body of a review comment split into markdown and suggested changes, with what can be done to a suggestion.
 * Built by {@link CodeReviewCommentBodyViewModelFactory}; a comment hands {@link #getBlocks()} out as its
 * {@link consulo.review.timeline.CodeReviewCommentViewModel#getBodyBlocks() body blocks}.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public interface CodeReviewCommentBodyViewModel {
    ObservableValue<List<CodeReviewCommentBodyBlock>> getBlocks();

    CodeReviewSuggestedChangeSupport getSupport();

    ObservableValue<Boolean> isOnReviewBranch();

    @RequiredUIAccess
    void applySuggestionLocally(TextFilePatch patch);

    @RequiredUIAccess
    void commitSuggestion(TextFilePatch patch, String commitMessage);
}
