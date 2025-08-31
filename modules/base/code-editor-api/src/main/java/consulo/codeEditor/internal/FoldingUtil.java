// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.codeEditor.internal;

import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public final class FoldingUtil {
    private FoldingUtil() {
    }

    @Nullable
    public static FoldRegion findFoldRegion(@Nonnull Editor editor, int startOffset, int endOffset) {
        FoldRegion region = editor.getFoldingModel().getFoldRegion(startOffset, endOffset);
        return region != null && region.isValid() ? region : null;
    }

    @Nullable
    public static FoldRegion findFoldRegionStartingAtLine(@Nonnull Editor editor, int line) {
        if (line < 0 || line >= editor.getDocument().getLineCount()) {
            return null;
        }
        FoldRegion result = null;
        FoldRegion[] regions = editor.getFoldingModel().getAllFoldRegions();
        for (FoldRegion region : regions) {
            if (!region.isValid()) {
                continue;
            }
            if (region.getDocument().getLineNumber(region.getStartOffset()) == line) {
                if (result != null) {
                    return null;
                }
                result = region;
            }
        }
        return result;
    }

    public static FoldRegion[] getFoldRegionsAtOffset(Editor editor, int offset) {
        List<FoldRegion> list = new ArrayList<>();
        FoldRegion[] allRegions = editor.getFoldingModel().getAllFoldRegions();
        for (FoldRegion region : allRegions) {
            if (region.getStartOffset() <= offset && offset <= region.getEndOffset()) {
                list.add(region);
            }
        }

        FoldRegion[] regions = list.toArray(FoldRegion.EMPTY_ARRAY);
        Arrays.sort(regions, Collections.reverseOrder(RangeMarker.BY_START_OFFSET));
        return regions;
    }

    public static boolean caretInsideRange(Editor editor, TextRange range) {
        int offset = editor.getCaretModel().getOffset();
        return range.contains(offset) && range.getStartOffset() != offset;
    }

    public static boolean isHighlighterFolded(@Nonnull Editor editor, @Nonnull RangeHighlighter highlighter) {
        int startOffset = highlighter instanceof RangeHighlighterEx ? ((RangeHighlighterEx) highlighter).getAffectedAreaStartOffset() : highlighter.getStartOffset();
        int endOffset = highlighter instanceof RangeHighlighterEx ? ((RangeHighlighterEx) highlighter).getAffectedAreaEndOffset() : highlighter.getEndOffset();
        return isTextRangeFolded(editor, new TextRange(startOffset, endOffset));
    }

    public static boolean isTextRangeFolded(@Nonnull Editor editor, @Nonnull TextRange range) {
        FoldRegion foldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(range.getStartOffset());
        return foldRegion != null && range.getEndOffset() <= foldRegion.getEndOffset();
    }

    /**
     * Iterates fold regions tree in a depth-first order (pre-order)
     */
    public static Iterator<FoldRegion> createFoldTreeIterator(@Nonnull Editor editor) {
        final FoldRegion[] allRegions = editor.getFoldingModel().getAllFoldRegions();
        return new Iterator<FoldRegion>() {
            private int sectionStart;
            private int current;
            private int sectionEnd;

            {
                advanceSection();
            }

            private void advanceSection() {
                sectionStart = sectionEnd;
                //noinspection StatementWithEmptyBody
                for (sectionEnd = sectionStart + 1; sectionEnd < allRegions.length && allRegions[sectionEnd].getStartOffset() == allRegions[sectionStart].getStartOffset(); sectionEnd++)
                    ;
                current = sectionEnd;
            }

            @Override
            public boolean hasNext() {
                return current > sectionStart || sectionEnd < allRegions.length;
            }

            @Override
            public FoldRegion next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                if (current <= sectionStart) {
                    advanceSection();
                }
                return allRegions[--current];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static void expandRegionAtOffset(@Nonnull Project project, @Nonnull Editor editor, int offset) {
        CodeEditorInternalHelper foldingManager = CodeEditorInternalHelper.getInstance();
        foldingManager.updateFoldRegions(project, editor);

        int line = editor.getDocument().getLineNumber(offset);
        Runnable processor = () -> {
            FoldRegion region = FoldingUtil.findFoldRegionStartingAtLine(editor, line);
            if (region != null && !region.isExpanded()) {
                region.setExpanded(true);
            }
            else {
                FoldRegion[] regions = FoldingUtil.getFoldRegionsAtOffset(editor, offset);
                for (int i = regions.length - 1; i >= 0; i--) {
                    region = regions[i];
                    if (!region.isExpanded()) {
                        region.setExpanded(true);
                        break;
                    }
                }
            }
        };
        editor.getFoldingModel().runBatchFoldingOperation(processor);
    }
}
