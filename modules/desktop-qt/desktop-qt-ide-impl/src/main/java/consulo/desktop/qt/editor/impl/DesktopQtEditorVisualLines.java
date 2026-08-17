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

import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModelEx;
import consulo.document.Document;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What a line looks like once collapsed regions are taken out of it - the layer that makes a visual line a
 * different thing from a logical one.
 * <p>
 * A collapsed region hides everything between its ends and shows a placeholder instead, so the lines it spans
 * become one visual line and the text of that line is no longer a plain slice of the document. Both the painter
 * and the coordinate mapper have to agree about that, exactly, or the caret lands somewhere the glyphs are not -
 * so they share this rather than each working it out.
 * <p>
 * Inlays and soft wraps belong here too when their turn comes; they change the same mapping in the same way.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorVisualLines {
    /**
     * A stretch of a visual line drawn in one piece: either a slice of the document, or the placeholder standing
     * in for a collapsed region.
     *
     * @param fold the region this stands for, null for ordinary text
     */
    public record Segment(int startOffset, int endOffset, @Nullable FoldRegion fold) {
        public boolean isFold() {
            return fold != null;
        }

        public CharSequence text(CharSequence documentText) {
            return fold != null ? fold.getPlaceholderText() : documentText.subSequence(startOffset, endOffset);
        }
    }

    private final DesktopQtEditorImpl myEditor;

    public DesktopQtEditorVisualLines(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    /**
     * How many rows the document occupies once folding is applied.
     */
    public int getVisualLineCount() {
        Document document = myEditor.getDocument();

        return logicalToVisualLine(document.getLineCount() - 1) + 1;
    }

    public int logicalToVisualLine(int logicalLine) {
        Document document = myEditor.getDocument();

        int line = visibleLineOf(Math.max(0, Math.min(logicalLine, document.getLineCount() - 1)));

        return line - myEditor.getFoldingModel().getFoldedLinesCountBefore(document.getLineStartOffset(line));
    }

    private int visibleLineOf(int logicalLine) {
        Document document = myEditor.getDocument();

        int lineStart = document.getLineStartOffset(logicalLine);

        FoldRegion region = myEditor.getFoldingModel().getCollapsedRegionAtOffset(lineStart);
        if (region == null || region.getStartOffset() >= lineStart) {
            return logicalLine;
        }

        return document.getLineNumber(region.getStartOffset());
    }

    /**
     * The first logical line shown on a visual line. Every line a collapsed region swallowed answers the visual
     * line of the region's own first line, so the search takes the earliest logical line that gets there.
     */
    public int visualToLogicalLine(int visualLine) {
        Document document = myEditor.getDocument();

        int lastLine = document.getLineCount() - 1;
        if (visualLine <= 0) {
            return 0;
        }
        if (visualLine >= getVisualLineCount()) {
            return lastLine;
        }

        int low = 0;
        int high = lastLine;

        while (low < high) {
            int middle = (low + high) >>> 1;

            if (logicalToVisualLine(middle) < visualLine) {
                low = middle + 1;
            }
            else {
                high = middle;
            }
        }

        return low;
    }

    public int visualLineStartOffset(int visualLine) {
        Document document = myEditor.getDocument();

        return document.getLineStartOffset(visualToLogicalLine(visualLine));
    }

    /**
     * Where the row ends in the document. A row covers every logical line from its own up to the one before the
     * next row starts, so this needs no walk over the regions in between - the collapsed ones are exactly what
     * makes that span longer than one line.
     */
    public int visualLineEndOffset(int visualLine) {
        Document document = myEditor.getDocument();

        int lastLine = document.getLineCount() - 1;
        int endLine = visualLine + 1 >= getVisualLineCount() ? lastLine : visualToLogicalLine(visualLine + 1) - 1;

        return document.getLineEndOffset(Math.max(0, Math.min(endLine, lastLine)));
    }

    /**
     * The pieces a visual line is drawn from, left to right. Walking the document from the start of the line, a
     * collapsed region is replaced by one placeholder segment and the walk resumes after it - which is what
     * pulls the lines the region spans onto this one.
     */
    public List<Segment> getSegments(int visualLine) {
        Document document = myEditor.getDocument();

        List<Segment> segments = new ArrayList<>();

        int offset = visualLineStartOffset(visualLine);
        int textLength = document.getTextLength();

        while (true) {
            int lineEnd = document.getLineEndOffset(document.getLineNumber(Math.min(offset, textLength)));

            FoldRegion fold = nextCollapsedRegion(offset, lineEnd);
            if (fold == null) {
                if (offset < lineEnd) {
                    segments.add(new Segment(offset, lineEnd, null));
                }
                return segments;
            }

            if (fold.getStartOffset() > offset) {
                segments.add(new Segment(offset, fold.getStartOffset(), null));
            }

            segments.add(new Segment(fold.getStartOffset(), fold.getEndOffset(), fold));

            offset = fold.getEndOffset();
        }
    }

    /**
     * The first collapsed region starting inside {@code [offset, lineEnd]}, or null when the rest of the line is
     * plain text. The bound is inclusive because a region may begin exactly at the line end and still fold the
     * break that follows it.
     * <p>
     * Read off the fold tree's cached array of collapsed top level regions, entered at the index the tree itself
     * reports for the offset, so this costs a step or two rather than a pass over every region in the file.
     */
    private @Nullable FoldRegion nextCollapsedRegion(int offset, int lineEnd) {
        FoldingModelEx foldingModel = (FoldingModelEx) myEditor.getFoldingModel();

        FoldRegion[] topLevel = foldingModel.fetchTopLevel();
        if (topLevel == null) {
            return null;
        }

        // the tree answers with the index of the last region before the offset, so the search starts after it
        int index = Math.max(0, foldingModel.getLastCollapsedRegionBefore(offset));

        for (int i = index; i < topLevel.length; i++) {
            FoldRegion region = topLevel[i];

            int start = region.getStartOffset();
            if (start > lineEnd) {
                return null;
            }

            if (start >= offset && region.isValid() && !region.isExpanded() && region.getEndOffset() > start) {
                return region;
            }
        }

        return null;
    }
}
