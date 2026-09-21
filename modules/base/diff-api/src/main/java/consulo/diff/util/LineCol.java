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
package consulo.diff.util;

import consulo.document.Document;
import consulo.codeEditor.Editor;

// counting from zero
public record LineCol(int line, int column) {
    public LineCol(int line) {
        this(line, 0);
    }

    public static LineCol fromOffset(Document document, int offset) {
        int line = document.getLineNumber(offset);
        int column = offset - document.getLineStartOffset(line);
        return new LineCol(line, column);
    }

    public static LineCol fromCaret(Editor editor) {
        return fromOffset(editor.getDocument(), editor.getCaretModel().getOffset());
    }

    public static int toOffset(Document document, LineCol linecol) {
        return linecol.toOffset(document);
    }

    public static int toOffset(Document document, int line, int col) {
        return new LineCol(line, col).toOffset(document);
    }

    public int toOffset(Document document) {
        return document.getLineStartOffset(line) + column;
    }

    public int toOffset(Editor editor) {
        return toOffset(editor.getDocument());
    }
}
