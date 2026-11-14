// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.suggested;

import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import org.jspecify.annotations.Nullable;

import java.util.function.IntPredicate;

public final class TextRangeUtils {
    private TextRangeUtils() {
    }

    public static TextRange stripWhitespace(TextRange range, CharSequence chars) {
        return strip(range, chars, Character::isWhitespace);
    }

    public static TextRange strip(TextRange range, CharSequence chars, IntPredicate predicate) {
        if (range.getStartOffset() < 0 || range.getEndOffset() > chars.length()) {
            throw new IllegalArgumentException(range + " is outside of " + chars.length());
        }

        int startOffset = range.getStartOffset();
        int endOffset = range.getEndOffset();
        while (startOffset < endOffset && predicate.test(chars.charAt(startOffset))) {
            startOffset++;
        }

        while (endOffset > startOffset && predicate.test(chars.charAt(endOffset - 1))) {
            endOffset--;
        }

        return new TextRange(startOffset, endOffset);
    }

    public static TextRange extendWithWhitespace(TextRange range, CharSequence chars) {
        return extend(range, chars, Character::isWhitespace);
    }

    public static TextRange extend(TextRange range, CharSequence chars, IntPredicate predicate) {
        if (range.getStartOffset() < 0 || range.getEndOffset() > chars.length()) {
            throw new IllegalArgumentException(range + " is outside of " + chars.length());
        }

        int startOffset = range.getStartOffset();
        while (startOffset > 0 && predicate.test(chars.charAt(startOffset - 1))) {
            startOffset--;
        }

        int endOffset = range.getEndOffset();
        while (endOffset < chars.length() && predicate.test(chars.charAt(endOffset))) {
            endOffset++;
        }

        return new TextRange(startOffset, endOffset);
    }

    /**
     * @return range of the marker, or <i>null</i> if the marker is not valid
     */
    public static @Nullable TextRange asTextRange(RangeMarker marker) {
        if (!marker.isValid()) {
            return null;
        }
        int start = marker.getStartOffset();
        int end = marker.getEndOffset();
        if (start >= 0 && start <= end) {
            return new TextRange(start, end);
        }
        return null;
    }
}
