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
import consulo.codeEditor.FoldingModelEx;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.ui.color.ColorValue;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.desktop.qt.ui.impl.DesktopQtInputDetails;
import io.qt.core.QPointF;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPolygon;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.widgets.QWidget;

import org.jspecify.annotations.Nullable;

import java.awt.Font;
import java.util.HashSet;
import java.util.Set;

/**
 * The line number strip to the left of the text.
 * <p>
 * A sibling of the viewport rather than something drawn inside it: the room it needs is taken out of the scroll
 * area with {@code setViewportMargins}, so the text never scrolls under it horizontally while both still scroll
 * together vertically. That is the arrangement qt's own editor example uses, and it keeps the gutter out of the
 * painter's clip arithmetic entirely.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorGutterWidget extends QWidget {
    private static final int LEFT_PADDING = 6;
    private static final int RIGHT_PADDING = 8;

    /**
     * The strip between the numbers and the text. It carries the separator rule and is painted in the editor
     * background rather than the gutter one, so the gutter stops short of the text - the same three pixels the
     * awt gutter reserves in {@code getWhitespaceSeparatorOffset}.
     */
    private static final int SEPARATOR_AREA_WIDTH = 3;

    /**
     * Room for the fold anchors and for whatever hangs an icon in the gutter - a breakpoint above all. The awt
     * gutter sizes this from the widest icon it holds; a fixed square is enough while only anchors are drawn.
     */
    private static final int MARKER_AREA_WIDTH = 14;

    private static final int ANCHOR_SIZE = 8;

    private final DesktopQtEditorWidget mySurface;
    private final DesktopQtEditorImpl myEditor;

    public DesktopQtEditorGutterWidget(DesktopQtEditorWidget surface, DesktopQtEditorImpl editor) {
        super(surface);
        mySurface = surface;
        myEditor = editor;
    }

    /**
     * The room the strip needs, which is what the surface takes out of its viewport. Zero hides it entirely,
     * since a gutter of no width still paints its background over the first pixels of the text.
     */
    public int preferredWidth() {
        if (!myEditor.getSettings().isLineNumbersShown()) {
            return 0;
        }

        int lastLine = Math.max(1, myEditor.getDocument().getLineCount());
        String widest = "0".repeat(String.valueOf(lastLine).length());

        return LEFT_PADDING
            + (int) Math.ceil(myEditor.getFontMetrics().getTextWidth(widest))
            + RIGHT_PADDING
            + MARKER_AREA_WIDTH
            + SEPARATOR_AREA_WIDTH;
    }

    /**
     * Where the numbers stop and the markers begin. Everything right of this belongs to the line markers as far
     * as the platform is concerned, which is the area a breakpoint is toggled in.
     */
    public int markerAreaOffset() {
        return Math.max(0, separatorOffset() - MARKER_AREA_WIDTH);
    }

    /**
     * Where the gutter stops and the strip belonging to the editor begins. The awt gutter answers the same
     * question for hit testing and for the free painters, so the platform can ask this one too.
     */
    public int separatorOffset() {
        return Math.max(0, width() - SEPARATOR_AREA_WIDTH);
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            paint(painter, event);
        }
        finally {
            painter.end();
        }
    }

    private void paint(QPainter painter, QPaintEvent event) {
        EditorColorsScheme scheme = myEditor.getColorsScheme();
        Document document = myEditor.getDocument();
        DesktopQtEditorFontMetrics metrics = myEditor.getFontMetrics();

        int separatorX = separatorOffset();

        // the gutter owns everything left of the separator; the strip right of it belongs to the text, so the
        // two backgrounds meet at the rule rather than the gutter running under the first pixels of the line
        ColorValue background = scheme.getColor(EditorColors.EDITOR_GUTTER_BACKGROUND);
        painter.fillRect(0, 0, separatorX, height(), TargetQt.to(background == null ? scheme.getDefaultBackground() : background));
        painter.fillRect(separatorX, 0, width() - separatorX, height(), TargetQt.to(scheme.getDefaultBackground()));

        ColorValue separatorColor = scheme.getColor(EditorColors.INDENT_GUIDE_COLOR);
        if (separatorColor != null) {
            painter.fillRect(separatorX, 0, 1, height(), TargetQt.to(separatorColor));
        }

        if (!myEditor.getSettings().isLineNumbersShown()) {
            return;
        }

        int lineHeight = metrics.getLineHeight();
        int scrollY = mySurface.verticalScrollBar().value();

        DesktopQtEditorVisualLines visualLines = myEditor.getVisualLines();

        int firstLine = Math.max(0, (event.rect().top() + scrollY) / lineHeight);
        int lastLine = Math.min(visualLines.getVisualLineCount() - 1, (event.rect().bottom() + scrollY) / lineHeight);

        if (lastLine < firstLine) {
            return;
        }

        ColorValue numberColor = scheme.getColor(EditorColors.LINE_NUMBERS_COLOR);
        ColorValue caretRowNumberColor = scheme.getColor(EditorColors.LINE_NUMBER_ON_CARET_ROW_COLOR);

        QColor plain = TargetQt.to(numberColor == null ? scheme.getDefaultForeground() : numberColor);
        QColor onCaretRow = caretRowNumberColor == null ? plain : TargetQt.to(caretRowNumberColor);

        Set<Integer> caretRows = caretRows();

        painter.setFont(metrics.getFont(Font.PLAIN));

        int right = markerAreaOffset() - RIGHT_PADDING;
        int baselineOffset = metrics.getAscent();

        for (int line = firstLine; line <= lastLine; line++) {
            // rows are numbered by the line of the file they show, so a collapsed region makes the numbers jump
            // rather than run on - and the strip counts from one while the editor counts from zero
            String number = String.valueOf(visualLines.visualToLogicalLine(line) + 1);
            double x = right - metrics.getTextWidth(number);

            painter.setPen(caretRows.contains(line) ? onCaretRow : plain);
            painter.drawText(new QPointF(x, line * lineHeight - scrollY + baselineOffset), number);
        }

        paintFoldAnchors(painter, firstLine, lastLine, scrollY, lineHeight, plain);
    }

    /**
     * A triangle per foldable row, pointing down when the region is open and right when it is collapsed - the
     * same shorthand every editor uses, and the only way to fold with the mouse.
     */
    private void paintFoldAnchors(QPainter painter, int firstLine, int lastLine, int scrollY, int lineHeight, QColor color) {
        painter.setBrush(new QBrush(color));
        painter.setPen(color);
        painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);

        int centerX = markerAreaOffset() + MARKER_AREA_WIDTH / 2;

        for (int line = firstLine; line <= lastLine; line++) {
            FoldRegion region = foldRegionAt(line);
            if (region == null) {
                continue;
            }

            int centerY = line * lineHeight - scrollY + lineHeight / 2;
            int half = ANCHOR_SIZE / 2;

            QPolygon triangle = region.isExpanded()
                ? new QPolygon(centerX - half, centerY - half / 2, centerX + half, centerY - half / 2, centerX, centerY + half)
                : new QPolygon(centerX - half / 2, centerY - half, centerX + half, centerY, centerX - half / 2, centerY + half);

            painter.drawPolygon(triangle);
        }

        painter.setRenderHint(QPainter.RenderHint.Antialiasing, false);
    }

    /**
     * The region that starts on the given row, or null when nothing on it can be folded. Only the row a region
     * begins on carries an anchor, which is what makes a collapsed region show exactly one.
     */
    private @Nullable FoldRegion foldRegionAt(int visualLine) {
        Document document = myEditor.getDocument();

        FoldRegion[] regions = ((FoldingModelEx) myEditor.getFoldingModel()).fetchVisible();
        if (regions == null) {
            return null;
        }

        int logicalLine = myEditor.getVisualLines().visualToLogicalLine(visualLine);

        for (FoldRegion region : regions) {
            if (region.isValid() && document.getLineNumber(region.getStartOffset()) == logicalLine) {
                return region;
            }
        }

        return null;
    }

    /**
     * Clicks are handed to the platform as editor mouse events naming the area they landed in, which is how
     * anything that lives in the gutter hears about them - a breakpoint is toggled by the debugger listening for
     * a click in the line marker area, not by the gutter knowing what a breakpoint is.
     */
    @Override
    protected void mousePressEvent(QMouseEvent event) {
        int x = event.pos().x();
        int visualLine = (event.pos().y() + mySurface.verticalScrollBar().value()) / myEditor.getLineHeight();

        // an anchor takes the click for itself, since folding is the gutter's own business
        if (x >= markerAreaOffset() && event.button() == Qt.MouseButton.LeftButton) {
            FoldRegion region = foldRegionAt(visualLine);
            if (region != null) {
                myEditor.getFoldingModel().runBatchFoldingOperation(() -> region.setExpanded(!region.isExpanded()));

                event.accept();
                return;
            }
        }

        fireEditorMouseEvent(event, x < markerAreaOffset()
            ? EditorMouseEventArea.LINE_NUMBERS_AREA
            : EditorMouseEventArea.LINE_MARKERS_AREA);

        event.accept();
    }

    private void fireEditorMouseEvent(QMouseEvent event, EditorMouseEventArea area) {
        EditorMouseEvent editorEvent = new EditorMouseEvent(
            myEditor,
            DesktopQtInputDetails.mouse(this, event),
            event.button() == Qt.MouseButton.RightButton,
            area
        );

        for (EditorMouseListener listener : myEditor.getEditorMouseListeners()) {
            listener.mousePressed(editorEvent);
        }
    }

    private Set<Integer> caretRows() {
        Set<Integer> lines = new HashSet<>();
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            lines.add(caret.getVisualPosition().line);
        }
        return lines;
    }
}
