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

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.timeline.CodeReviewTimelineItem;
import consulo.review.timeline.CodeReviewTimelineViewModel;
import consulo.review.util.ComputedResult;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The description comment first, then the threads, then the comments posted since - loaded with a delay so the
 * loading state shows.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewTimelineViewModel implements CodeReviewTimelineViewModel {
    private final MutableObservableValue<ComputedResult<List<CodeReviewTimelineItem>>> myItems =
        MutableObservableValue.of(ComputedResult.loading());
    private final List<CodeReviewTimelineItem> myLoaded = new ArrayList<>();
    private final SandReviewNewCommentViewModel myNewCommentVm;

    public SandReviewTimelineViewModel(Project project,
                                       Disposable parentDisposable,
                                       ScheduledExecutorService scheduler,
                                       List<CodeReviewTimelineItem> items) {
        myLoaded.addAll(items);
        myNewCommentVm = new SandReviewNewCommentViewModel(project, parentDisposable, scheduler, this::addComment);
        scheduler.schedule(this::publish, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public ObservableValue<ComputedResult<List<CodeReviewTimelineItem>>> getItems() {
        return myItems;
    }

    @Override
    public CodeReviewNewCommentViewModel getNewCommentVm() {
        return myNewCommentVm;
    }

    public synchronized void addItem(CodeReviewTimelineItem item) {
        myLoaded.add(item);
        publish();
    }

    private void addComment(SandReviewComment comment) {
        addItem(comment);
    }

    private synchronized void publish() {
        myItems.set(ComputedResult.success(List.copyOf(myLoaded)));
    }

    @Override
    public AsyncHtmlImageLoader getHtmlImageLoader() {
        return SandReviewImages.HTML_IMAGES;
    }
}
