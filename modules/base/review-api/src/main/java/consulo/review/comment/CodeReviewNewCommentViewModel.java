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
package consulo.review.comment;

import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.List;

/**
 * A comment input that sends its {@link #getText() text} somewhere - a new timeline comment, a thread reply or a new
 * line comment.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface CodeReviewNewCommentViewModel extends CodeReviewSubmittableTextViewModel {
    void submit();

    /**
     * The named ways to send the comment: the first one is the send action of the input, the others are offered next to
     * its suffix. Empty sends with {@link #submit()} under the default name.
     */
    default ObservableValue<List<CodeReviewCommentSubmitAction>> getCommentSubmitActions() {
        return ObservableValue.of(List.of());
    }

    /**
     * Whether the comment is about lines a change can be suggested for - the input then offers to insert a suggestion.
     */
    default boolean canSuggestChange() {
        return false;
    }

    /**
     * A suggested change of the lines the comment is about, holding the lines as they are, written the way the
     * platform writes suggestions.
     */
    default String createSuggestedChange() {
        return "";
    }
}
