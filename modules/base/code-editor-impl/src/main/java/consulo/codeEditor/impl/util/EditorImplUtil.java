/*
 * Copyright 2013-2022 consulo.io
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
package consulo.codeEditor.impl.util;

import consulo.application.ReadAction;
import consulo.codeEditor.*;
import consulo.codeEditor.impl.CodeEditorInlayModelBase;
import consulo.codeEditor.impl.ComplementaryFontsRegistry;
import consulo.codeEditor.impl.FontInfo;
import consulo.codeEditor.impl.internal.RealEditorWithEditorView;
import consulo.codeEditor.impl.internal.VisualLinesIterator;
import consulo.codeEditor.util.EditorUtil;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.document.impl.Interval;
import consulo.document.impl.TextRangeInterval;
import consulo.document.util.DocumentUtil;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static consulo.codeEditor.util.EditorUtil.getTabSize;

/**
 * @author VISTALL
 * @since 20-Mar-22
 */
public class EditorImplUtil {
    private static final Logger LOG = Logger.getInstance(EditorImplUtil.class);

    public static int getNotFoldedLineStartOffset(@Nonnull Document document, @Nonnull FoldingModel foldingModel, int startOffset, boolean stopAtInvisibleFoldRegions) {
        int offset = startOffset;
        while (true) {
            offset = DocumentUtil.getLineStartOffset(offset, document);
            FoldRegion foldRegion = foldingModel.getCollapsedRegionAtOffset(offset - 1);
            if (foldRegion == null ||
                stopAtInvisibleFoldRegions && foldRegion.getPlaceholderText().isEmpty() ||
                foldRegion.getStartOffset() >= offset) {
                break;
            }
            offset = foldRegion.getStartOffset();
        }
        return offset;
    }

    /**
     * Tells whether given inlay element is invisible due to folding of text in editor
     */
    public static boolean isInlayFolded(@Nonnull Inlay inlay) {
        if (CodeEditorInlayModelBase.showWhenFolded(inlay)) {
            return false;
        }
        return ReadAction.compute(() -> {
            Editor editor = inlay.getEditor();
            Inlay.Placement placement = inlay.getPlacement();
            int offset = inlay.getOffset();
            if (placement == Inlay.Placement.AFTER_LINE_END) {
                offset = DocumentUtil.getLineEndOffset(offset, editor.getDocument());
            }
            else if ((placement == Inlay.Placement.ABOVE_LINE || placement == Inlay.Placement.BELOW_LINE) && !inlay.isRelatedToPrecedingText()) {
                offset--;
            }
            FoldingModel foldingModel = editor.getFoldingModel();
            return foldingModel.isOffsetCollapsed(offset) ||
                ((placement == Inlay.Placement.INLINE || placement == Inlay.Placement.AFTER_LINE_END) &&
                    foldingModel.getCollapsedRegionAtOffset(offset - 1) instanceof CustomFoldRegion);
        });
    }

    /**
     * First value returned is the range of {@code y} coordinates in editor coordinate space (relative to
     * {@code editor.getContentComponent()}), corresponding to a given logical line in a document. Most often, a logical line corresponds to a
     * single visual line, in that case the returned range has a height of {@code editor.getLineHeight()} (or a height of fold region
     * placeholder, if the line is collapsed in a {@link CustomFoldRegion}). This will be not the case, if the
     * line is soft-wrapped. Then the vertical range will be larger, as it will include several visual lines. Block inlays displayed on
     * either side of the calculated range, are not included in the result.
     * <p>
     * The second value is a sub-range no other logical line maps to (or {@code null} if there's no such sub-range).
     *
     * @return EXCLUSIVE intervals [startY, endY)
     * @see #yToLogicalLineRange(Editor, int)
     */
    @Nonnull
    public static Pair<Interval, Interval> logicalLineToYRange(@Nonnull Editor editor, int logicalLine) {
        if (logicalLine < 0) {
            throw new IllegalArgumentException("Logical line is negative: " + logicalLine);
        }
        Document document = editor.getDocument();
        int startVisualLine;
        int endVisualLine;
        boolean topOverlapped;
        boolean bottomOverlapped;
        if (logicalLine >= document.getLineCount()) {
            startVisualLine = endVisualLine = logicalToVisualLine(editor, logicalLine);
            topOverlapped = bottomOverlapped = false;
        }
        else {
            int lineStartOffset = document.getLineStartOffset(logicalLine);
            int lineEndOffset = document.getLineEndOffset(logicalLine);
            FoldRegion foldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(lineStartOffset);
            if (foldRegion instanceof CustomFoldRegion) {
                int startY = editor.visualLineToY(((RealEditor) editor).offsetToVisualLine(foldRegion.getStartOffset(), false));
                Interval interval = new TextRangeInterval(startY, startY + ((CustomFoldRegion) foldRegion).getHeightInPixels());
                return Pair.create(interval, foldRegion.getStartOffset() == document.getLineStartOffset(logicalLine) &&
                    foldRegion.getEndOffset() == document.getLineEndOffset(logicalLine) ? interval : null);
            }
            startVisualLine = ((RealEditor) editor).offsetToVisualLine(lineStartOffset, false);
            endVisualLine = startVisualLine + editor.getSoftWrapModel().getSoftWrapsForRange(lineStartOffset + 1, lineEndOffset - 1).size();
            topOverlapped = editor.getFoldingModel().isOffsetCollapsed(lineStartOffset - 1);
            bottomOverlapped = logicalLine + 1 < document.getLineCount() &&
                editor.getFoldingModel().isOffsetCollapsed(document.getLineStartOffset(logicalLine + 1) - 1);
        }
        int lineHeight = editor.getLineHeight();
        int startY = editor.visualLineToY(startVisualLine);
        int endY = (endVisualLine == startVisualLine ? startY : editor.visualLineToY(endVisualLine)) + lineHeight;
        int startYEx = topOverlapped ? startY + lineHeight : startY;
        int endYEx = bottomOverlapped ? endY - lineHeight : endY;
        return Pair.create(new TextRangeInterval(startY, endY), startYEx < endYEx ? new TextRangeInterval(startYEx, endYEx) : null);
    }

    public static int logicalToVisualLine(@Nonnull Editor editor, int logicalLine) {
        LogicalPosition logicalPosition = new LogicalPosition(logicalLine, 0);
        VisualPosition visualPosition = editor.logicalToVisualPosition(logicalPosition);
        return visualPosition.line;
    }

    public static int getLastVisualLineColumnNumber(@Nonnull Editor editor, final int line) {
        if (editor instanceof RealEditorWithEditorView editorImpl) {
            int lineEndOffset = line >= editorImpl.getVisibleLineCount()
                ? editor.getDocument().getTextLength()
                : new VisualLinesIterator(editorImpl, line).getVisualLineEndOffset();

            return editor.offsetToVisualPosition(lineEndOffset, true, true).column;
        }

        return ReadAction.compute(() -> {
            Document document = editor.getDocument();
            int lastLine = document.getLineCount() - 1;
            if (lastLine < 0) {
                return 0;
            }

            // Filter all lines that are not shown because of a collapsed folding region.
            VisualPosition visStart = new VisualPosition(line, 0);
            LogicalPosition logStart = editor.visualToLogicalPosition(visStart);
            int lastLogLine = logStart.line;
            while (lastLogLine < document.getLineCount() - 1) {
                logStart = new LogicalPosition(logStart.line + 1, logStart.column);
                VisualPosition tryVisible = editor.logicalToVisualPosition(logStart);
                if (tryVisible.line != visStart.line) break;
                lastLogLine = logStart.line;
            }

            int resultLogLine = Math.min(lastLogLine, lastLine);
            VisualPosition resVisStart = editor.offsetToVisualPosition(document.getLineStartOffset(resultLogLine));
            VisualPosition resVisEnd = editor.offsetToVisualPosition(document.getLineEndOffset(resultLogLine));

            // Target logical line is not soft wrap affected.
            if (resVisStart.line == resVisEnd.line) {
                return resVisEnd.column;
            }

            int visualLinesToSkip = line - resVisStart.line;
            List<? extends SoftWrap> softWraps = editor.getSoftWrapModel().getSoftWrapsForLine(resultLogLine);
            for (int i = 0; i < softWraps.size(); i++) {
                SoftWrap softWrap = softWraps.get(i);
                CharSequence text = document.getCharsSequence();
                if (visualLinesToSkip <= 0) {
                    VisualPosition visual = editor.offsetToVisualPosition(softWrap.getStart() - 1);
                    int result = visual.column;
                    int x = editor.visualPositionToXY(visual).x;
                    // We need to add the width of the next symbol because the current result column points to the last symbol before the soft wrap.
                    return result + textWidthInColumns(editor, text, softWrap.getStart() - 1, softWrap.getStart(), x);
                }

                int softWrapLineFeeds = StringUtil.countNewLines(softWrap.getText());
                if (softWrapLineFeeds < visualLinesToSkip) {
                    visualLinesToSkip -= softWrapLineFeeds;
                    continue;
                }

                // Target visual column is located on the last visual line of the current soft wrap.
                if (softWrapLineFeeds == visualLinesToSkip) {
                    if (i >= softWraps.size() - 1) {
                        return resVisEnd.column;
                    }
                    // We need to find visual column for line feed of the next soft wrap.
                    SoftWrap nextSoftWrap = softWraps.get(i + 1);
                    VisualPosition visual = editor.offsetToVisualPosition(nextSoftWrap.getStart() - 1);
                    int result = visual.column;
                    int x = editor.visualPositionToXY(visual).x;

                    /* We need to add symbol width because current column points to the last symbol before the next soft wrap; */
                    result += textWidthInColumns(editor, text, nextSoftWrap.getStart() - 1, nextSoftWrap.getStart(), x);

                    int lineFeedIndex = StringUtil.indexOf(nextSoftWrap.getText(), '\n');
                    result += textWidthInColumns(editor, nextSoftWrap.getText(), 0, lineFeedIndex, 0);
                    return result;
                }

                // Target visual column is the one before line feed introduced by the current soft wrap.
                int softWrapStartOffset = 0;
                int softWrapEndOffset = 0;
                int softWrapTextLength = softWrap.getText().length();
                while (visualLinesToSkip-- > 0) {
                    softWrapStartOffset = softWrapEndOffset + 1;
                    if (softWrapStartOffset >= softWrapTextLength) {
                        assert false;
                        return resVisEnd.column;
                    }
                    softWrapEndOffset = StringUtil.indexOf(softWrap.getText(), '\n', softWrapStartOffset, softWrapTextLength);
                    if (softWrapEndOffset < 0) {
                        assert false;
                        return resVisEnd.column;
                    }
                }
                VisualPosition visual = editor.offsetToVisualPosition(softWrap.getStart() - 1);
                int result = visual.column; // Column of the symbol just before the soft wrap
                int x = editor.visualPositionToXY(visual).x;

                // Target visual column is located on the last visual line of the current soft wrap.
                result += textWidthInColumns(editor, text, softWrap.getStart() - 1, softWrap.getStart(), x);
                result += EditorUtil.calcColumnNumber(editor, softWrap.getText(), softWrapStartOffset, softWrapEndOffset);
                return result;
            }

            CharSequence editorInfo = "editor's class: " + editor.getClass()
                + ", all soft wraps: " + editor.getSoftWrapModel().getSoftWrapsForRange(0, document.getTextLength())
                + ", fold regions: " + Arrays.toString(editor.getFoldingModel().getAllFoldRegions());
            LOG.error("Can't calculate last visual column", new Throwable(), AttachmentFactory.get().create("context.txt", String.format(
                "Target visual line: %d, mapped logical line: %d, visual lines range for the mapped logical line: [%s]-[%s], soft wraps for "
                    + "the target logical line: %s. Editor info: %s",
                line, resultLogLine, resVisStart, resVisEnd, softWraps, editorInfo
            )));

            return resVisEnd.column;
        });
    }

    @Deprecated
    public static int calcColumnNumber(@Nonnull Editor editor, @Nonnull CharSequence text, int start, int offset) {
        return EditorUtil.calcColumnNumber(editor, text, start, offset);
    }

    @Deprecated
    public static int calcColumnNumber(@Nullable Editor editor,
                                       @Nonnull CharSequence text,
                                       final int start,
                                       final int offset,
                                       final int tabSize) {
        return EditorUtil.calcColumnNumber(editor, text, start, offset, tabSize);
    }

    public static int textWidthInColumns(@Nonnull Editor editor, @Nonnull CharSequence text, int start, int end, int x) {
        int startToUse = start;
        int lastTabSymbolIndex = -1;

        // Skip all lines except the last.
        loop:
        for (int i = end - 1; i >= start; i--) {
            switch (text.charAt(i)) {
                case '\n':
                    startToUse = i + 1;
                    break loop;
                case '\t':
                    if (lastTabSymbolIndex < 0) {
                        lastTabSymbolIndex = i;
                    }
            }
        }

        // Tabulation is assumed to be the only symbol which representation may take various number of visual columns, hence,
        // we return eagerly if no such symbol is found.
        if (lastTabSymbolIndex < 0) {
            return end - startToUse;
        }

        int result = 0;
        int spaceSize = getSpaceWidth(Font.PLAIN, editor);

        // Calculate number of columns up to the latest tabulation symbol.
        for (int i = startToUse; i <= lastTabSymbolIndex; i++) {
            SoftWrap softWrap = editor.getSoftWrapModel().getSoftWrap(i);
            if (softWrap != null) {
                x = softWrap.getIndentInPixels();
            }
            char c = text.charAt(i);
            int prevX = x;
            switch (c) {
                case '\t':
                    x = nextTabStop(x, editor);
                    result += columnsNumber(x - prevX, spaceSize);
                    break;
                case '\n':
                    x = result = 0;
                    break;
                default:
                    x += charWidth(c, Font.PLAIN, editor);
                    result++;
            }
        }

        // Add remaining tabulation-free columns.
        result += end - lastTabSymbolIndex - 1;
        return result;
    }

    public static int nextTabStop(int x, @Nonnull Editor editor) {
        int tabSize = getTabSize(editor);
        if (tabSize <= 0) {
            tabSize = 1;
        }
        return nextTabStop(x, editor, tabSize);
    }

    /**
     * Allows to answer how many visual columns are occupied by the given width.
     *
     * @param width          target width
     * @param plainSpaceSize width of the single space symbol within the target editor (in plain font style)
     * @return number of visual columns are occupied by the given width
     */
    public static int columnsNumber(int width, int plainSpaceSize) {
        int result = width / plainSpaceSize;
        if (width % plainSpaceSize > 0) {
            result++;
        }
        return result;
    }

    public static int nextTabStop(int x, @Nonnull Editor editor, int tabSize) {
        int leftInset = editor.getContentComponent().getInsets().left;
        return nextTabStop(x - leftInset, getSpaceWidth(Font.PLAIN, editor), tabSize) + leftInset;
    }

    public static int nextTabStop(int x, int plainSpaceWidth, int tabSize) {
        if (tabSize <= 0) {
            return x + plainSpaceWidth;
        }
        tabSize *= plainSpaceWidth;

        int nTabs = x / tabSize;
        return (nTabs + 1) * tabSize;
    }

    public static int getSpaceWidth(@JdkConstants.FontStyle int fontType, @Nonnull Editor editor) {
        int width = charWidth(' ', fontType, editor);
        return width > 0 ? width : 1;
    }

    public static int getPlainSpaceWidth(@Nonnull Editor editor) {
        return getSpaceWidth(Font.PLAIN, editor);
    }

    public static int charWidth(char c, @JdkConstants.FontStyle int fontType, @Nonnull Editor editor) {
        return fontForChar(c, fontType, editor).charWidth(c);
    }

    @Nonnull
    public static FontInfo fontForChar(final char c, @JdkConstants.FontStyle int style, @Nonnull Editor editor) {
        EditorColorsScheme colorsScheme = editor.getColorsScheme();
        return ComplementaryFontsRegistry.getFontAbleToDisplay(c,
            style,
            colorsScheme.getFontPreferences(),
            FontInfo.getFontRenderContext(editor.getContentComponent()));
    }
}
