// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.editor;

import consulo.document.Document;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Document helpers needed by the review-in-editor renderers.
 * <p>
 * Kept separate from {@code ReviewInEditorUtil} because the renderers must be loadable on a split-mode frontend, while
 * {@code ReviewInEditorUtil} also owns the inspection-widget toolbar and therefore pulls in actions and the message
 * bundle.
 */
public final class CodeReviewEditorDocumentUtil {
    private CodeReviewEditorDocumentUtil() {
    }

    public static boolean isLastBlankLine(Document document, int lineIdx) {
        int lineCount = Math.max(document.getLineCount(), 1);
        if (lineIdx != lineCount - 1) {
            return false;
        }
        int start = document.getLineStartOffset(lineIdx);
        int end = document.getLineEndOffset(lineIdx);
        return start == end;
    }

    /**
     * Maps a line index in the "before" side of {@code ranges} to the corresponding line in the "after" side.
     */
    public static int transferLineToAfter(List<CodeReviewChangedRange> ranges, int line) {
        if (ranges.isEmpty()) {
            return line;
        }
        int result = line;
        for (CodeReviewChangedRange range : ranges) {
            if (line >= range.baseLine1() && line < range.baseLine2()) {
                return Math.max(range.line2() - 1, 0);
            }

            if (range.baseLine2() > line) {
                return result;
            }

            int length1 = range.baseLine2() - range.baseLine1();
            int length2 = range.line2() - range.line1();
            result += length2 - length1;
        }
        return result;
    }

    /**
     * Maps a line index in the "after" side of {@code ranges} back to the "before" side.
     * <p>
     * Returns {@code null} when the line only exists in the "after" side, unless {@code approximate} is set.
     */
    public static @Nullable Integer transferLineFromAfter(List<CodeReviewChangedRange> ranges, int line, boolean approximate) {
        if (ranges.isEmpty()) {
            return line;
        }
        int result = line;
        for (CodeReviewChangedRange range : ranges) {
            if (line < range.line1()) {
                return result;
            }

            if (line >= range.line1() && line < range.line2()) {
                return approximate ? range.baseLine2() : null;
            }

            int length1 = range.baseLine2() - range.baseLine1();
            int length2 = range.line2() - range.line1();
            result -= length2 - length1;
        }
        return result;
    }
}
