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
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.comment.CodeReviewNewCommentViewModel;
import consulo.review.impl.internal.CodeReviewColorUtil;
import consulo.review.impl.internal.comment.CodeReviewCommentTextFieldFactory;
import consulo.review.impl.internal.comment.CodeReviewCommentUIUtil;
import consulo.review.impl.internal.list.ReviewListCellRenderer;
import consulo.review.localize.ReviewLocalize;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.review.timeline.CollapsibleTimelineItemViewModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.review.util.ReviewUIUtil;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListStyle;
import consulo.ui.style.ComponentColors;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A review thread: while folded only its first comment and the folded controls, once unfolded every reply and the
 * reply input. The same component is the thread of the timeline and, compact and without the header, of the editor
 * inlay - there a folded thread longer than three comments shows its first and its last comment around the link
 * unfolding the others, and the reply input opens on demand.
 * <p>
 * A {@linkplain CollapsibleTimelineItemViewModel collapsed} thread of the timeline shows a preview of its first comment
 * alone.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class CodeReviewThreadComponentFactory {
    private static final int COMPACT_FOLDED_LIMIT = 3;
    private static final int PREVIEW_LENGTH = 200;

    private CodeReviewThreadComponentFactory() {
    }

    @RequiredUIAccess
    public static Component create(Disposable parentDisposable, CodeReviewThreadViewModel thread, boolean compact) {
        VerticalLayout root = VerticalLayout.create(Space.NONE);

        CollapsibleTimelineItemViewModel collapsible = !compact && thread instanceof CollapsibleTimelineItemViewModel vm ? vm : null;
        if (!compact) {
            Component diff = thread.createDiffComponent(parentDisposable);
            root.add(createHeader(parentDisposable, thread, diff == null, diff == null ? collapsible : null));
            if (diff != null) {
                root.add(diff);
            }
        }

        Label preview = Label.create();
        preview.setForegroundColor(ComponentColors.DISABLED_TEXT);
        preview.setVisible(false);
        root.add(preview);

        MessageList messages = createList(thread, compact);
        root.add(messages);

        Hyperlink expander = Hyperlink.create(LocalizeValue.empty(), event -> thread.unfoldReplies());
        expander.setVisible(false);
        VerticalLayout expanderPanel = VerticalLayout.create(Space.NONE).add(expander);
        expanderPanel.paddingBuilder().verticalSet(Space.SMALL).leftSet(Space.X_LARGE).apply();
        expanderPanel.setVisible(false);
        root.add(expanderPanel);

        MessageList lastMessage = createList(thread, compact);
        lastMessage.setVisible(false);
        root.add(lastMessage);

        Component foldedControls = compact
            ? createReplyActions(parentDisposable, thread)
            : CodeReviewCommentUIUtil.createFoldedThreadControlsIn(parentDisposable, thread);
        root.add(foldedControls);

        CodeReviewNewCommentViewModel replyVm = thread.getReplyVm();
        Component replyInput = null;
        if (replyVm != null) {
            Component resolveLink = compact ? null : CodeReviewCommentUIUtil.createResolveLink(parentDisposable, thread);
            replyInput = CodeReviewCommentTextFieldFactory.create(parentDisposable,
                replyVm,
                ReviewLocalize.reviewCommentsReplyAction(),
                ReviewLocalize.reviewCommentsReplyAction(),
                resolveLink,
                CodeReviewCommentTextFieldFactory.HintKind.REPLY);
            root.add(replyInput);
        }

        // in the editor every part stands on the editor background the frame around them has - on the desktop a
        // layout paints a background of its own otherwise
        if (compact) {
            root.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
            foldedControls.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
            expanderPanel.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
            if (replyInput != null) {
                replyInput.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
            }
        }

        @Nullable ObservableValue<Boolean> writingReply = thread.isWritingReply();
        ObservableValue<Boolean> collapsed = !compact && thread instanceof CollapsibleTimelineItemViewModel collapsibleVm
            ? collapsibleVm.getCollapsed()
            : ObservableValue.of(false);

        Component reply = replyInput;
        Disposable[] itemsDisposable = {Disposable.newDisposable()};
        Disposer.register(parentDisposable, () -> Disposer.dispose(itemsDisposable[0]));
        Runnable refresh = () -> {
            Disposer.dispose(itemsDisposable[0]);
            itemsDisposable[0] = Disposable.newDisposable();

            List<CodeReviewCommentViewModel> comments = thread.getComments().get();
            boolean folded = thread.getRepliesFolded().get();
            boolean isCollapsed = collapsed.get();
            boolean writing = writingReply == null ? !folded : writingReply.get();

            messages.clear();
            lastMessage.clear();
            boolean split = compact && folded && comments.size() > COMPACT_FOLDED_LIMIT;
            if (isCollapsed) {
                preview.setText(LocalizeValue.of(comments.isEmpty() ? "" : previewOf(comments.get(0))));
            }
            else if (split) {
                CodeReviewCommentItemFactory.add(itemsDisposable[0], messages, comments.get(0));
                expander.setText(ReviewLocalize.reviewThreadMoreReplies(comments.size() - 2));
                CodeReviewCommentItemFactory.add(itemsDisposable[0], lastMessage, comments.get(comments.size() - 1));
            }
            else {
                List<CodeReviewCommentViewModel> shown = !compact && folded && !comments.isEmpty() ? comments.subList(0, 1) : comments;
                for (CodeReviewCommentViewModel comment : shown) {
                    CodeReviewCommentItemFactory.add(itemsDisposable[0], messages, comment);
                }
            }

            preview.setVisible(isCollapsed);
            messages.setVisible(!isCollapsed);
            expander.setVisible(split && !isCollapsed);
            expanderPanel.setVisible(split && !isCollapsed);
            lastMessage.setVisible(split && !isCollapsed);

            boolean canReply = thread.getCanCreateReplies().get();
            if (compact) {
                foldedControls.setVisible(!writing || reply == null);
            }
            else {
                foldedControls.setVisible(folded && !isCollapsed);
            }
            if (reply != null) {
                reply.setVisible(writing && canReply && !isCollapsed);
            }
        };
        ReviewUIUtil.bind(parentDisposable, thread.getComments(), thread.getRepliesFolded(), (comments, folded) -> refresh.run());
        ReviewUIUtil.bind(parentDisposable, collapsed, value -> refresh.run());
        if (writingReply != null) {
            ReviewUIUtil.bind(parentDisposable, writingReply, value -> refresh.run());
        }
        return root;
    }

    @RequiredUIAccess
    private static MessageList createList(CodeReviewThreadViewModel thread, boolean compact) {
        MessageList messages = CodeReviewCommentItemFactory.createList(thread.getHtmlImageLoader());
        if (compact) {
            messages.addStyle(MessageListStyle.COMPACT);
        }
        CodeReviewCommentItemFactory.installLinkHandler(messages, thread::openLink);
        return messages;
    }

    /**
     * The links of a compact thread which is not being replied to: start a reply, resolve.
     */
    @RequiredUIAccess
    private static Component createReplyActions(Disposable parentDisposable, CodeReviewThreadViewModel thread) {
        Hyperlink reply = Hyperlink.create(ReviewLocalize.reviewCommentsReplyAction(), event -> {
            thread.startWritingReply();
            CodeReviewNewCommentViewModel replyVm = thread.getReplyVm();
            if (replyVm != null) {
                replyVm.requestFocus();
            }
        });
        boolean hasReplyInput = thread.getReplyVm() != null;
        ReviewUIUtil.bind(parentDisposable, thread.getCanCreateReplies(), canReply -> reply.setVisible(hasReplyInput && canReply));

        HorizontalLayout actions = HorizontalLayout.create(Space.LARGE);
        actions.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        actions.add(reply);
        actions.add(CodeReviewCommentUIUtil.createResolveLink(parentDisposable, thread));
        actions.paddingBuilder().verticalSet(Space.SMALL).leftSet(Space.X_LARGE).apply();
        return actions;
    }

    private static String previewOf(CodeReviewCommentViewModel comment) {
        String body = comment.getBody().get().strip().replaceAll("\\s+", " ");
        return body.length() > PREVIEW_LENGTH ? body.substring(0, PREVIEW_LENGTH) + "\u2026" : body;
    }

    @RequiredUIAccess
    private static Component createHeader(Disposable parentDisposable,
                                          CodeReviewThreadViewModel thread,
                                          boolean showLocation,
                                          @Nullable CollapsibleTimelineItemViewModel collapsible) {
        HorizontalLayout header = HorizontalLayout.create(Space.LARGE);
        String location = showLocation ? thread.getLocation() : null;
        if (location != null) {
            if (thread.canNavigate()) {
                header.add(Hyperlink.create(LocalizeValue.of(location), event -> thread.navigate()));
            }
            else {
                Label locationLabel = Label.create(LocalizeValue.of(location));
                locationLabel.addStyle(LabelStyle.FONT_BOLD);
                header.add(locationLabel);
            }
        }

        header.add(createTag(parentDisposable, ReviewLocalize.reviewThreadOutdatedTag(), thread.isOutdated()));
        header.add(createTag(parentDisposable, ReviewLocalize.reviewThreadResolvedTag(), thread.isResolved()));
        header.add(createTag(parentDisposable, ReviewLocalize.reviewThreadPendingTag(), thread.isPending()));

        if (collapsible != null) {
            Button toggle = Button.create(LocalizeValue.empty());
            toggle.addStyle(ButtonStyle.INPLACE);
            toggle.addClickListener(event -> collapsible.setCollapsed(!collapsible.getCollapsed().get()));
            ReviewUIUtil.bind(parentDisposable, collapsible.getCollapsed(), value -> {
                toggle.setIcon(value ? PlatformIconGroup.generalExpandcomponent() : PlatformIconGroup.generalCollapsecomponent());
                toggle.setToolTipText(value ? ReviewLocalize.reviewThreadExpand() : ReviewLocalize.reviewThreadCollapse());
            });
            ReviewUIUtil.bind(parentDisposable, collapsible.getCollapsible(), toggle::setVisible);
            header.add(toggle);
        }
        return header;
    }

    @RequiredUIAccess
    private static Label createTag(Disposable parentDisposable, LocalizeValue text, ObservableValue<Boolean> visible) {
        Label tag = ReviewListCellRenderer.createTagLabel();
        tag.setText(text);
        ReviewUIUtil.bind(parentDisposable, visible, tag::setVisible);
        return tag;
    }
}
