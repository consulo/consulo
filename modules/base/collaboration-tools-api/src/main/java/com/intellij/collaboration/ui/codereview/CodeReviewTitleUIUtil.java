// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview;

import consulo.application.util.HtmlChunk;
import consulo.ui.ex.awt.util.ColorUtil;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

public final class CodeReviewTitleUIUtil {
    private CodeReviewTitleUIUtil() {
    }

    public static @Nonnull String createTitleText(
        @Nonnull String title,
        @Nonnull String reviewNumber,
        @Nonnull String url,
        @Nls @Nonnull String tooltip
    ) {
        HtmlChunk reviewNumberLink = HtmlChunk
            .link(url, reviewNumber)
            .attr("title", tooltip)
            .wrapWith(HtmlChunk.font(ColorUtil.toHex(NamedColorUtil.getInactiveTextColor())));

        return new HtmlBuilder()
            .appendRaw(unwrap(title))
            .nbsp()
            .append(reviewNumberLink)
            .toString();
    }

    private static @Nonnull String unwrap(@Nonnull String s) {
        String result = s;
        if (result.startsWith("<body>")) {
            result = result.substring("<body>".length());
        }
        if (result.endsWith("</body>")) {
            result = result.substring(0, result.length() - "</body>".length());
        }
        if (result.startsWith("<p>")) {
            result = result.substring("<p>".length());
        }
        if (result.endsWith("</p>")) {
            result = result.substring(0, result.length() - "</p>".length());
        }
        return result;
    }
}
