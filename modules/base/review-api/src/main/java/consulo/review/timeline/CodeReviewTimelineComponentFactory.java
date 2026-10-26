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
package consulo.review.timeline;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.review.timeline.thread.CodeReviewFoldableThreadViewModel;
import consulo.review.timeline.thread.CodeReviewResolvableItemViewModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.review.util.ComputedResult;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The review timeline and the pieces it is made of - comments, threads and comment inputs - for providers composing
 * their own discussion UI.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeReviewTimelineComponentFactory {
    /**
     * Opens the timeline of a review in an editor tab, or selects the tab already showing it.
     */
    @RequiredUIAccess
    void openTimeline(Project project, CodeReviewDetailsTabViewModel tabVm, CodeReviewTimelineViewModel timelineVm);

    /**
     * Opens the timeline of a review in an editor tab, or selects the tab already showing it.
     * {@code owner} is disposed once the tab is closed, or at once when a tab already shows {@code timelineVm} - so view
     * models acquired for the tab live as long as it does.
     */
    @RequiredUIAccess
    void openTimeline(Project project, CodeReviewDetailsTabViewModel tabVm, CodeReviewTimelineViewModel timelineVm, Disposable owner);

    /**
     * Opens the timeline of a review in an editor tab at once, or selects the tab already showing it. While
     * {@code tabVm} loads, the tab is named {@code name} and shows the loading, a failure shows its reason, and the
     * timeline is shown once the tab view model is loaded.
     * {@code owner} is disposed once the tab is closed, or at once when a tab already shows {@code timelineVm}.
     */
    @RequiredUIAccess
    void openTimeline(Project project,
                      LocalizeValue name,
                      ObservableValue<ComputedResult<CodeReviewDetailsTabViewModel>> tabVm,
                      CodeReviewTimelineViewModel timelineVm,
                      Disposable owner);

    /**
     * Presents the editor tabs showing {@code timelineVm} again - their {@linkplain CodeReviewDetailsTabViewModel#getDisplayName() name}
     * and {@linkplain CodeReviewDetailsTabViewModel#getIcon() icon}.
     */
    @RequiredUIAccess
    void updateTimelinePresentation(Project project, CodeReviewTimelineViewModel timelineVm);

    /**
     * Closes the editor tabs showing {@code timelineVm}.
     */
    @RequiredUIAccess
    void closeTimeline(Project project, CodeReviewTimelineViewModel timelineVm);

    /**
     * The whole timeline: the review title, every comment and thread in time order, and the new comment input.
     */
    @RequiredUIAccess
    Component create(Disposable parentDisposable, CodeReviewDetailsTabViewModel tabVm, CodeReviewTimelineViewModel timelineVm);

    /**
     * The whole timeline once {@code tabVm} is loaded - the loading or the failure until then. For providers showing
     * the timeline in their own editor.
     *
     * @param timelineVm asked once {@code tabVm} is loaded
     */
    @RequiredUIAccess
    Component create(Disposable parentDisposable,
                     ObservableValue<ComputedResult<CodeReviewDetailsTabViewModel>> tabVm,
                     Supplier<CodeReviewTimelineViewModel> timelineVm);

    /**
     * A thread: while folded its first comment and the folded controls, once unfolded every reply and the reply input.
     *
     * @param compact small pictures and no header - the thread standing in the editor under its line; otherwise the
     *                location and the resolved tag are shown above the comments
     */
    @RequiredUIAccess
    Component createThread(Disposable parentDisposable, CodeReviewThreadViewModel thread, boolean compact);

    /**
     * An empty list of review comments, to be filled with {@link #addComment}.
     */
    @RequiredUIAccess
    MessageList createCommentList();

    @RequiredUIAccess
    MessageListItem addComment(Disposable parentDisposable, MessageList messages, CodeReviewCommentViewModel comment);

    /**
     * The input of a new comment. Sending is locked while {@code vm} submits, and a failed submit shows its reason.
     *
     * @param suffix shown next to the send button
     */
    @RequiredUIAccess
    Component createCommentInput(Disposable parentDisposable,
                                 CodeReviewNewCommentViewModel vm,
                                 LocalizeValue placeholder,
                                 LocalizeValue sendText,
                                 @Nullable Component suffix);

    /**
     * Who replied to a folded thread, the link unfolding the replies, when the last reply came and the resolve toggle.
     */
    @RequiredUIAccess
    Component createFoldedThreadControls(Disposable parentDisposable, CodeReviewFoldableThreadViewModel vm);

    /**
     * The link resolving or reopening a thread.
     */
    @RequiredUIAccess
    Component createResolveLink(Disposable parentDisposable, CodeReviewResolvableItemViewModel vm);

    /**
     * The commits added to a review: who added them and when, then a block of every commit - its hash opening it,
     * its title, author and date.
     */
    @RequiredUIAccess
    Component createCommits(Disposable parentDisposable, CodeReviewCommitsTimelineItem item);
}
