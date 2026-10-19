// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.patch.apply;

import consulo.diff.util.LineOffsets;
import consulo.diff.util.LineOffsetsUtil;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.versionControlSystem.change.patch.PatchLine;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class PlainSimplePatchApplier {
    private static final Logger LOG = Logger.getInstance(PlainSimplePatchApplier.class);

    private final List<? extends PatchHunk> myHunks;
    private final CharSequence myText;
    private final LineOffsets myLineOffsets;

    private final StringBuilder sb = new StringBuilder();
    private int baseLine = 0;
    private int patchedLine = 0;

    public static @Nullable String apply(CharSequence text, List<? extends PatchHunk> hunks) {
        return new PlainSimplePatchApplier(text, hunks).execute();
    }

    private PlainSimplePatchApplier(CharSequence text, List<? extends PatchHunk> hunks) {
        myText = text;
        myHunks = hunks;
        myLineOffsets = LineOffsetsUtil.create(text);
    }

    private @Nullable String execute() {
        if (myHunks.isEmpty()) {
            return myText.toString();
        }

        try {
            for (PatchHunk hunk : myHunks) {
                appendUnchangedLines(hunk.getStartLineBefore());

                checkContextLines(hunk);

                applyChangedLines(hunk);
            }

            if (!handleLastLine()) {
                appendUnchangedLines(myLineOffsets.getLineCount());
            }

            return sb.toString();
        }
        catch (PatchApplyException e) {
            LOG.debug(e);
            return null;
        }
    }

    private boolean handleLastLine() {
        List<PatchLine> lastHunkLines = myHunks.get(myHunks.size() - 1).getLines();
        PatchLine lastBaseLine = findLast(lastHunkLines, PatchLine.Type.ADD);
        PatchLine lastPatchedLine = findLast(lastHunkLines, PatchLine.Type.REMOVE);

        if (lastBaseLine != null) {
            boolean lastLineAlreadyApplied =
                !lastBaseLine.isSuppressNewLine() && baseLine + 1 == myLineOffsets.getLineCount() && getLineContent(baseLine).isEmpty() ||
                    lastBaseLine.isSuppressNewLine() && baseLine == myLineOffsets.getLineCount();
            if (lastLineAlreadyApplied) {
                boolean isNoNewlinePatched = lastPatchedLine != null ? lastPatchedLine.isSuppressNewLine() : lastBaseLine.isSuppressNewLine();
                if (!isNoNewlinePatched) {
                    if (patchedLine > 0) {
                        sb.append('\n');
                    }
                }
                return true;
            }
            return false;
        }

        // insertion into empty file - use "No newline at end of file" flag from patch
        if (baseLine == 0 && myText.isEmpty()) {
            boolean isNoNewlinePatched = lastPatchedLine != null && lastPatchedLine.isSuppressNewLine();
            if (!isNoNewlinePatched) {
                if (patchedLine > 0) {
                    sb.append('\n');
                }
            }
            return true;
        }

        return false;
    }

    private static @Nullable PatchLine findLast(List<PatchLine> lines, PatchLine.Type skippedType) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).getType() != skippedType) {
                return lines.get(i);
            }
        }
        return null;
    }

    private void checkContextLines(PatchHunk hunk) {
        int baseStart = hunk.getStartLineBefore();
        int baseEnd = hunk.getEndLineBefore();
        int patchedStart = hunk.getStartLineAfter();
        int patchedEnd = hunk.getEndLineAfter();

        if (baseLine != baseStart) {
            error("Unexpected chunk base start: expected  - " + baseLine + ", actual - " + baseStart);
        }
        if (patchedLine != patchedStart) {
            error("Unexpected chunk patched start: expected  - " + patchedLine + ", actual - " + patchedStart);
        }
        if (baseEnd > myLineOffsets.getLineCount()) {
            error("Unexpected chunk base end: total lines  - " + myLineOffsets.getLineCount() + ", hunk end - " + baseEnd);
        }

        int baseCount = 0;
        int patchedCount = 0;
        for (PatchLine patchLine : hunk.getLines()) {
            if (patchLine.getType() != PatchLine.Type.ADD) {
                baseCount++;
            }
            if (patchLine.getType() != PatchLine.Type.REMOVE) {
                patchedCount++;
            }
        }

        if (baseCount != baseEnd - baseStart) {
            error("Unexpected chunk base body: expected - " + (baseEnd - baseStart) + ", actual - " + baseCount);
        }
        if (patchedCount != patchedEnd - patchedStart) {
            error("Unexpected chunk patched body: expected - " + (patchedEnd - patchedStart) + ", actual - " + patchedCount);
        }

        int count = 0;
        for (PatchLine patchLine : hunk.getLines()) {
            if (patchLine.getType() != PatchLine.Type.ADD) {
                CharSequence expectedContent = getLineContent(baseStart + count);
                String actualContent = patchLine.getText();
                if (!StringUtil.equals(expectedContent, actualContent)) {
                    error("Unexpected chunk content: expected - " + expectedContent + ", actual - " + patchLine);
                }
                count++;
            }
        }
    }

    private void applyChangedLines(PatchHunk hunk) {
        for (PatchLine patchLine : hunk.getLines()) {
            if (patchLine.getType() != PatchLine.Type.REMOVE) {
                appendLine(patchLine.getText());
            }
        }
        baseLine = hunk.getEndLineBefore();
    }

    private void appendUnchangedLines(int untilLine) {
        if (baseLine > untilLine) {
            error("Unexpected baseline: expected - " + baseLine + ", actual - " + untilLine);
        }
        if (untilLine > myLineOffsets.getLineCount()) {
            error("Unexpected baseline: total lines - " + myLineOffsets.getLineCount() + ", actual - " + untilLine);
        }

        for (int i = baseLine; i < untilLine; i++) {
            appendLine(getLineContent(i));
        }
        baseLine = untilLine;
    }

    private void appendLine(CharSequence lineContent) {
        if (patchedLine > 0) {
            sb.append('\n');
        }
        sb.append(lineContent);
        patchedLine++;
    }

    private CharSequence getLineContent(int line) {
        return myText.subSequence(myLineOffsets.getLineStart(line), myLineOffsets.getLineEnd(line));
    }

    private static void error(String error) {
        throw new PatchApplyException(error);
    }

    private static class PatchApplyException extends RuntimeException {
        PatchApplyException(String message) {
            super(message);
        }
    }
}
