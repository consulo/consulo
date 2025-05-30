/*
 * Copyright 2013-2017 consulo.io
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
package consulo.language.codeStyle;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.util.lang.CharArrayUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * from kotlin platform\lang-impl\src\com\intellij\psi\formatter\IndentRangesCalculator.kt
 *
 * @author VISTALL
 * @since 2017-05-01
 */
public class IndentRangesCalculator {
    private Document document;
    private TextRange textRange;

    private int startOffset = textRange.getStartOffset();
    private int endOffset = textRange.getEndOffset();

    public IndentRangesCalculator(Document document, TextRange textRange) {
        this.document = document;
        this.textRange = textRange;
    }

    public List<TextRange> calcIndentRanges() {
        int startLine = document.getLineNumber(startOffset);
        int endLine = document.getLineNumber(endOffset);
        CharSequence chars = document.getCharsSequence();

        List<TextRange> indentRanges = new ArrayList<>();

        for (int line = startLine; line <= endLine; line++) {
            int lineStartOffset = document.getLineStartOffset(line);
            int lineEndOffset = document.getLineEndOffset(line);
            int firstNonWsChar = CharArrayUtil.shiftForward(chars, lineStartOffset, lineEndOffset + 1, " \t");
            indentRanges.add(new TextRange(lineStartOffset, firstNonWsChar));
        }

        return indentRanges;
    }
}
