// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.codeEditor.impl;

import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.markup.LineFrameEdge;
import consulo.codeEditor.markup.LineFramePresentation;
import consulo.codeEditor.markup.LineFrameResize;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.document.Document;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.cursor.StandardCursors;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.function.Predicate;

/**
 * The drag of an edge of a {@link LineFramePresentation} which carries a {@link LineFrameResize}, for a frontend which
 * hands the pointer over in editor coordinates - the space of {@link Editor#visualLineToY(int)}. The gutter and the
 * text share that y, so both feed the same gesture.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public final class LineFrameResizeGesture {
    private static final int OUTLINE_OUTSIDE_DETECTION_MARGIN = 3;
    private static final float OUTLINE_DETECTION_LINE_FRACTION = 0.3f;

    private final CodeEditorBase myEditor;

    private @Nullable LineFrameResize myDragged;
    private int myDragY;
    private int myDragLine = -1;
    private boolean myResizeCursor;

    public LineFrameResizeGesture(CodeEditorBase editor) {
        myEditor = editor;
    }

    public boolean isDragging() {
        return myDragged != null;
    }

    /**
     * The pointer moved with no button down. Answers the edge it is over, so the frontend can show its tooltip, and
     * puts the resize pointer on while the edge can be dragged.
     */
    @RequiredUIAccess
    public @Nullable LineFrameEdgeHit moved(int y) {
        if (myDragged != null) {
            return null;
        }
        LineFrameEdgeHit hit = edgeAt(y);
        setCursor(hit != null && hit.resize().enabled());
        return hit;
    }

    /**
     * The pointer left the editor.
     */
    @RequiredUIAccess
    public void exited() {
        if (myDragged == null) {
            setCursor(false);
        }
    }

    /**
     * A press of the main button. Starts a drag when it lands on an edge which can be dragged.
     *
     * @return whether the press was taken - the editor must then neither move the caret nor start a selection
     */
    @RequiredUIAccess
    public boolean pressed(int y) {
        LineFrameEdgeHit hit = edgeAt(y);
        if (hit == null || !hit.resize().enabled()) {
            return false;
        }
        myDragged = hit.resize();
        myDragY = y;
        myDragLine = -1;
        setCursor(true);
        hit.resize().listener().dragStarted(hit.edge());
        return true;
    }

    /**
     * The pointer moved with the button down.
     *
     * @return whether a drag of an edge is in progress, which the editor must not take as a selection
     */
    @RequiredUIAccess
    public boolean dragged(int y) {
        LineFrameResize dragged = myDragged;
        if (dragged == null) {
            return false;
        }
        myDragY = y;
        int line = lineAt(y);
        if (line != myDragLine) {
            myDragLine = line;
            dragged.listener().dragged(line);
        }
        return true;
    }

    /**
     * The editor scrolled under a pointer which stayed where it was, so it is over another line now.
     */
    @RequiredUIAccess
    public void scrolled(int deltaY) {
        if (myDragged != null && deltaY != 0) {
            dragged(myDragY + deltaY);
        }
    }

    /**
     * @return whether the release ended a drag of an edge
     */
    @RequiredUIAccess
    public boolean released() {
        LineFrameResize dragged = myDragged;
        if (dragged == null) {
            return false;
        }
        myDragged = null;
        myDragLine = -1;
        setCursor(false);
        dragged.listener().dragFinished();
        return true;
    }

    /**
     * The edge of a resizable frame under {@code y}, if any.
     */
    @RequiredUIAccess
    public @Nullable LineFrameEdgeHit edgeAt(int y) {
        Document document = myEditor.getDocument();
        int lineHeight = myEditor.getLineHeight();
        int start = document.getLineStartOffset(lineAt(Math.max(0, y - lineHeight)));
        int end = document.getLineEndOffset(lineAt(y + lineHeight));

        LineFrameEdgeHit[] found = new LineFrameEdgeHit[1];
        Predicate<RangeHighlighterEx> processor = highlighter -> {
            LineFramePresentation presentation = highlighter.getLineFramePresentation();
            LineFrameResize resize = presentation == null ? null : presentation.resize();
            if (resize == null || !highlighter.isValid()) {
                return true;
            }
            LineFrameEdge edge = getEdgeAt(document.getLineNumber(highlighter.getStartOffset()),
                document.getLineNumber(highlighter.getEndOffset()),
                y);
            if (edge == null) {
                return true;
            }
            found[0] = new LineFrameEdgeHit(resize, edge);
            return false;
        };
        if (myEditor.getMarkupModel().processRangeHighlightersOverlappingWith(start, end, processor)) {
            myEditor.getFilteredDocumentMarkupModel().processRangeHighlightersOverlappingWith(start, end, processor);
        }
        return found[0];
    }

    private @Nullable LineFrameEdge getEdgeAt(int startLine, int endLine, int y) {
        int[] yBorders = yRangeForLogicalLineRange(startLine, endLine);
        float topY = yBorders[0];
        float botY = yBorders[1];
        float lineHeight = myEditor.getLineHeight();

        if (y >= Math.max(topY - OUTLINE_OUTSIDE_DETECTION_MARGIN, 0f) && y <= topY + lineHeight * OUTLINE_DETECTION_LINE_FRACTION) {
            return LineFrameEdge.TOP;
        }
        if (y >= botY - lineHeight * OUTLINE_DETECTION_LINE_FRACTION && y <= botY + OUTLINE_OUTSIDE_DETECTION_MARGIN) {
            return LineFrameEdge.BOTTOM;
        }
        return null;
    }

    private int[] yRangeForLogicalLineRange(int startLine, int endLine) {
        Document document = myEditor.getDocument();
        int maxLine = Math.max(document.getLineCount() - 1, 0);
        int startOffset = document.getLineStartOffset(Math.clamp(startLine, 0, maxLine));
        int endOffset = document.getLineEndOffset(Math.clamp(endLine, 0, maxLine));

        int startY = myEditor.offsetToXY(startOffset).y;

        FoldRegion foldRegion = myEditor.getFoldingModel().getCollapsedRegionAtOffset(endOffset - 1);
        Point location = foldRegion instanceof CustomFoldRegion custom ? custom.getLocation() : null;
        int endY = location != null
            ? location.y + ((CustomFoldRegion) foldRegion).getHeightInPixels()
            : myEditor.offsetToXY(endOffset).y + myEditor.getLineHeight();

        return new int[]{startY, endY};
    }

    private int lineAt(int y) {
        int line = myEditor.xyToLogicalPosition(new Point(0, y)).line;
        return Math.clamp(line, 0, Math.max(myEditor.getDocument().getLineCount(), 1) - 1);
    }

    @RequiredUIAccess
    private void setCursor(boolean resize) {
        if (myResizeCursor != resize) {
            myResizeCursor = resize;
            myEditor.setCustomCursor(this, resize ? StandardCursors.VERTICAL_RESIZE : null);
        }
    }
}
