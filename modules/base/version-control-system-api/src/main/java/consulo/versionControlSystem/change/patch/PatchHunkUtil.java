// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.change.patch;

import consulo.diff.util.LineRange;
import consulo.diff.util.Range;
import consulo.diff.util.Side;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PatchHunkUtil {
    private PatchHunkUtil() {
    }

    public static Range getRange(PatchHunk hunk) {
        int end1 = hunk.getStartLineBefore();
        int end2 = hunk.getStartLineAfter();

        for (PatchLine line : hunk.getLines()) {
            switch (line.getType()) {
                case REMOVE -> end1++;
                case ADD -> end2++;
                case CONTEXT -> {
                    end1++;
                    end2++;
                }
            }
        }

        return new Range(hunk.getStartLineBefore(), end1, hunk.getStartLineAfter(), end2);
    }

    public static List<Range> getChangeOnlyRanges(PatchHunk hunk) {
        List<Range> ranges = new ArrayList<>();
        int start1 = Math.max(hunk.getStartLineBefore(), 0);
        int start2 = Math.max(hunk.getStartLineAfter(), 0);
        int end1 = Math.max(hunk.getStartLineBefore(), 0);
        int end2 = Math.max(hunk.getStartLineAfter(), 0);
        boolean changeFound = false;
        boolean newLine1 = false;
        boolean newLine2 = false;

        for (PatchLine line : hunk.getLines()) {
            switch (line.getType()) {
                case REMOVE -> {
                    end1++;
                    changeFound = true;
                    newLine1 = !line.isSuppressNewLine();
                }
                case ADD -> {
                    end2++;
                    changeFound = true;
                    newLine2 = !line.isSuppressNewLine();
                }
                case CONTEXT -> {
                    if (changeFound) {
                        ranges.add(new Range(start1, end1, start2, end2));
                        start1 = end1;
                        start2 = end2;
                    }
                    start1++;
                    start2++;
                    end1++;
                    end2++;
                    changeFound = false;
                    newLine1 = !line.isSuppressNewLine();
                    newLine2 = !line.isSuppressNewLine();
                }
            }
        }
        if (changeFound) {
            if (newLine1 != newLine2) {
                if (newLine1) {
                    end1++;
                }
                if (newLine2) {
                    end2++;
                }
            }
            ranges.add(new Range(start1, end1, start2, end2));
        }
        return ranges;
    }

    public static List<Range> withoutContext(Collection<PatchHunk> hunks) {
        List<Range> ranges = new ArrayList<>();
        for (PatchHunk hunk : hunks) {
            ranges.addAll(getChangeOnlyRanges(hunk));
        }
        return ranges;
    }

    public static @Nullable Integer findHunkLineIndex(PatchHunk hunk, Pair<Side, Integer> locationInDiff) {
        return findHunkLineIndex(hunk, locationInDiff, false);
    }

    /**
     * @param diffFile if {@code true} will return index of the line in diff file (with header and no-newline comments), else - in {@link PatchHunk}
     */
    public static @Nullable Integer findHunkLineIndex(PatchHunk hunk, Pair<Side, Integer> locationInDiff, boolean diffFile) {
        Side side = locationInDiff.getFirst();
        int fileLineIndex = locationInDiff.getSecond();
        int sideFileLineIndex = fileLineIndex - side.select(hunk.getStartLineBefore(), hunk.getStartLineAfter());
        int sideFileLineCounter = 0;

        // +1 for header
        int hunkLineIndex = diffFile ? 1 : 0;

        Integer lastMatchedLineWithNewline = null;
        for (PatchLine line : hunk.getLines()) {
            if (line.getType() == PatchLine.Type.ADD && side == Side.RIGHT
                || line.getType() == PatchLine.Type.REMOVE && side == Side.LEFT
                || line.getType() == PatchLine.Type.CONTEXT) {
                if (sideFileLineCounter == sideFileLineIndex) {
                    return hunkLineIndex;
                }
                sideFileLineCounter++;
                //potentially a comment on a newline
                if (sideFileLineCounter == sideFileLineIndex && !line.isSuppressNewLine()) {
                    lastMatchedLineWithNewline = hunkLineIndex;
                }
            }
            hunkLineIndex += diffFile && line.isSuppressNewLine() ? 2 : 1;
        }
        return lastMatchedLineWithNewline;
    }

    public static @Nullable Integer findDiffFileLineIndex(TextFilePatch patch, Pair<Side, Integer> locationInDiff) {
        Pair<PatchHunk, Integer> hunkWithOffset = findHunkWithOffset(patch, locationInDiff);
        if (hunkWithOffset == null) {
            return null;
        }
        Integer hunkLineIndex = findHunkLineIndex(hunkWithOffset.getFirst(), locationInDiff, true);
        if (hunkLineIndex == null) {
            return null;
        }

        // header included in offset
        return hunkWithOffset.getSecond() + (hunkLineIndex - 1);
    }

    private static @Nullable Pair<PatchHunk, Integer> findHunkWithOffset(TextFilePatch patch, Pair<Side, Integer> locationInDiff) {
        Side side = locationInDiff.getFirst();
        int lineIndex = locationInDiff.getSecond();
        int diffLineCounter = 0;
        for (PatchHunk hunk : patch.getHunks()) {
            // +1 for header
            diffLineCounter++;
            Range range = getRange(hunk);
            int start = side.select(range.start1(), range.start2());
            int end = side.select(range.end1(), range.end2());

            if (lineIndex >= start && lineIndex <= end) {
                return Pair.create(hunk, diffLineCounter);
            }

            int suppressedNewLines = 0;
            for (PatchLine line : hunk.getLines()) {
                if (line.isSuppressNewLine()) {
                    suppressedNewLines++;
                }
            }
            diffLineCounter += hunk.getLines().size() + suppressedNewLines;
        }
        return null;
    }

    public static String createPatchFromHunk(String filePath, String diffHunk) {
        return "--- a/" + filePath + "\n"
            + "+++ b/" + filePath + "\n"
            + diffHunk;
    }

    public static PatchHunk truncateHunkBefore(PatchHunk hunk, int hunkLineIndex) {
        if (hunkLineIndex <= 0) {
            return hunk;
        }
        List<PatchLine> lines = hunk.getLines();

        int startLineBefore = hunk.getStartLineBefore();
        int startLineAfter = hunk.getStartLineAfter();

        for (int i = 0; i < hunkLineIndex; i++) {
            PatchLine line = lines.get(i);
            switch (line.getType()) {
                case CONTEXT -> {
                    startLineBefore++;
                    startLineAfter++;
                }
                case ADD -> startLineAfter++;
                case REMOVE -> startLineBefore++;
            }
        }
        List<PatchLine> truncatedLines = lines.subList(hunkLineIndex, lines.size());
        PatchHunk result = new PatchHunk(startLineBefore, hunk.getEndLineBefore(), startLineAfter, hunk.getEndLineAfter());
        for (PatchLine line : truncatedLines) {
            result.addLine(line);
        }
        return result;
    }

    public static PatchHunk truncateHunkAfter(PatchHunk hunk, int hunkLineIndex) {
        List<PatchLine> lines = hunk.getLines();
        if (hunkLineIndex > lines.size() - 1) {
            return hunk;
        }

        int endLineBefore = hunk.getEndLineBefore();
        int endLineAfter = hunk.getEndLineAfter();

        for (int i = lines.size() - 1; i >= hunkLineIndex; i--) {
            PatchLine line = lines.get(i);
            switch (line.getType()) {
                case CONTEXT -> {
                    endLineBefore--;
                    endLineAfter--;
                }
                case ADD -> endLineAfter--;
                case REMOVE -> endLineBefore--;
            }
        }
        List<PatchLine> truncatedLines = lines.subList(0, hunkLineIndex + 1);
        PatchHunk result = new PatchHunk(hunk.getStartLineBefore(), endLineBefore, hunk.getStartLineAfter(), endLineAfter);
        for (PatchLine line : truncatedLines) {
            result.addLine(line);
        }
        return result;
    }

    public static List<PatchLine> getLinesInRange(PatchHunk hunk, Side side, LineRange range) {
        int lineIdx = hunk.getStartLineAfter();
        List<PatchLine> result = new ArrayList<>();
        for (PatchLine line : hunk.getLines()) {
            PatchLine.Type ignoredType = side == Side.RIGHT ? PatchLine.Type.REMOVE : PatchLine.Type.ADD;
            if (line.getType() == ignoredType) {
                continue;
            }
            if (lineIdx >= range.start()) {
                result.add(line);
            }
            lineIdx++;
            if (lineIdx >= range.end()) {
                break;
            }
        }
        return result;
    }

    public static @Nullable String getLinesLeft(TextFilePatch patch, LineRange lines) {
        for (PatchHunk hunk : patch.getHunks()) {
            if (hunk.getStartLineBefore() <= lines.start() && hunk.getEndLineBefore() >= lines.end()) {
                StringBuilder builder = new StringBuilder();
                int lineCounter = hunk.getStartLineBefore();
                for (PatchLine line : hunk.getLines()) {
                    if (line.getType() == PatchLine.Type.CONTEXT) {
                        lineCounter++;
                    }
                    if (line.getType() == PatchLine.Type.REMOVE) {
                        if (lineCounter >= lines.start()) {
                            builder.append(line.getText());
                            if (!line.isSuppressNewLine()) {
                                builder.append("\n");
                            }
                        }
                        lineCounter++;
                    }
                    if (lineCounter >= lines.end()) {
                        break;
                    }
                }
                return builder.toString();
            }
        }
        return null;
    }
}
