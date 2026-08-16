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

    public DesktopQtEditorCoordinateMapper(DesktopQtEditorImpl editor) {
        myEditor = editor;
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

    public VisualPosition logicalToVisualPosition(LogicalPosition position) {
        return new VisualPosition(position.line, position.column);
    }

    public LogicalPosition visualToLogicalPosition(VisualPosition position) {
        return new LogicalPosition(position.line, position.column);
    }

    public int offsetToVisualLine(int offset) {
        Document document = myEditor.getDocument();

        return document.getLineNumber(Math.max(0, Math.min(offset, document.getTextLength())));
    }

    public int visualLineStartOffset(int visualLine) {
        Document document = myEditor.getDocument();

        if (visualLine < 0) {
            return 0;
        }
        if (visualLine >= document.getLineCount()) {
            return document.getTextLength();
        }
        return document.getLineStartOffset(visualLine);
    }

    public Point visualPositionToXY(VisualPosition position) {
        int x = (int) Math.round(columnToX(position.line, position.column));

        return new Point(x, position.line * myEditor.getLineHeight());
    }

    public LogicalPosition xyToLogicalPosition(Point p) {
        Document document = myEditor.getDocument();

        int lineHeight = myEditor.getLineHeight();
        int line = Math.max(0, Math.min(p.y / lineHeight, document.getLineCount() - 1));

        return new LogicalPosition(line, xToColumn(line, p.x));
    }

    private double columnToX(int line, int column) {
        Document document = myEditor.getDocument();

        if (column <= 0 || line < 0 || line >= document.getLineCount()) {
            return 0;
        }

        int lineStart = document.getLineStartOffset(line);
        int lineEnd = document.getLineEndOffset(line);
        int end = Math.min(lineStart + column, lineEnd);

        double x = myEditor.getFontMetrics().getTextWidth(document.getCharsSequence().subSequence(lineStart, end));

        // a caret past the line end still has to land somewhere, so the missing columns are billed as spaces
        int overshoot = lineStart + column - end;
        return x + overshoot * myEditor.getFontMetrics().getSpaceWidth();
    }

    private int xToColumn(int line, int x) {
        Document document = myEditor.getDocument();

        int lineStart = document.getLineStartOffset(line);
        int lineLength = document.getLineEndOffset(line) - lineStart;

        if (x <= 0) {
            return 0;
        }

        // the column whose glyph midpoint the click passed - the caret snaps to the nearer character boundary
        double previous = 0;
        for (int column = 1; column <= lineLength; column++) {
            double current = columnToX(line, column);
            if (x < (previous + current) / 2) {
                return column - 1;
            }
            previous = current;
        }

        return lineLength + (int) Math.round((x - previous) / myEditor.getFontMetrics().getSpaceWidth());
    }
}
