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
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.impl.CaretData;
import consulo.codeEditor.impl.IterationState;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.ui.color.ColorValue;
import consulo.desktop.qt.ui.impl.TargetQt;
import io.qt.core.QPointF;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.gui.QColor;
import io.qt.gui.QPainter;
import org.jspecify.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

        painter.fillRect(clip, TargetQt.to(scheme.getDefaultBackground()));

        int lineHeight = metrics.getLineHeight();
        int firstLine = Math.max(0, (clip.top() + scrollY) / lineHeight);
        int lastLine = Math.min(myEditor.getVisualLines().getVisualLineCount() - 1, (clip.bottom() + scrollY) / lineHeight);

        if (lastLine >= firstLine) {
            CaretData caretData = CaretData.createCaretData(document, myEditor.getCaretModel());

            // the caret row reaches the right edge of the editor, while the attributes iteration only knows about
            // the text - so the band is laid down first and every background of the line then paints over it
            ColorValue caretRowColor = caretRowBackground();
            Set<Integer> caretRows = caretRowColor == null ? Set.of() : caretRows();

            for (int line = firstLine; line <= lastLine; line++) {
                int y = line * lineHeight - scrollY;

                if (caretRows.contains(line)) {
                    painter.fillRect(clip.left(), y, clip.width(), lineHeight, TargetQt.to(caretRowColor));
                }

                paintLine(painter, line, y, -scrollX, caretData);
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

        CharSequence text = document.getCharsSequence();

        int lineHeight = metrics.getLineHeight();
        int baseline = y + metrics.getAscent();

        List<Run> runs = collectVisualLineRuns(line, caretData);

        // first pass lays down every background of the line, second pass draws the glyphs over them
        double x = startX;
        for (Run run : runs) {
            CharSequence chunk = run.text(text);
            double width = metrics.getTextWidth(chunk, run.fontType());

            if (run.background() != null) {
                painter.fillRect(new QRectF(x, y, width, lineHeight), TargetQt.to(run.background()));
            }

            x += width;
        }

        x = startX;
        for (Run run : runs) {
            CharSequence chunk = run.text(text);
            int fontType = run.fontType();

            ColorValue foreground = run.foreground();
            painter.setFont(metrics.getFont(fontType));
            painter.setPen(TargetQt.to(foreground == null ? scheme.getDefaultForeground() : foreground));

            // the x must stay fractional. iteration splits a line at the caret, so rounding each chunk's origin
            // would make the glyphs after the caret jump by a pixel every time the caret moved through the line
            painter.drawText(new QPointF(x, baseline), chunk.toString());

            x += metrics.getTextWidth(chunk, fontType);
        }
    }

    /**
     * A stretch of the line that is drawn in one go.
     */
    private record Run(
        int startOffset,
        int endOffset,
        @Nullable ColorValue foreground,
        @Nullable ColorValue background,
        int fontType,
        @Nullable String placeholder
    ) {
        CharSequence text(CharSequence documentText) {
            return placeholder != null ? placeholder : documentText.subSequence(startOffset, endOffset);
        }
    }

    /**
     * The runs of a whole visual line. Folding is not unpicked here: {@link IterationState} is asked to honour
     * fold regions and then walks them itself, off the fold tree's own cached top level array, reporting each
     * collapsed region as a single segment whose merged attributes already carry the placeholder styling - and
     * the selection or caret row over it, which attributes read straight off the scheme would have lost.
     */
    private List<Run> collectVisualLineRuns(int visualLine, CaretData caretData) {
        DesktopQtEditorVisualLines visualLines = myEditor.getVisualLines();

        return collectRuns(visualLines.visualLineStartOffset(visualLine), visualLines.visualLineEndOffset(visualLine), caretData);
    }

    /**
     * The line cut into the fewest pieces that still draw correctly.
     * <p>
     * {@link IterationState} hands out one lexer token at a time, so an operator like {@code ->} arrives as two
     * segments even though nothing about them differs. Drawing each on its own would be two {@code drawText}
     * calls with a shaping run each, and a ligature cannot form across those - which is why the arrow of a
     * coding font never appeared. Neighbours that would be drawn identically are therefore welded back together,
     * which also cuts the number of draw calls per line by an order of magnitude.
     */
    private List<Run> collectRuns(int lineStart, int lineEnd, CaretData caretData) {
        List<Run> runs = new ArrayList<>();

        for (IterationState state = createIterationState(lineStart, lineEnd, caretData); !state.atEnd(); state.advance()) {
            TextAttributes attributes = state.getMergedAttributes();

            ColorValue foreground = attributes.getForegroundColor();
            ColorValue background = attributes.getBackgroundColor();
            int fontType = attributes.getFontType();

            FoldRegion fold = state.getCurrentFold();
            if (fold != null) {
                runs.add(new Run(
                    state.getStartOffset(),
                    state.getEndOffset(),
                    foreground,
                    background,
                    fontType,
                    fold.getPlaceholderText()
                ));
                continue;
            }

            Run last = runs.isEmpty() ? null : runs.get(runs.size() - 1);

            if (last != null && last.placeholder() == null
                && last.endOffset() == state.getStartOffset()
                && last.fontType() == fontType
                && Objects.equals(last.foreground(), foreground)
                && Objects.equals(last.background(), background)) {
                runs.set(runs.size() - 1, new Run(last.startOffset(), state.getEndOffset(), foreground, background, fontType, null));
            }
            else {
                runs.add(new Run(state.getStartOffset(), state.getEndOffset(), foreground, background, fontType, null));
            }
        }

        return runs;
    }

    /**
     * The colour the row of a caret is washed with, or null when the setting is off. Read from the caret model
     * rather than from the scheme so that the band and the attributes {@link IterationState} merges into the text
     * of the same row cannot disagree.
     */
    private @Nullable ColorValue caretRowBackground() {
        return myEditor.isRendererMode() ? null : myEditor.getCaretModel().getTextAttributes().getBackgroundColor();
    }

    private Set<Integer> caretRows() {
        Set<Integer> lines = new HashSet<>();
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            lines.add(caret.getVisualPosition().line);
        }
        return lines;
    }

    private IterationState createIterationState(int startOffset, int endOffset, CaretData caretData) {
        // folds on: the state then reports a collapsed region as one segment instead of the text under it
        return new IterationState(myEditor, startOffset, endOffset, caretData, false, false, true, false);
    }

    private void paintCarets(QPainter painter, int scrollX, int scrollY) {
        ColorValue caretColor = myEditor.getColorsScheme().getColor(EditorColors.CARET_COLOR);
        if (caretColor == null) {
            caretColor = myEditor.getColorsScheme().getDefaultForeground();
        }

        int lineHeight = myEditor.getLineHeight();
        QColor color = TargetQt.to(caretColor);

        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            LogicalPosition position = caret.getLogicalPosition();
            Point point = myEditor.visualPositionToXY(myEditor.logicalToVisualPosition(position));

            painter.fillRect(new QRect(point.x - scrollX, point.y - scrollY, CARET_WIDTH, lineHeight), color);
        }
    }
}
