// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview;

import com.intellij.collaboration.ui.SimpleHtmlPane;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.HtmlChunk;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.util.Date;

public final class CodeReviewTimelineUIUtil {
    public static final int VERTICAL_GAP = 4;

    public static final int VERT_PADDING = 6;
    public static final int HEADER_VERT_PADDING = 20;

    public static final int ITEM_HOR_PADDING = 16;
    public static final int ITEM_VERT_PADDING = 10;

    public static @Nonnull Border getItemBorder() {
        return JBUI.Borders.empty(ITEM_HOR_PADDING, ITEM_VERT_PADDING);
    }

    private CodeReviewTimelineUIUtil() {
    }

    public static final class Thread {
        public static final int DIFF_TEXT_GAP = 8;

        private Thread() {
        }

        public static final class Replies {
            private Replies() {
            }

            public static final class ActionsFolded {
                public static final int VERTICAL_PADDING = 8;
                public static final int HORIZONTAL_GAP = 8;
                public static final int HORIZONTAL_GROUP_GAP = 14;

                private ActionsFolded() {
                }
            }
        }
    }

    public static @Nonnull JComponent createTitleTextPane(
        @Nls @Nonnull String authorName,
        @Nullable String authorUrl,
        @Nullable Date date
    ) {
        String titleText = getTitleHtml(authorName, authorUrl, date);
        SimpleHtmlPane titleTextPane = new SimpleHtmlPane(titleText);
        titleTextPane.setForeground(UIUtil.getContextHelpForeground());
        return titleTextPane;
    }

    private static @Nonnull String getTitleHtml(
        @Nls @Nonnull String authorName,
        @Nullable String authorUrl,
        @Nullable Date date
    ) {
        HtmlChunk userNameChunk = authorUrl != null
            ? HtmlChunk.link(authorUrl, authorName)
            : HtmlChunk.text(authorName);
        HtmlChunk userNameLink = userNameChunk
            .wrapWith(HtmlChunk.font(ColorUtil.toHtmlColor(UIUtil.getLabelForeground())))
            .bold();
        HtmlBuilder builder = new HtmlBuilder().append(userNameLink);
        if (date != null) {
            builder.append(HtmlChunk.nbsp())
                .append(DateFormatUtil.formatPrettyDateTime(date));
        }

        return builder.toString();
    }
}
