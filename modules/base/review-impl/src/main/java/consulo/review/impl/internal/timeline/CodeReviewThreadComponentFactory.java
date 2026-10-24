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
import consulo.review.impl.internal.CodeReviewColorUtil;
import consulo.review.util.ReviewUIUtil;
import consulo.review.impl.internal.comment.CodeReviewCommentTextFieldFactory;
import consulo.review.impl.internal.comment.CodeReviewCommentUIUtil;
import consulo.review.impl.internal.list.ReviewListCellRenderer;
import consulo.review.localize.ReviewLocalize;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListStyle;

import java.util.List;

/**
 * A review thread: while folded only its first comment and the folded controls, once unfolded every reply and the
 * reply input. The same component is the thread of the timeline and, compact and without the header, of the editor
 * inlay.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class CodeReviewThreadComponentFactory {
    private CodeReviewThreadComponentFactory() {
    }

    @RequiredUIAccess
    public static Component create(Disposable parentDisposable, CodeReviewThreadViewModel thread, boolean compact) {
        VerticalLayout root = VerticalLayout.create(Space.NONE);

        if (!compact) {
            Component diff = thread.createDiffComponent(parentDisposable);
            root.add(createHeader(parentDisposable, thread, diff == null));
            if (diff != null) {
                root.add(diff);
            }
        }

        MessageList messages = CodeReviewCommentItemFactory.createList();
        if (compact) {
            messages.addStyle(MessageListStyle.COMPACT);
        }
        root.add(messages);

        Component foldedControls = CodeReviewCommentUIUtil.createFoldedThreadControlsIn(parentDisposable, thread);
        root.add(foldedControls);

        CodeReviewNewCommentViewModel replyVm = thread.getReplyVm();
        Component replyInput = null;
        if (replyVm != null) {
            Component resolveLink = CodeReviewCommentUIUtil.createResolveLink(parentDisposable, thread);
            replyInput = CodeReviewCommentTextFieldFactory.create(parentDisposable,
                replyVm,
                ReviewLocalize.reviewCommentsReplyAction(),
                ReviewLocalize.reviewCommentsReplyAction(),
                resolveLink);
            root.add(replyInput);
        }

        // in the editor every part stands on the editor background the frame around them has - on the desktop a
        // layout paints a background of its own otherwise
        if (compact) {
            root.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
            foldedControls.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
            if (replyInput != null) {
                replyInput.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
            }
        }

        Component reply = replyInput;
        Disposable[] itemsDisposable = {Disposable.newDisposable()};
        Disposer.register(parentDisposable, () -> Disposer.dispose(itemsDisposable[0]));
        ReviewUIUtil.bind(parentDisposable, thread.getComments(), thread.getRepliesFolded(), (comments, folded) -> {
            Disposer.dispose(itemsDisposable[0]);
            itemsDisposable[0] = Disposable.newDisposable();

            messages.clear();
            List<CodeReviewCommentViewModel> shown = folded && !comments.isEmpty() ? comments.subList(0, 1) : comments;
            for (CodeReviewCommentViewModel comment : shown) {
                CodeReviewCommentItemFactory.add(itemsDisposable[0], messages, comment);
            }

            foldedControls.setVisible(folded);
            if (reply != null) {
                reply.setVisible(!folded && thread.getCanCreateReplies().get());
            }
        });
        return root;
    }

    @RequiredUIAccess
    private static Component createHeader(Disposable parentDisposable, CodeReviewThreadViewModel thread, boolean showLocation) {
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

        Label resolvedTag = ReviewListCellRenderer.createTagLabel();
        resolvedTag.setText(ReviewLocalize.reviewCommentsResolvedTag());
        ReviewUIUtil.bind(parentDisposable, thread.isResolved(), resolvedTag::setVisible);
        header.add(resolvedTag);
        return header;
    }
}
