// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review;

import consulo.application.util.DateFormatUtil;
import consulo.application.util.HtmlBuilder;
import consulo.application.util.HtmlChunk;
import consulo.localize.LocalizeValue;
import consulo.ui.HtmlLabel;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Date;

public final class CodeReviewTimelineUIUtil {
    public static final int VERTICAL_GAP = 4;

    public static final int VERT_PADDING = 6;
    public static final int HEADER_VERT_PADDING = 20;

    public static final int ITEM_HOR_PADDING = 16;
    public static final int ITEM_VERT_PADDING = 10;

    public static final int THREAD_DIFF_TEXT_GAP = 8;

    public static final int THREAD_REPLIES_ACTIONS_FOLDED_VERTICAL_PADDING = 8;
    public static final int THREAD_REPLIES_ACTIONS_FOLDED_HORIZONTAL_GAP = 8;
    public static final int THREAD_REPLIES_ACTIONS_FOLDED_HORIZONTAL_GROUP_GAP = 14;

    private CodeReviewTimelineUIUtil() {
    }

    @RequiredUIAccess
    public static HtmlLabel createTitleTextPane(LocalizeValue authorName, @Nullable String authorUrl, @Nullable Date date) {
        return HtmlLabel.create(LocalizeValue.of(getTitleHtml(authorName, authorUrl, date)));
    }

    public static String getTitleHtml(LocalizeValue authorName, @Nullable String authorUrl, @Nullable Date date) {
        HtmlChunk userNameLink = (authorUrl != null ? HtmlChunk.link(authorUrl, authorName) : HtmlChunk.text(authorName))
            .wrapWith(HtmlChunk.span().setClass("author-name"))
            .bold();
        HtmlBuilder builder = new HtmlBuilder().append(userNameLink);
        if (date != null) {
            builder.append(HtmlChunk.nbsp()).append(DateFormatUtil.formatPrettyDateTime(date));
        }
        return builder.toString();
    }
}
