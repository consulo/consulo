// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal;

import consulo.diff.comparison.iterable.DiffIterable;
import consulo.diff.comparison.iterable.DiffIterableUtil;
import consulo.diff.util.DiffRangeUtil;
import consulo.diff.util.LineOffsets;
import consulo.diff.util.LineOffsetsUtil;
import consulo.diff.util.Range;
import consulo.document.Document;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.internal.VcsRange;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LineStatusTrackerTestUtil {
    private LineStatusTrackerTestUtil() {
    }

    public static String parseInput(String input) {
        return input.replace('_', '\n');
    }

    public static void assertEqualRanges(List<VcsRange> actual, List<VcsRange> expected) {
        assertEquals(toString(expected), toString(actual));
    }

    private static String toString(List<VcsRange> ranges) {
        StringBuilder builder = new StringBuilder();
        for (VcsRange range : ranges) {
            builder.append('[').append(range.getLine1()).append(',').append(range.getLine2())
                .append(" - ").append(range.getVcsLine1()).append(',').append(range.getVcsLine2()).append("]\n");
        }
        return builder.toString();
    }

    public static void checkRangesAreValid(Document vcsDocument, Document document, List<VcsRange> ranges) {
        List<Range> diffRanges = new ArrayList<>(ranges.size());
        for (VcsRange range : ranges) {
            diffRanges.add(new Range(range.getVcsLine1(), range.getVcsLine2(), range.getLine1(), range.getLine2()));
        }

        CharSequence content1 = vcsDocument.getCharsSequence();
        CharSequence content2 = document.getCharsSequence();
        LineOffsets lineOffsets1 = LineOffsetsUtil.create(vcsDocument);
        LineOffsets lineOffsets2 = LineOffsetsUtil.create(document);

        DiffIterable iterable = DiffIterableUtil.fair(
            DiffIterableUtil.create(diffRanges, lineOffsets1.getLineCount(), lineOffsets2.getLineCount()));

        for (Range range : iterable.iterateUnchanged()) {
            List<String> lines1 = DiffRangeUtil.getLines(content1, lineOffsets1, range.start1, range.end1);
            List<String> lines2 = DiffRangeUtil.getLines(content2, lineOffsets2, range.start2, range.end2);
            assertEquals(lines1, lines2);
        }
    }

    public static void checkCantTrim(Document vcsDocument, Document document, List<VcsRange> ranges) {
        for (VcsRange range : ranges) {
            if (range.getType() != VcsRange.MODIFIED) continue;

            List<String> lines1 = getVcsLines(vcsDocument, range);
            List<String> lines2 = getCurrentLines(document, range);
            if (lines1.isEmpty() || lines2.isEmpty()) continue;

            assertFalse(lines1.get(0).equals(lines2.get(0)), "range can be trimmed from the start: " + range);
            assertFalse(lines1.get(lines1.size() - 1).equals(lines2.get(lines2.size() - 1)),
                "range can be trimmed from the end: " + range);
        }
    }

    public static void checkCantMerge(List<VcsRange> ranges) {
        for (int i = 0; i < ranges.size() - 1; i++) {
            assertFalse(ranges.get(i).getLine2() == ranges.get(i + 1).getLine1(),
                "ranges " + i + " and " + (i + 1) + " can be merged");
        }
    }

    public static void checkInnerRanges(Document vcsDocument, Document document, List<VcsRange> ranges) {
        for (VcsRange range : ranges) {
            List<VcsRange.InnerRange> innerRanges = range.getInnerRanges();
            if (innerRanges == null) continue;

            if (range.getType() != VcsRange.MODIFIED) {
                assertTrue(innerRanges.isEmpty(), "non-modified range must have no inner ranges: " + range);
                continue;
            }

            int last = 0;
            for (VcsRange.InnerRange innerRange : innerRanges) {
                assertEquals(innerRange.getLine1() == innerRange.getLine2(),
                    innerRange.getType() == VcsRange.DELETED,
                    "only DELETED inner ranges may be empty");

                assertEquals(last, innerRange.getLine1(), "inner ranges must be contiguous");
                last = innerRange.getLine2();
            }
            assertEquals(range.getLine2() - range.getLine1(), last,
                "inner ranges must cover the whole range, relative to its start");

            List<String> lines1 = getVcsLines(vcsDocument, range);
            List<String> lines2 = getCurrentLines(document, range);

            int start = 0;
            for (VcsRange.InnerRange innerRange : innerRanges) {
                if (innerRange.getType() != VcsRange.EQUAL) continue;

                for (int i = innerRange.getLine1(); i < innerRange.getLine2(); i++) {
                    String line = lines2.get(i);
                    int index = -1;
                    for (int j = start; j < lines1.size(); j++) {
                        if (StringUtil.equalsIgnoreWhitespaces(lines1.get(j), line)) {
                            index = j - start;
                            break;
                        }
                    }
                    assertTrue(index != -1, "EQUAL inner line not found on the vcs side: '" + line + "'");
                    start += index + 1;
                }
            }
        }
    }

    private static List<String> getVcsLines(Document document, VcsRange range) {
        return DiffRangeUtil.getLines(document.getCharsSequence(), LineOffsetsUtil.create(document),
            range.getVcsLine1(), range.getVcsLine2());
    }

    private static List<String> getCurrentLines(Document document, VcsRange range) {
        return DiffRangeUtil.getLines(document.getCharsSequence(), LineOffsetsUtil.create(document),
            range.getLine1(), range.getLine2());
    }
}
