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
package consulo.desktop.qt.editor.impl;

import consulo.codeEditor.Caret;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.impl.CaretData;
import consulo.codeEditor.impl.IterationState;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import io.qt.core.QPointF;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.gui.QColor;
import io.qt.gui.QPainter;

import java.awt.Point;

/**
 * Draws the visible text of the editor.
 * <p>
 * The awt {@code EditorPainter} runs sixteen ordered phases over a whole visible-line range; this is the first two
 * of them - backgrounds then text - done per line. The ordering matters and is kept: every background in a line is
 * laid down before any glyph of that line, so a highlighter that starts mid-line cannot paint over the character
 * that precedes it. {@link IterationState} is shared with awt rather than reimplemented, since it lives in the
 * frontend-neutral base and already merges lexer, markup, selection and caret-row attributes.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorPainter {
    private static final int CARET_WIDTH = 2;

    private final DesktopQtEditorImpl myEditor;

    public DesktopQtEditorPainter(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    public void paint(QPainter painter, QRect clip, int scrollX, int scrollY, boolean paintCarets) {
        EditorColorsScheme scheme = myEditor.getColorsScheme();
        Document document = myEditor.getDocument();
        DesktopQtEditorFontMetrics metrics = myEditor.getFontMetrics();

        painter.fillRect(clip, toQColor(scheme.getDefaultBackground()));

        int lineHeight = metrics.getLineHeight();
        int firstLine = Math.max(0, (clip.top() + scrollY) / lineHeight);
        int lastLine = Math.min(document.getLineCount() - 1, (clip.bottom() + scrollY) / lineHeight);

        if (lastLine >= firstLine) {
            CaretData caretData = CaretData.createCaretData(document, myEditor.getCaretModel());

            for (int line = firstLine; line <= lastLine; line++) {
                paintLine(painter, line, line * lineHeight - scrollY, -scrollX, caretData);
            }
        }

        if (paintCarets) {
            paintCarets(painter, scrollX, scrollY);
        }
    }

    private void paintLine(QPainter painter, int line, int y, double startX, CaretData caretData) {
        Document document = myEditor.getDocument();
        DesktopQtEditorFontMetrics metrics = myEditor.getFontMetrics();
        EditorColorsScheme scheme = myEditor.getColorsScheme();

        int lineStart = document.getLineStartOffset(line);
        int lineEnd = document.getLineEndOffset(line);
        CharSequence text = document.getCharsSequence();

        int lineHeight = metrics.getLineHeight();
        int baseline = y + metrics.getAscent();

        // first pass lays down every background of the line, second pass draws the glyphs over them
        double x = startX;
        for (IterationState state = createIterationState(lineStart, lineEnd, caretData); !state.atEnd(); state.advance()) {
            CharSequence chunk = text.subSequence(state.getStartOffset(), state.getEndOffset());
            TextAttributes attributes = state.getMergedAttributes();
            double width = metrics.getTextWidth(chunk, attributes.getFontType());

            ColorValue background = attributes.getBackgroundColor();
            if (background != null) {
                painter.fillRect(new QRectF(x, y, width, lineHeight), toQColor(background));
            }

            x += width;
        }

        x = startX;
        for (IterationState state = createIterationState(lineStart, lineEnd, caretData); !state.atEnd(); state.advance()) {
            CharSequence chunk = text.subSequence(state.getStartOffset(), state.getEndOffset());
            TextAttributes attributes = state.getMergedAttributes();
            int fontType = attributes.getFontType();

            ColorValue foreground = attributes.getForegroundColor();
            painter.setFont(metrics.getFont(fontType));
            painter.setPen(toQColor(foreground == null ? scheme.getDefaultForeground() : foreground));

            // the x must stay fractional. iteration splits a line at the caret, so rounding each chunk's origin
            // would make the glyphs after the caret jump by a pixel every time the caret moved through the line
            painter.drawText(new QPointF(x, baseline), chunk.toString());

            x += metrics.getTextWidth(chunk, fontType);
        }
    }

    private IterationState createIterationState(int startOffset, int endOffset, CaretData caretData) {
        return new IterationState(myEditor, startOffset, endOffset, caretData, false, false, false, false);
    }

    private void paintCarets(QPainter painter, int scrollX, int scrollY) {
        ColorValue caretColor = myEditor.getColorsScheme().getColor(EditorColors.CARET_COLOR);
        if (caretColor == null) {
            caretColor = myEditor.getColorsScheme().getDefaultForeground();
        }

        int lineHeight = myEditor.getLineHeight();
        QColor color = toQColor(caretColor);

        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            LogicalPosition position = caret.getLogicalPosition();
            Point point = myEditor.visualPositionToXY(myEditor.logicalToVisualPosition(position));

            painter.fillRect(new QRect(point.x - scrollX, point.y - scrollY, CARET_WIDTH, lineHeight), color);
        }
    }

    private static QColor toQColor(ColorValue colorValue) {
        RGBColor rgb = colorValue.toRGB();
        return new QColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), rgb.getAlpha());
    }
}
