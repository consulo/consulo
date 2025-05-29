/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.document.util;

import consulo.document.Document;
import consulo.document.internal.DocumentEx;
import jakarta.annotation.Nonnull;

/**
 * Is intended to hold utility methods to use during {@link Document} processing.
 */
public final class DocumentUtil {
    private DocumentUtil() {
    }

    /**
     * Ensures that given task is executed when given document is at the given 'in bulk' mode.
     *
     * @param document      target document
     * @param executeInBulk {@code true} to force given document to be in bulk mode when given task is executed;
     *                      {@code false} to force given document to be <b>not</b> in bulk mode when given task is executed
     * @param task          task to execute
     */
    public static void executeInBulk(@Nonnull Document document, final boolean executeInBulk, @Nonnull Runnable task) {
        if (!(document instanceof DocumentEx)) {
            task.run();
            return;
        }

        DocumentEx documentEx = (DocumentEx) document;
        if (executeInBulk == documentEx.isInBulkUpdate()) {
            task.run();
            return;
        }

        documentEx.setInBulkUpdate(executeInBulk);
        try {
            task.run();
        }
        finally {
            documentEx.setInBulkUpdate(!executeInBulk);
        }
    }

    public static int getFirstNonSpaceCharOffset(@Nonnull Document document, int line) {
        int startOffset = document.getLineStartOffset(line);
        int endOffset = document.getLineEndOffset(line);
        return getFirstNonSpaceCharOffset(document, startOffset, endOffset);
    }

    public static int getFirstNonSpaceCharOffset(@Nonnull Document document, int startOffset, int endOffset) {
        CharSequence text = document.getImmutableCharSequence();
        for (int i = startOffset; i < endOffset; i++) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t') {
                return i;
            }
        }
        return startOffset;
    }

    public static boolean isValidOffset(int offset, @Nonnull Document document) {
        return offset >= 0 && offset <= document.getTextLength();
    }

    public static int getLineStartOffset(int offset, @Nonnull Document document) {
        if (offset < 0 || offset > document.getTextLength()) {
            return offset;
        }
        int lineNumber = document.getLineNumber(offset);
        return document.getLineStartOffset(lineNumber);
    }

    public static int getLineEndOffset(int offset, @Nonnull Document document) {
        if (offset < 0 || offset > document.getTextLength()) {
            return offset;
        }
        int lineNumber = document.getLineNumber(offset);
        return document.getLineEndOffset(lineNumber);
    }

    @Nonnull
    public static TextRange getLineTextRange(@Nonnull Document document, int line) {
        return TextRange.create(document.getLineStartOffset(line), document.getLineEndOffset(line));
    }

    public static boolean isAtLineStart(int offset, @Nonnull Document document) {
        return offset >= 0 && offset <= document.getTextLength() && offset == document.getLineStartOffset(document.getLineNumber(offset));
    }

    public static boolean isAtLineEnd(int offset, @Nonnull Document document) {
        return offset >= 0 && offset <= document.getTextLength() && offset == document.getLineEndOffset(document.getLineNumber(offset));
    }

    public static int alignToCodePointBoundary(@Nonnull Document document, int offset) {
        return isInsideSurrogatePair(document, offset) ? offset - 1 : offset;
    }

    public static boolean isSurrogatePair(@Nonnull Document document, int offset) {
        CharSequence text = document.getImmutableCharSequence();
        if (offset < 0 || (offset + 1) >= text.length()) {
            return false;
        }
        return Character.isSurrogatePair(text.charAt(offset), text.charAt(offset + 1));
    }

    public static boolean isInsideSurrogatePair(@Nonnull Document document, int offset) {
        return isSurrogatePair(document, offset - 1);
    }

    public static int getPreviousCodePointOffset(@Nonnull Document document, int offset) {
        return offset - (isSurrogatePair(document, offset - 2) ? 2 : 1);
    }

    public static int getNextCodePointOffset(@Nonnull Document document, int offset) {
        return offset + (isSurrogatePair(document, offset) ? 2 : 1);
    }

    public static boolean isLineEmpty(@Nonnull Document document, final int line) {
        final CharSequence chars = document.getCharsSequence();
        int start = document.getLineStartOffset(line);
        int end = Math.min(document.getLineEndOffset(line), document.getTextLength() - 1);
        for (int i = start; i <= end; i++) {
            if (!Character.isWhitespace(chars.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidLine(int line, @Nonnull Document document) {
        if (line < 0) {
            return false;
        }
        int lineCount = document.getLineCount();
        return lineCount == 0 ? line == 0 : line < lineCount;
    }

    /**
     * Calculates offset of the first non-whitespace character of the {@code line}
     * In case of empty line it returns line end's offset.
     *
     * @see #getFirstNonSpaceCharOffset(Document, int)
     */
    public static int getLineStartIndentedOffset(@Nonnull Document document, int line) {
        int lineStartOffset = document.getLineStartOffset(line);
        return lineStartOffset + getIndentLengthAtLineStart(document, lineStartOffset);
    }

    private static int getIndentLengthAtLineStart(@Nonnull Document document, int lineOffset) {
        CharSequence content = document.getImmutableCharSequence();
        int result = 0;
        while (lineOffset + result < content.length()) {
            char ch = content.charAt(lineOffset + result);
            if (!Character.isWhitespace(ch) || ch == '\n' || ch == '\r') {
                break;
            }
            result++;
        }
        return result;
    }

    /**
     * Calculates indent of the line containing {@code offset}
     *
     * @return Whitespaces at the beginning of the line
     */
    public static CharSequence getIndent(@Nonnull Document document, int offset) {
        int lineOffset = getLineStartOffset(offset, document);
        return document.getCharsSequence().subSequence(lineOffset, lineOffset + getIndentLengthAtLineStart(document, lineOffset));
    }
}
