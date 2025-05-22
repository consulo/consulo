// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.stickyLine;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.internal.stickyLine.StickyLine;
import consulo.codeEditor.internal.stickyLine.StickyLinesModel;
import consulo.codeEditor.internal.stickyLine.VisualStickyLine;
import consulo.document.util.DocumentUtil;

import java.awt.*;
import java.util.List;
import java.util.*;

public class VisualStickyLines {
    private final Editor editor;
    private final StickyLinesModel stickyModel;
    private final int scopeMinSize;

    private final List<StickyLine> logicalStickyLines = new ArrayList<>();
    private final List<VisualStickyLine> visualStickyLines = new ArrayList<>();
    private final List<VisualStickyLine> visualWithYStickyLines = new ArrayList<>();

    private int totalHeight = 0;
    private int lineHeight = 0;
    private int lineLimit = 0;

    public VisualStickyLines(Editor editor, StickyLinesModel stickyModel, int scopeMinSize) {
        if (scopeMinSize < 2) throw new IllegalArgumentException();
        this.editor = editor;
        this.stickyModel = stickyModel;
        this.scopeMinSize = scopeMinSize;
    }

    public VisualStickyLines(Editor editor, StickyLinesModel stickyModel) {
        this(editor, stickyModel, 5);
    }

    public void recalculate(Rectangle visibleArea) {
        clear();
        recalculate(visibleArea, logicalStickyLines, visualStickyLines);
    }

    public List<VisualStickyLine> lines(Rectangle visibleArea) {
        visualWithYStickyLines.clear();
        totalHeight = setYLocation(visibleArea, visualStickyLines, visualWithYStickyLines);
        return visualWithYStickyLines;
    }

    public int height() {
        return totalHeight;
    }

    public void clear() {
        logicalStickyLines.clear();
        visualStickyLines.clear();
        visualWithYStickyLines.clear();
        totalHeight = 0;
    }

    private void recalculate(Rectangle visibleArea,
                             List<StickyLine> logicalLines,
                             List<VisualStickyLine> visualLines) {
        this.lineLimit = editor.getSettings().getStickyLinesLimit();
        this.lineHeight = editor.getLineHeight();
        collectLogical(visibleArea, logicalLines);
        logicalToVisualLines(logicalLines, visualLines);
    }

    private void collectLogical(Rectangle visibleArea, List<StickyLine> logicalStickyLines) {
        int maxStickyPanelHeight = lineHeight * lineLimit + 1;
        int yStart = visibleArea.y;
        int yEnd = yStart + maxStickyPanelHeight;

        int startLine = editor.xyToLogicalPosition(new Point(0, yStart)).line;
        int endLine = editor.xyToLogicalPosition(new Point(0, yEnd)).line;

        if (!DocumentUtil.isValidLine(endLine, editor.getDocument())) {
            if (!DocumentUtil.isValidLine(startLine, editor.getDocument())) return;
            endLine = Math.max(editor.getDocument().getLineCount() - 1, 0);
        }

        int startOffset = editor.getDocument().getLineStartOffset(startLine);
        int endOffset = editor.getDocument().getLineEndOffset(endLine);

        stickyModel.processStickyLines(startOffset, endOffset, stickyLine -> {
            logicalStickyLines.add(stickyLine);
            return true;
        });
    }

    private void logicalToVisualLines(List<StickyLine> logicalLines, List<VisualStickyLine> visualLines) {
        if (logicalLines.isEmpty()) return;

        Set<Integer> deduplicatedLines = new HashSet<>();
        for (StickyLine logicalLine : logicalLines) {
            int primaryVisual = toVisualLine(logicalLine.primaryLine());
            if (deduplicatedLines.add(primaryVisual)) {
                int scopeVisual = toVisualLine(logicalLine.scopeLine());
                if (isScopeNotNarrow(primaryVisual, scopeVisual)) {
                    visualLines.add(new VisualStickyLine(logicalLine, primaryVisual, scopeVisual));
                }
            }
        }
        Collections.sort(visualLines);
    }

    private int setYLocation(Rectangle visibleArea,
                             List<VisualStickyLine> visualLines,
                             List<VisualStickyLine> withYLocation) {
        if (visualLines.isEmpty()) return 0;

        int totalPanelHeight = 0;
        int editorY = visibleArea.y;
        int editorH = visibleArea.height;

        if (isPanelTooBig(lineHeight, totalPanelHeight, editorH)) return 0;

        for (VisualStickyLine line : visualLines) {
            int startY1 = editor.visualLineToY(line.primaryLine());
            int startY2 = startY1 + lineHeight;
            int endY1 = editor.visualLineToY(line.scopeLine());
            int endY2 = endY1 + lineHeight;
            int stickyY = editorY + totalPanelHeight + lineHeight;

            if (startY2 < stickyY && stickyY <= endY2) {
                int yOverlap = stickyY <= endY1 ? 0 : stickyY - endY1;
                if (yOverlap < 0) {
                    throw new AssertionError(String.format("startY1: %d, startY2: %d, endY1: %d, endY2: %d, stickyY: %d",
                        startY1, startY2, endY1, endY2, stickyY));
                }

                line.setYLocation(totalPanelHeight - yOverlap);
                totalPanelHeight += lineHeight - yOverlap;

                if (lineHeight > yOverlap) {
                    withYLocation.add(line);
                }

                if (yOverlap > 0 ||
                    withYLocation.size() >= lineLimit ||
                    isPanelTooBig(lineHeight, totalPanelHeight, editorH)) {
                    break;
                }
            }
        }

        return totalPanelHeight;
    }

    private int toVisualLine(int logicalLine) {
        return editor.logicalToVisualPosition(new LogicalPosition(logicalLine, 0)).line;
    }

    private boolean isPanelTooBig(int lineHeight, int panelHeight, int editorHeight) {
        return panelHeight + 2 * lineHeight > editorHeight / 2;
    }

    private boolean isScopeNotNarrow(int primaryVisual, int scopeVisual) {
        return scopeVisual - primaryVisual + 1 >= scopeMinSize;
    }
}
