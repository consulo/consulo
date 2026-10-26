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
package consulo.review.timeline.thread;

import consulo.disposer.Disposable;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.review.timeline.CodeReviewTimelineItem;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A discussion thread: a first comment, its replies and whether it is resolved.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface CodeReviewThreadViewModel extends CodeReviewTimelineItem,
    CodeReviewResolvableItemViewModel,
    CodeReviewFoldableThreadViewModel,
    CodeReviewTrackableItemViewModel {
    ObservableValue<List<CodeReviewCommentViewModel>> getComments();

    /**
     * Where the thread is anchored, such as {@code Main.java:42}, or {@code null} for a thread on the whole review.
     */
    @Nullable
    default String getLocation() {
        return null;
    }

    /**
     * Whether {@link #navigate()} can show where the thread is anchored.
     */
    default boolean canNavigate() {
        return false;
    }

    /**
     * Shows where the thread is anchored - its file opened at its line, with the thread unfolded there.
     */
    @RequiredUIAccess
    default void navigate() {
    }

    /**
     * Writes a reply, or {@code null} when the user cannot reply.
     */
    @Nullable
    default CodeReviewNewCommentViewModel getReplyVm() {
        return null;
    }

    /**
     * The code the thread is about, shown in the timeline above its comments in place of the {@link #getLocation()
     * location}, or {@code null} for none.
     *
     * @param parentDisposable disposed when the timeline stops showing the thread
     */
    @RequiredUIAccess
    default @Nullable Component createDiffComponent(Disposable parentDisposable) {
        return null;
    }

    /**
     * Loads the images of the comment bodies, or {@code null} to read them from their addresses as they are.
     */
    default @Nullable AsyncHtmlImageLoader getHtmlImageLoader() {
        return null;
    }

    /**
     * Whether the code the thread is about changed since it was written, shown as a tag of the thread.
     */
    default ObservableValue<Boolean> isOutdated() {
        return ObservableValue.of(false);
    }

    /**
     * Whether the thread is a part of a review not submitted yet, shown as a tag of the thread.
     */
    default ObservableValue<Boolean> isPending() {
        return ObservableValue.of(false);
    }

    /**
     * Whether the reply input of the compact thread is open - until then the thread offers to start a reply. {@code null}
     * shows the reply input together with the unfolded replies.
     */
    default @Nullable ObservableValue<Boolean> isWritingReply() {
        return null;
    }

    /**
     * Opens the reply input of the compact thread, see {@link #isWritingReply()}.
     */
    default void startWritingReply() {
        unfoldReplies();
    }

    /**
     * Opens a link of a comment inside the IDE, such as a reference to another review.
     *
     * @return {@code false} to open the link outside
     */
    @RequiredUIAccess
    default boolean openLink(String href) {
        return false;
    }
}
