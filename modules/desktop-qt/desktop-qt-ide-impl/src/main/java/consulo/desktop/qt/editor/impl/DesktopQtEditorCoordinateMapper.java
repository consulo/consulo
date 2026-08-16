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

import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.VisualPosition;
import consulo.desktop.qt.editor.impl.DesktopQtEditorVisualLines.Segment;
import consulo.document.Document;

import java.awt.Point;

/**
 * Translation between the four coordinate spaces the platform speaks: character offset, logical position,
 * visual position and pixels.
 * <p>
 * The boundary mirrors awt's {@code EditorCoordinateMapper} because plugins compute against these coordinates, but
 * the interior is deliberately simpler than awt's for now: folding, inlays and soft wraps are not consulted, so
 * visual and logical positions coincide and a column is a plain character index. Widening this to the awt
 * semantics is the point of the fragment layer that comes next - callers should not have to change when it lands.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorCoordinateMapper {
    private final DesktopQtEditorImpl myEditor;
    private final DesktopQtEditorVisualLines myVisualLines;

    public DesktopQtEditorCoordinateMapper(DesktopQtEditorImpl editor, DesktopQtEditorVisualLines visualLines) {
        myEditor = editor;
        myVisualLines = visualLines;
    }

    public LogicalPosition offsetToLogicalPosition(int offset) {
        Document document = myEditor.getDocument();

        int clamped = Math.max(0, Math.min(offset, document.getTextLength()));
        int line = document.getLineNumber(clamped);

        return new LogicalPosition(line, clamped - document.getLineStartOffset(line));
    }

    public int logicalPositionToOffset(LogicalPosition position) {
        Document document = myEditor.getDocument();

        int line = Math.max(0, Math.min(position.line, document.getLineCount() - 1));
        int lineStart = document.getLineStartOffset(line);
        int lineEnd = document.getLineEndOffset(line);

        return Math.min(lineStart + Math.max(0, position.column), lineEnd);
    }

    /**
     * A logical position sits on the visual line of whatever is actually shown for it: a position inside a
     * collapsed region is not on screen at all, so it answers the line the placeholder is on.
     */
    public VisualPosition logicalToVisualPosition(LogicalPosition position) {
        int offset = logicalPositionToOffset(position);
        int visualLine = myVisualLines.logicalToVisualLine(position.line);

        return new VisualPosition(visualLine, offsetToVisualColumn(visualLine, offset));
    }

    public LogicalPosition visualToLogicalPosition(VisualPosition position) {
        return offsetToLogicalPosition(visualPositionToOffset(position));
    }

    public int offsetToVisualLine(int offset) {
        Document document = myEditor.getDocument();

        int clamped = Math.max(0, Math.min(offset, document.getTextLength()));

        return myVisualLines.logicalToVisualLine(document.getLineNumber(clamped));
    }

    public int visualLineStartOffset(int visualLine) {
        Document document = myEditor.getDocument();

        if (visualLine < 0) {
            return 0;
        }
        if (visualLine >= myVisualLines.getVisualLineCount()) {
            return document.getTextLength();
        }
        return myVisualLines.visualLineStartOffset(visualLine);
    }

    /**
     * Columns are counted along what is drawn, so a collapsed region costs the length of its placeholder rather
     * than the length of the text it hides.
     */
    private int offsetToVisualColumn(int visualLine, int offset) {
        int column = 0;

        for (Segment segment : myVisualLines.getSegments(visualLine)) {
            if (offset < segment.endOffset()) {
                // an offset buried inside a collapsed region has no column of its own - the placeholder is one
                // thing, so the caret goes to its near end
                return segment.isFold() ? column : column + (offset - segment.startOffset());
            }

            column += segmentLength(segment);
        }

        return column;
    }

    private int visualPositionToOffset(VisualPosition position) {
        int line = Math.max(0, Math.min(position.line, myVisualLines.getVisualLineCount() - 1));
        int column = 0;

        int offset = myVisualLines.visualLineStartOffset(line);

        for (Segment segment : myVisualLines.getSegments(line)) {
            int length = segmentLength(segment);

            if (position.column < column + length) {
                return segment.isFold() ? segment.startOffset() : segment.startOffset() + (position.column - column);
            }

            column += length;
            offset = segment.endOffset();
        }

        // past the last piece of the line, so the columns beyond it are virtual space after its end
        return offset;
    }

    private int segmentLength(Segment segment) {
        return segment.isFold()
            ? segment.fold().getPlaceholderText().length()
            : segment.endOffset() - segment.startOffset();
    }

    public Point visualPositionToXY(VisualPosition position) {
        int x = (int) Math.round(columnToX(position.line, position.column));

        return new Point(x, position.line * myEditor.getLineHeight());
    }

    public LogicalPosition xyToLogicalPosition(Point p) {
        int lineHeight = myEditor.getLineHeight();
        int visualLine = Math.max(0, Math.min(p.y / lineHeight, myVisualLines.getVisualLineCount() - 1));

        return visualToLogicalPosition(new VisualPosition(visualLine, xToColumn(visualLine, p.x)));
    }

    /**
     * Measured along the pieces the line is drawn from, so a placeholder is as wide as the text of it rather than
     * as wide as what it hides.
     */
    private double columnToX(int visualLine, int column) {
        if (column <= 0 || visualLine < 0 || visualLine >= myVisualLines.getVisualLineCount()) {
            return 0;
        }

        CharSequence text = myEditor.getDocument().getCharsSequence();
        DesktopQtEditorFontMetrics metrics = myEditor.getFontMetrics();

        double x = 0;
        int consumed = 0;

        for (Segment segment : myVisualLines.getSegments(visualLine)) {
            int length = segmentLength(segment);
            CharSequence segmentText = segment.text(text);

            if (column < consumed + length) {
                return x + metrics.getTextWidth(segmentText.subSequence(0, column - consumed));
            }

            x += metrics.getTextWidth(segmentText);
            consumed += length;
        }

        // a caret past the end of the line still has to land somewhere, so the columns beyond it are spaces
        return x + (column - consumed) * metrics.getSpaceWidth();
    }

    private int xToColumn(int visualLine, int x) {
        if (x <= 0) {
            return 0;
        }

        int length = visualLineLength(visualLine);

        // the column whose glyph midpoint the click passed - the caret snaps to the nearer character boundary
        double previous = 0;
        for (int column = 1; column <= length; column++) {
            double current = columnToX(visualLine, column);
            if (x < (previous + current) / 2) {
                return column - 1;
            }
            previous = current;
        }

        return length + (int) Math.round((x - previous) / myEditor.getFontMetrics().getSpaceWidth());
    }

    private int visualLineLength(int visualLine) {
        int length = 0;
        for (Segment segment : myVisualLines.getSegments(visualLine)) {
            length += segmentLength(segment);
        }
        return length;
    }
}
