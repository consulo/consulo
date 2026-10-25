// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review;

import consulo.application.util.HtmlBuilder;
import consulo.application.util.HtmlChunk;
import consulo.localize.LocalizeValue;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.ColorValueUtil;

public final class CodeReviewTitleUIUtil {
    private CodeReviewTitleUIUtil() {
    }

    public static String createTitleText(String title, String reviewNumber, String url, LocalizeValue tooltip) {
        HtmlChunk reviewNumberLink = HtmlChunk
            .link(url, LocalizeValue.of(reviewNumber))
            .attr("title", tooltip.get())
            .wrapWith(HtmlChunk.font("#" + ColorValueUtil.toHex(ComponentColors.DISABLED_TEXT)));

        return new HtmlBuilder()
            .appendRaw(unwrap(title))
            .nbsp()
            .append(reviewNumberLink)
            .toString();
    }

    private static String unwrap(String text) {
        String result = removeSuffix(removePrefix(text, "<body>"), "</body>");
        return removeSuffix(removePrefix(result, "<p>"), "</p>");
    }

    private static String removePrefix(String text, String prefix) {
        return text.startsWith(prefix) ? text.substring(prefix.length()) : text;
    }

    private static String removeSuffix(String text, String suffix) {
        return text.endsWith(suffix) ? text.substring(0, text.length() - suffix.length()) : text;
    }
}
