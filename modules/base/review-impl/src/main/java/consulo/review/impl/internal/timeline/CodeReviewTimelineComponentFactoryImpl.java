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
package consulo.review.impl.internal.timeline;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.review.impl.internal.comment.CodeReviewCommentTextFieldFactory;
import consulo.review.impl.internal.comment.CodeReviewCommentUIUtil;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.review.timeline.CodeReviewCommitsTimelineItem;
import consulo.review.timeline.CodeReviewTimelineComponentFactory;
import consulo.review.timeline.CodeReviewTimelineViewModel;
import consulo.review.timeline.thread.CodeReviewFoldableThreadViewModel;
import consulo.review.timeline.thread.CodeReviewResolvableItemViewModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@Singleton
@ServiceImpl
public class CodeReviewTimelineComponentFactoryImpl implements CodeReviewTimelineComponentFactory {
    @RequiredUIAccess
    @Override
    public void openTimeline(Project project, CodeReviewDetailsTabViewModel tabVm, CodeReviewTimelineViewModel timelineVm) {
        CodeReviewTimelineVirtualFile.open(project, tabVm, timelineVm, null);
    }

    @RequiredUIAccess
    @Override
    public void openTimeline(Project project, CodeReviewDetailsTabViewModel tabVm, CodeReviewTimelineViewModel timelineVm, Disposable owner) {
        CodeReviewTimelineVirtualFile.open(project, tabVm, timelineVm, owner);
    }

    @RequiredUIAccess
    @Override
    public void closeTimeline(Project project, CodeReviewTimelineViewModel timelineVm) {
        CodeReviewTimelineVirtualFile.close(project, timelineVm);
    }

    @RequiredUIAccess
    @Override
    public Component create(Disposable parentDisposable, CodeReviewDetailsTabViewModel tabVm, CodeReviewTimelineViewModel timelineVm) {
        return CodeReviewTimelineComponent.create(parentDisposable, tabVm, timelineVm);
    }

    @RequiredUIAccess
    @Override
    public Component createThread(Disposable parentDisposable, CodeReviewThreadViewModel thread, boolean compact) {
        return CodeReviewThreadComponentFactory.create(parentDisposable, thread, compact);
    }

    @RequiredUIAccess
    @Override
    public MessageList createCommentList() {
        return CodeReviewCommentItemFactory.createList();
    }

    @RequiredUIAccess
    @Override
    public MessageListItem addComment(Disposable parentDisposable, MessageList messages, CodeReviewCommentViewModel comment) {
        return CodeReviewCommentItemFactory.add(parentDisposable, messages, comment);
    }

    @RequiredUIAccess
    @Override
    public Component createCommentInput(Disposable parentDisposable,
                                        CodeReviewNewCommentViewModel vm,
                                        LocalizeValue placeholder,
                                        LocalizeValue sendText,
                                        @Nullable Component suffix) {
        return CodeReviewCommentTextFieldFactory.create(parentDisposable, vm, placeholder, sendText, suffix);
    }

    @RequiredUIAccess
    @Override
    public Component createFoldedThreadControls(Disposable parentDisposable, CodeReviewFoldableThreadViewModel vm) {
        return CodeReviewCommentUIUtil.createFoldedThreadControlsIn(parentDisposable, vm);
    }

    @RequiredUIAccess
    @Override
    public Component createResolveLink(Disposable parentDisposable, CodeReviewResolvableItemViewModel vm) {
        return CodeReviewCommentUIUtil.createResolveLink(parentDisposable, vm);
    }

    @RequiredUIAccess
    @Override
    public Component createCommits(Disposable parentDisposable, CodeReviewCommitsTimelineItem item) {
        return CodeReviewCommitsItemFactory.create(parentDisposable, item);
    }
}
