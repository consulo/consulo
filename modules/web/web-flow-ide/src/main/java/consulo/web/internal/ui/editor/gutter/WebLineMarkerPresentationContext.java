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
package consulo.web.internal.ui.editor.gutter;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.markup.LineMarkerPresentationContext;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

/**
 * The browser has no viewport the server knows about and no gutter hover it reports, so the visible range is
 * the whole document and there is no hovered line. Everything reaching a provider stays the same otherwise -
 * a provider must not be able to tell which frontend it is building for.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public class WebLineMarkerPresentationContext implements LineMarkerPresentationContext {
    private final Editor myEditor;
    private final int myStartLine;
    private final int myEndLine;

    public WebLineMarkerPresentationContext(Editor editor, RangeHighlighter highlighter) {
        myEditor = editor;

        Document document = editor.getDocument();
        myStartLine = toLine(document, highlighter.getStartOffset());
        // exclusive, so a highlighter covering a single line yields [line, line + 1) rather than the
        // degenerate [line, line) that denotes a boundary between lines
        myEndLine = Math.min(document.getLineCount(), toLine(document, highlighter.getEndOffset()) + 1);
    }

    private static int toLine(Document document, int offset) {
        int lastLine = Math.max(0, document.getLineCount() - 1);
        if (offset <= 0) {
            return 0;
        }
        if (offset >= document.getTextLength()) {
            return lastLine;
        }
        return Math.min(lastLine, document.getLineNumber(offset));
    }

    @Override
    public int startLine() {
        return myStartLine;
    }

    @Override
    public int endLine() {
        return myEndLine;
    }

    @Override
    public int firstVisibleLine() {
        return 0;
    }

    @Override
    public int lastVisibleLine() {
        return lineCount();
    }

    @Override
    public int lineCount() {
        return myEditor.getDocument().getLineCount();
    }

    @Override
    public int hoveredLine() {
        return -1;
    }

    @Override
    public boolean isLineNumbersShown() {
        return myEditor.getSettings().isLineNumbersShown();
    }

    @Override
    public boolean isAnnotationsShown() {
        return false;
    }

    @Override
    public boolean isMirrored() {
        return false;
    }

    @Override
    public @Nullable ColorValue getColor(EditorColorKey key) {
        return myEditor.getColorsScheme().getColor(key);
    }

    @Override
    public ColorValue getEditorBackgroundColor() {
        return myEditor instanceof EditorEx editorEx
            ? editorEx.getBackgroundColor()
            : myEditor.getColorsScheme().getDefaultBackground();
    }

    @Override
    public TextAttributes getAttributes(TextAttributesKey key) {
        EditorColorsScheme scheme = myEditor.getColorsScheme();
        TextAttributes attributes = scheme.getAttributes(key);
        return attributes != null ? attributes : new TextAttributes();
    }
}
