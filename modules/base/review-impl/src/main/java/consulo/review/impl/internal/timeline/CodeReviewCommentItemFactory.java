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
import consulo.localize.LocalizeValue;
import consulo.review.impl.internal.ReviewUIUtil;
import consulo.review.list.UserPresentation;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemStyle;

/**
 * Review comments are chat items of a {@link MessageList}: the author with the avatar, the time, and the markdown body.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class CodeReviewCommentItemFactory {
    private CodeReviewCommentItemFactory() {
    }

    @RequiredUIAccess
    public static MessageList createList() {
        MessageList messages = MessageList.create();
        messages.setMarkdown(true);
        messages.setShowOwnAvatar(true);
        return messages;
    }

    @RequiredUIAccess
    public static MessageListItem add(Disposable parentDisposable, MessageList messages, CodeReviewCommentViewModel comment) {
        UserPresentation author = comment.getAuthor();
        MessageListItem item = messages.add(LocalizeValue.of(author.getPresentableName()), comment.getBody().get());
        item.setTime(comment.getCreatedAt().toInstant());
        item.setAuthorImage(author.getAvatarIcon());
        item.setStyle(comment.isOwn() ? MessageListItemStyle.OWN : MessageListItemStyle.OTHER);
        ReviewUIUtil.bind(parentDisposable, comment.getBody(), body -> {
            if (!body.equals(item.getText())) {
                item.setText(body);
            }
        });
        return item;
    }
}
