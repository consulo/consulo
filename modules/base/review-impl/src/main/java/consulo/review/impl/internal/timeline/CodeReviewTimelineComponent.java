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

import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.review.util.ReviewUIUtil;
import consulo.review.impl.internal.comment.CodeReviewCommentTextFieldFactory;
import consulo.review.impl.internal.details.CodeReviewDetailsTitleComponentFactory;
import consulo.review.impl.internal.list.ReviewListComponentFactoryImpl;
import consulo.review.localize.ReviewLocalize;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.review.timeline.CodeReviewCommitsTimelineItem;
import consulo.review.timeline.CodeReviewTimelineItem;
import consulo.review.timeline.CodeReviewTimelineViewModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.image.Image;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
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
                fillItems(itemsDisposable[0], items, loaded, timelineVm);
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

        content.add(createProgressAndError(parentDisposable, timelineVm));

        CodeReviewNewCommentViewModel newCommentVm = timelineVm.getNewCommentVm();
        if (newCommentVm != null) {
            Component input = CodeReviewCommentTextFieldFactory.create(parentDisposable,
                newCommentVm,
                ReviewLocalize.reviewCommentPlaceholder(),
                ReviewLocalize.reviewCommentSubmit(),
                null,
                CodeReviewCommentTextFieldFactory.HintKind.COMMENT);
            VerticalLayout inputPanel = VerticalLayout.create(Space.NONE).add(input);
            inputPanel.paddingBuilder().topSet(Space.LARGE).horizontalSet(Space.X_LARGE).apply();
            content.add(inputPanel);
        }

        ScrollableLayout scroll = ReviewListComponentFactoryImpl.wrapWithLazyVerticalScroll(content, timelineVm::requestMore);
        scroll.putUserData(UiDataProvider.KEY, tabVm::uiDataSnapshot);
        ActionGroup popupGroup = tabVm.getPopupActionGroup();
        if (popupGroup != null) {
            scroll.addContextMenuListener(event -> {
                ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.POPUP, popupGroup);
                menu.setTargetComponent(scroll);
                menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
            });
        }
        return scroll;
    }

    /**
     * The loading of the next part of the timeline, or why it failed.
     */
    @RequiredUIAccess
    private static Component createProgressAndError(Disposable parentDisposable, CodeReviewTimelineViewModel timelineVm) {
        Label loading = Label.create(ReviewLocalize.reviewTimelineLoading());
        loading.setImage(Image.busy());
        loading.setForegroundColor(ComponentColors.DISABLED_TEXT);
        loading.setVisible(false);
        ReviewUIUtil.bind(parentDisposable, timelineVm.isLoading(), loading::setVisible);

        Label error = Label.create();
        error.setForegroundColor(StandardColors.RED);
        error.setVisible(false);
        ReviewUIUtil.bind(parentDisposable, timelineVm.getLoadingError(), loadingError -> {
            if (loadingError == null) {
                error.setVisible(false);
                return;
            }
            String message = loadingError.getLocalizedMessage();
            LocalizeValue title = ReviewLocalize.reviewTimelineCannotLoad();
            error.setText(message == null || message.isBlank() ? title : LocalizeValue.join(title, LocalizeValue.of(": " + message)));
            error.setVisible(true);
        });

        VerticalLayout panel = VerticalLayout.create(Space.SMALL, HorizontalAlignment.CENTER);
        panel.add(loading);
        panel.add(error);
        panel.paddingBuilder().verticalSet(Space.MEDIUM).horizontalSet(Space.X_LARGE).apply();
        return panel;
    }

    @RequiredUIAccess
    private static void fillItems(Disposable parentDisposable,
                                  VerticalLayout items,
                                  List<CodeReviewTimelineItem> loaded,
                                  CodeReviewTimelineViewModel timelineVm) {
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
                run = appendTo(run, items, timelineVm);
                CodeReviewCommentItemFactory.add(parentDisposable, run, comment);
            }
            else if (item instanceof CodeReviewCommitsTimelineItem commits) {
                run = null;
                items.add(CodeReviewCommitsItemFactory.create(parentDisposable, commits));
            }
        }
    }

    @RequiredUIAccess
    private static MessageList appendTo(@Nullable MessageList run, VerticalLayout items, CodeReviewTimelineViewModel timelineVm) {
        if (run != null) {
            return run;
        }
        MessageList messages = CodeReviewCommentItemFactory.createList(timelineVm.getHtmlImageLoader());
        CodeReviewCommentItemFactory.installLinkHandler(messages, timelineVm::openLink);
        items.add(messages);
        return messages;
    }
}
