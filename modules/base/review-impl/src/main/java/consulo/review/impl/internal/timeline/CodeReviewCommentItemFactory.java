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
import consulo.platform.Platform;
import consulo.review.comment.CodeReviewCommentBodyBlock;
import consulo.review.comment.CodeReviewComponentCommentBodyBlock;
import consulo.review.comment.CodeReviewMarkdownCommentBodyBlock;
import consulo.review.comment.CodeReviewTextEditingViewModel;
import consulo.review.util.ReviewUIUtil;
import consulo.review.impl.internal.comment.CodeReviewCommentTextFieldFactory;
import consulo.review.impl.internal.comment.CodeReviewCommentUIUtil;
import consulo.review.list.UserPresentation;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemPart;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

/**
 * Review comments are chat items of a {@link MessageList}: the author with the avatar, the time, and the markdown body
 * - or the blocks of the body, when the platform splits it.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class CodeReviewCommentItemFactory {
    private CodeReviewCommentItemFactory() {
    }

    @RequiredUIAccess
    public static MessageList createList() {
        return createList(null);
    }

    @RequiredUIAccess
    public static MessageList createList(@Nullable AsyncHtmlImageLoader imageLoader) {
        MessageList messages = MessageList.create();
        messages.setMarkdown(true);
        messages.setShowOwnAvatar(true);
        messages.setImageLoader(imageLoader);
        return messages;
    }

    /**
     * Opens a link of a comment with {@code linkOpener}, or outside when it does not take the link.
     */
    @RequiredUIAccess
    public static void installLinkHandler(MessageList messages, @RequiredUIAccess Predicate<String> linkOpener) {
        messages.addHyperlinkListener(event -> openLink(event.getDescription(), linkOpener));
    }

    @RequiredUIAccess
    public static void openLink(String href, @RequiredUIAccess Predicate<String> linkOpener) {
        if (linkOpener.test(href)) {
            return;
        }
        if (href.contains("://")) {
            Platform.current().openInBrowser(href);
        }
    }

    @RequiredUIAccess
    public static MessageListItem add(Disposable parentDisposable, MessageList messages, CodeReviewCommentViewModel comment) {
        UserPresentation author = comment.getAuthor();
        MessageListItem item = messages.add(LocalizeValue.of(author.getPresentableName()), comment.getBody().get());
        Date createdAt = comment.getCreatedAt();
        if (createdAt != null) {
            item.setTime(createdAt.toInstant());
        }
        item.setAuthorImage(author.getAvatarIcon());
        item.setStyle(comment.isOwn() ? MessageListItemStyle.OWN : MessageListItemStyle.OTHER);

        boolean[] editing = {false};
        Disposable[] bodyDisposable = {Disposable.newDisposable()};
        Disposer.register(parentDisposable, () -> Disposer.dispose(bodyDisposable[0]));

        ObservableValue<List<CodeReviewCommentBodyBlock>> blocks = comment.getBodyBlocks();
        Runnable showBody = () -> {
            Disposer.dispose(bodyDisposable[0]);
            bodyDisposable[0] = Disposable.newDisposable();
            List<CodeReviewCommentBodyBlock> value = blocks == null ? List.of() : blocks.get();
            if (value.isEmpty()) {
                item.setText(comment.getBody().get());
            }
            else {
                item.setParts(toParts(bodyDisposable[0], value));
            }
        };

        ReviewUIUtil.bind(parentDisposable, comment.getBody(), body -> {
            if (!editing[0]) {
                showBody.run();
            }
        });
        if (blocks != null) {
            ReviewUIUtil.bind(parentDisposable, blocks, value -> {
                if (!editing[0]) {
                    showBody.run();
                }
            });
        }

        ObservableValue<@Nullable CodeReviewTextEditingViewModel> editVm = comment.getEditVm();
        if (editVm != null) {
            ReviewUIUtil.bind(parentDisposable, editVm, vm -> {
                if (vm != null) {
                    editing[0] = true;
                    Disposer.dispose(bodyDisposable[0]);
                    bodyDisposable[0] = Disposable.newDisposable();
                    item.setParts(List.of(MessageListItemPart.component(CodeReviewCommentTextFieldFactory.createEdit(bodyDisposable[0], vm))));
                    vm.requestFocus();
                }
                else if (editing[0]) {
                    editing[0] = false;
                    showBody.run();
                }
            });
        }

        Component actions = createActions(comment);
        if (actions != null) {
            item.setActionsComponent(actions);
        }
        return item;
    }

    @RequiredUIAccess
    private static @Nullable Component createActions(CodeReviewCommentViewModel comment) {
        if (!comment.canEdit() && !comment.canDelete()) {
            return null;
        }
        HorizontalLayout actions = HorizontalLayout.create(Space.SMALL);
        if (comment.canEdit()) {
            actions.add(CodeReviewCommentUIUtil.createEditButton(comment::editBody));
        }
        if (comment.canDelete()) {
            actions.add(CodeReviewCommentUIUtil.createDeleteCommentIconButton(comment::delete));
        }
        return actions;
    }

    @RequiredUIAccess
    private static List<MessageListItemPart> toParts(Disposable parentDisposable, List<CodeReviewCommentBodyBlock> blocks) {
        List<MessageListItemPart> parts = new ArrayList<>(blocks.size());
        for (CodeReviewCommentBodyBlock block : blocks) {
            if (block instanceof CodeReviewMarkdownCommentBodyBlock markdown) {
                parts.add(MessageListItemPart.text(markdown.getMarkdown()));
            }
            else if (block instanceof CodeReviewComponentCommentBodyBlock component) {
                parts.add(MessageListItemPart.component(component.createComponent(parentDisposable)));
            }
        }
        return parts;
    }
}
