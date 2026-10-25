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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.review.util.ReviewUIUtil;
import consulo.review.impl.internal.comment.CodeReviewCommentTextFieldFactory;
import consulo.review.impl.internal.details.CodeReviewDetailsTitleComponentFactory;
import consulo.review.localize.ReviewLocalize;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.review.timeline.CodeReviewCommitsTimelineItem;
import consulo.review.timeline.CodeReviewTimelineItem;
import consulo.review.timeline.CodeReviewTimelineViewModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import consulo.ui.messageList.MessageList;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The timeline of a review in an editor tab: its title, every comment and thread in time order, and the input of a new
 * comment.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class CodeReviewTimelineComponent {
    private CodeReviewTimelineComponent() {
    }

    @RequiredUIAccess
    public static Component create(Disposable parentDisposable, CodeReviewDetailsTabViewModel tabVm, CodeReviewTimelineViewModel timelineVm) {
        VerticalLayout content = VerticalLayout.create(Space.NONE);
        content.paddingBuilder().verticalSet(Space.MEDIUM).apply();

        Component title = CodeReviewDetailsTitleComponentFactory.create(parentDisposable, tabVm.getDetailsVm());
        VerticalLayout titlePanel = VerticalLayout.create(Space.NONE).add(title);
        titlePanel.paddingBuilder().verticalSet(Space.X_LARGE).horizontalSet(Space.X_LARGE).apply();
        content.add(titlePanel);

        VerticalLayout items = VerticalLayout.create(Space.LARGE);
        items.paddingBuilder().horizontalSet(Space.X_LARGE).apply();
        content.add(items);

        Disposable[] itemsDisposable = {Disposable.newDisposable()};
        Disposer.register(parentDisposable, () -> Disposer.dispose(itemsDisposable[0]));
        ReviewUIUtil.bind(parentDisposable, timelineVm.getItems(), result -> {
            Disposer.dispose(itemsDisposable[0]);
            itemsDisposable[0] = Disposable.newDisposable();
            items.removeAll();

            List<CodeReviewTimelineItem> loaded = result.getOrNull();
            Throwable error = result.exceptionOrNull();
            if (loaded != null) {
                fillItems(itemsDisposable[0], items, loaded, timelineVm.getHtmlImageLoader());
            }
            else if (error != null) {
                Label errorLabel = Label.create(LocalizeValue.of(String.valueOf(error.getLocalizedMessage())));
                errorLabel.setForegroundColor(StandardColors.RED);
                items.add(errorLabel);
            }
            else {
                Label loading = Label.create(ReviewLocalize.reviewTimelineLoading());
                loading.setForegroundColor(ComponentColors.DISABLED_TEXT);
                items.add(loading);
            }
        });

        CodeReviewNewCommentViewModel newCommentVm = timelineVm.getNewCommentVm();
        if (newCommentVm != null) {
            Component input = CodeReviewCommentTextFieldFactory.create(parentDisposable,
                newCommentVm,
                ReviewLocalize.reviewCommentPlaceholder(),
                ReviewLocalize.reviewCommentSubmit(),
                null);
            VerticalLayout inputPanel = VerticalLayout.create(Space.NONE).add(input);
            inputPanel.paddingBuilder().topSet(Space.LARGE).horizontalSet(Space.X_LARGE).apply();
            content.add(inputPanel);
        }

        return ScrollableLayout.create(content);
    }

    @RequiredUIAccess
    private static void fillItems(Disposable parentDisposable,
                                  VerticalLayout items,
                                  List<CodeReviewTimelineItem> loaded,
                                  @Nullable AsyncHtmlImageLoader imageLoader) {
        if (loaded.isEmpty()) {
            Label empty = Label.create(ReviewLocalize.reviewTimelineNoDescription());
            empty.setForegroundColor(ComponentColors.DISABLED_TEXT);
            items.add(empty);
            return;
        }

        MessageList run = null;
        for (CodeReviewTimelineItem item : loaded) {
            if (item instanceof CodeReviewThreadViewModel thread) {
                run = null;
                items.add(CodeReviewThreadComponentFactory.create(parentDisposable, thread, false));
            }
            else if (item instanceof CodeReviewCommentViewModel comment) {
                run = appendTo(run, items, imageLoader);
                CodeReviewCommentItemFactory.add(parentDisposable, run, comment);
            }
            else if (item instanceof CodeReviewCommitsTimelineItem commits) {
                run = null;
                items.add(CodeReviewCommitsItemFactory.create(parentDisposable, commits));
            }
        }
    }

    @RequiredUIAccess
    private static MessageList appendTo(@Nullable MessageList run, VerticalLayout items, @Nullable AsyncHtmlImageLoader imageLoader) {
        if (run != null) {
            return run;
        }
        MessageList messages = CodeReviewCommentItemFactory.createList(imageLoader);
        items.add(messages);
        return messages;
    }
}
