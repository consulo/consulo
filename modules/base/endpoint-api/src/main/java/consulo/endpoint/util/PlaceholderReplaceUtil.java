// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.util;

import consulo.document.util.PlaceholderTextRanges;
import consulo.document.util.TextRange;

import java.util.Set;

public final class PlaceholderReplaceUtil {
    private PlaceholderReplaceUtil() {
    }

    public static String substituteUrlVariables(String urlPattern, String prefix, String suffix, String replacement) {
        String url = urlPattern;
        Set<TextRange> ranges = PlaceholderTextRanges.getPlaceholderRanges(url, prefix, suffix, false, true);
        if (ranges.isEmpty()) {
            return url;
        }

        StringBuilder builder = new StringBuilder(url.length());
        int offset = 0;
        for (TextRange range : ranges) {
            builder.append(url, offset, range.getStartOffset() - prefix.length());
            builder.append(replacement);
            offset = range.getEndOffset() + suffix.length();
        }
        builder.append(url.substring(offset));
        url = builder.toString();
        return url;
    }
}
