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
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.ui.color.ColorValue;
import consulo.desktop.qt.ui.impl.TargetQt;
import io.qt.core.QPointF;
import io.qt.core.Qt;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPen;
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

        List<Cell> cells = collectCells(line, caretData);

        // first pass lays down every background of the line, second pass draws the glyphs over them
        double x = startX;
        for (Cell cell : cells) {
            Run run = cell.run();

            if (run != null && run.background() != null) {
                painter.fillRect(new QRectF(x, y, cell.width(metrics, text), lineHeight), TargetQt.to(run.background()));
            }

            x += cell.width(metrics, text);
        }

        x = startX;
        for (Cell cell : cells) {
            Run run = cell.run();

            if (run == null) {
                myEditor.getInlays().paint(painter, cell.inlay(), x, y, lineHeight);

                x += cell.width(metrics, text);
                continue;
            }

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

        // effects last: an error is drawn as an underline and nothing else, so it has to survive the glyphs
        x = startX;
        for (Cell cell : cells) {
            double width = cell.width(metrics, text);
            Run run = cell.run();

            if (run != null) {
                paintEffect(painter, x, x + width, baseline, run.effectColor(), run.effectType());
            }

            x += width;
        }
    }

    private void paintEffect(
        QPainter painter,
        double xFrom,
        double xTo,
        int baseline,
        @Nullable ColorValue color,
        @Nullable EffectType type
    ) {
        if (color == null || type == null || xTo <= xFrom) {
            return;
        }

        DesktopQtEditorFontMetrics metrics = myEditor.getFontMetrics();

        QColor qColor = TargetQt.to(color);
        int descent = Math.max(1, metrics.getDescent());

        switch (type) {
            case LINE_UNDERSCORE -> fillLine(painter, xFrom, xTo, baseline + 1, 1, qColor);
            case BOLD_LINE_UNDERSCORE -> fillLine(painter, xFrom, xTo, baseline + 1, Math.min(2, descent), qColor);
            case STRIKEOUT -> fillLine(painter, xFrom, xTo, baseline - metrics.getAscent() / 3, 1, qColor);
            case BOLD_DOTTED_LINE -> paintDotted(painter, xFrom, xTo, baseline + 1, qColor);
            case WAVE_UNDERSCORE -> paintWave(painter, xFrom, xTo, baseline + 1, descent, qColor);
            case BOXED, ROUNDED_BOX, SLIGHTLY_WIDER_BOX -> paintBox(painter, xFrom, xTo, baseline, qColor);
            default -> {
            }
        }
    }

    private static void fillLine(QPainter painter, double xFrom, double xTo, double y, int thickness, QColor color) {
        painter.fillRect(new QRectF(xFrom, y, xTo - xFrom, thickness), color);
    }

    private static void paintDotted(QPainter painter, double xFrom, double xTo, double y, QColor color) {
        for (double x = xFrom; x < xTo; x += 4) {
            painter.fillRect(new QRectF(x, y, Math.min(2, xTo - x), 2), color);
        }
    }

    /**
     * The squiggle an error is drawn with. The wave is kept inside the descent so it cannot reach into the line
     * below, and its period is fixed in pixels rather than derived from the font, the way awt's effect painter
     * does it.
     */
    private static void paintWave(QPainter painter, double xFrom, double xTo, double y, int descent, QColor color) {
        double height = Math.max(2, Math.min(3, descent));
        double period = 4;

        QPainterPath path = new QPainterPath();
        path.moveTo(xFrom, y + height);

        boolean up = true;
        for (double x = xFrom; x < xTo; x += period / 2) {
            double next = Math.min(x + period / 2, xTo);
            path.lineTo(next, up ? y : y + height);
            up = !up;
        }

        painter.save();
        painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
        painter.setBrush(new QBrush(Qt.BrushStyle.NoBrush));
        painter.setPen(new QPen(color, 1.0));
        painter.drawPath(path);
        painter.restore();
    }

    private void paintBox(QPainter painter, double xFrom, double xTo, int baseline, QColor color) {
        DesktopQtEditorFontMetrics metrics = myEditor.getFontMetrics();

        painter.save();
        painter.setBrush(new QBrush(Qt.BrushStyle.NoBrush));
        painter.setPen(new QPen(color, 1.0));
        painter.drawRect(new QRectF(xFrom, baseline - metrics.getAscent(), xTo - xFrom - 1, metrics.getLineHeight() - 1));
        painter.restore();
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
        @Nullable String placeholder,
        @Nullable ColorValue effectColor,
        @Nullable EffectType effectType
    ) {
        CharSequence text(CharSequence documentText) {
            return placeholder != null ? placeholder : documentText.subSequence(startOffset, endOffset);
        }

        Run slice(int from, int to) {
            return new Run(from, to, foreground, background, fontType, null, effectColor, effectType);
        }
    }

    /**
     * The runs of a whole visual line. Folding is not unpicked here: {@link IterationState} is asked to honour
     * fold regions and then walks them itself, off the fold tree's own cached top level array, reporting each
     * collapsed region as a single segment whose merged attributes already carry the placeholder styling - and
     * the selection or caret row over it, which attributes read straight off the scheme would have lost.
     */
    /**
     * One piece of a row as it is laid out left to right: either a run of the text, or a hint standing between
     * two characters of it. The three passes over a row all walk the same cells, so a hint cannot advance the
     * glyphs by one amount and the backgrounds by another.
     */
    private record Cell(@Nullable Run run, DesktopQtEditorInlays.@Nullable Rendered inlay) {
        double width(DesktopQtEditorFontMetrics metrics, CharSequence documentText) {
            return run == null
                ? Objects.requireNonNull(inlay).width()
                : metrics.getTextWidth(run.text(documentText), run.fontType());
        }
    }

    /**
     * The row cut into the pieces it is drawn from. A hint sits between two characters, so a run met across one
     * is split at it - otherwise the hint would be drawn over the glyphs rather than between them.
     */
    private List<Cell> collectCells(int visualLine, CaretData caretData) {
        DesktopQtEditorVisualLines visualLines = myEditor.getVisualLines();

        int lineStart = visualLines.visualLineStartOffset(visualLine);
        int lineEnd = visualLines.visualLineEndOffset(visualLine);

        List<Run> runs = collectRuns(lineStart, lineEnd, caretData);

        List<DesktopQtEditorInlays.Rendered> inlines = myEditor.getInlays().inlineIn(lineStart, lineEnd);
        List<DesktopQtEditorInlays.Rendered> afterEnd = myEditor.getInlays().afterLineEndIn(lineStart, lineEnd);

        List<Cell> cells = new ArrayList<>(runs.size() + inlines.size() + afterEnd.size());

        int next = 0;

        for (Run run : runs) {
            int from = run.startOffset();

            // a placeholder stands for a whole collapsed region and cannot be cut, so hints inside it are skipped
            while (next < inlines.size() && inlines.get(next).inlay().getOffset() <= from) {
                cells.add(new Cell(null, inlines.get(next++)));
            }

            if (run.placeholder() != null) {
                cells.add(new Cell(run, null));
                continue;
            }

            int cut = from;

            while (next < inlines.size() && inlines.get(next).inlay().getOffset() < run.endOffset()) {
                int offset = inlines.get(next).inlay().getOffset();

                if (offset > cut) {
                    cells.add(new Cell(run.slice(cut, offset), null));
                    cut = offset;
                }

                cells.add(new Cell(null, inlines.get(next++)));
            }

            if (cut < run.endOffset()) {
                cells.add(new Cell(run.slice(cut, run.endOffset()), null));
            }
        }

        while (next < inlines.size()) {
            cells.add(new Cell(null, inlines.get(next++)));
        }

        for (DesktopQtEditorInlays.Rendered rendered : afterEnd) {
            cells.add(new Cell(null, rendered));
        }

        return cells;
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
            ColorValue effectColor = attributes.getEffectColor();
            EffectType effectType = effectColor == null ? null : attributes.getEffectType();

            FoldRegion fold = state.getCurrentFold();
            if (fold != null) {
                runs.add(new Run(
                    state.getStartOffset(),
                    state.getEndOffset(),
                    foreground,
                    background,
                    fontType,
                    fold.getPlaceholderText(),
                    effectColor,
                    effectType
                ));
                continue;
            }

            Run last = runs.isEmpty() ? null : runs.get(runs.size() - 1);

            if (last != null && last.placeholder() == null
                && last.endOffset() == state.getStartOffset()
                && last.fontType() == fontType
                && Objects.equals(last.foreground(), foreground)
                && Objects.equals(last.background(), background)
                && Objects.equals(last.effectColor(), effectColor)
                && last.effectType() == effectType) {
                runs.set(
                    runs.size() - 1,
                    new Run(last.startOffset(), state.getEndOffset(), foreground, background, fontType, null, effectColor, effectType)
                );
            }
            else {
                runs.add(new Run(
                    state.getStartOffset(), state.getEndOffset(), foreground, background, fontType, null, effectColor, effectType
                ));
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
