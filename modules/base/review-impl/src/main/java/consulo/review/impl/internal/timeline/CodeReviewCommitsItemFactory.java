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

import consulo.application.util.DateFormatUtil;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.review.details.CommitPresentation;
import consulo.review.list.UserPresentation;
import consulo.review.localize.ReviewLocalize;
import consulo.review.timeline.CodeReviewCommitsTimelineItem;
import consulo.review.timeline.CodeReviewTimelineCommit;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.util.lang.xml.XmlStringUtil;

import java.util.Date;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class CodeReviewCommitsItemFactory {
    private CodeReviewCommitsItemFactory() {
    }

    @RequiredUIAccess
    public static Component create(Disposable parentDisposable, CodeReviewCommitsTimelineItem item) {
        List<CodeReviewTimelineCommit> commits = item.getCommits();
        int commitsCount = commits.size();

        UserPresentation author = item.getAuthor();
        LocalizeValue title = commitsCount == 1
            ? ReviewLocalize.reviewTimelineCommitAdded()
            : ReviewLocalize.reviewTimelineCommitsAdded(commitsCount);

        MessageList header = CodeReviewCommentItemFactory.createList();
        MessageListItem headerItem = header.add(LocalizeValue.of(author.getPresentableName()), title.get());
        Date createdAt = item.getCreatedAt();
        if (createdAt != null) {
            headerItem.setTime(createdAt.toInstant());
        }
        headerItem.setAuthorImage(author.getAvatarIcon());

        Toast commitsBlock = Toast.create();
        commitsBlock.addStyle(ToastStyle.INFO);
        commitsBlock.addStyle(ToastStyle.LEFT_LINE);
        commitsBlock.setHtmlText(LocalizeValue.of(buildCommitsHtml(commits)));
        commitsBlock.addHyperlinkListener(event -> item.showCommit(event.getDescription()));

        VerticalLayout blockHolder = VerticalLayout.create(Space.NONE);
        blockHolder.add(commitsBlock);
        blockHolder.paddingBuilder().horizontalSet(Space.X_LARGE).bottomSet(Space.LARGE).apply();

        VerticalLayout root = VerticalLayout.create(Space.NONE);
        root.add(header);
        root.add(blockHolder);
        return root;
    }

    private static String buildCommitsHtml(List<CodeReviewTimelineCommit> commits) {
        StringBuilder html = new StringBuilder();
        for (CodeReviewTimelineCommit commit : commits) {
            CommitPresentation presentation = commit.presentation();
            html.append("<p><a href=\"").append(XmlStringUtil.escapeText(commit.hash())).append("\">")
                .append(XmlStringUtil.escapeText(commit.abbreviatedHash())).append("</a>&nbsp;")
                .append(presentation.titleHtml())
                .append("<br>")
                .append(XmlStringUtil.escapeText(presentation.authorAndDate(DateFormatUtil::formatPrettyDateTime)))
                .append("</p>");
        }
        return html.toString();
    }
}
