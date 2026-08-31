// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal;

import consulo.application.Application;
import consulo.disposer.AutoDisposable;
import consulo.document.Document;
import consulo.document.internal.DocumentFactory;
import consulo.test.light.LightApplicationBuilder;
import consulo.versionControlSystem.internal.VcsRange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static consulo.versionControlSystem.impl.internal.LineStatusTrackerTestUtil.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocumentTrackerTest {
    @Test
    public void testInitialEmpty() {
        test("", "", t -> t.assertRanges());
    }

    @Test
    public void testInitialEquals() {
        test("1234_2345_3456", "1234_2345_3456", t -> t.assertRanges());
    }

    @Test
    public void testInitialInsertion() {
        test("1234_2345_3456", "1234_3456", t -> t.assertRanges(range(1, 2, 1, 1)));
    }

    @Test
    public void testInitialDeletion() {
        test("1234_3456", "1234_2345_3456", t -> t.assertRanges(range(1, 1, 1, 2)));
    }

    @Test
    public void testInitialModification() {
        test("1234_x_3456", "1234_2345_3456", t -> t.assertRanges(range(1, 2, 1, 2)));
    }

    @Test
    public void testInitialModification2() {
        test("1_3_4_5_6_7", "1_2_3_5_6_12", t -> t.assertRanges(
            range(1, 1, 1, 2),
            range(2, 3, 3, 3),
            range(5, 6, 5, 6)));
    }

    @Test
    public void testSimpleInsert() {
        test("1234_2345_3456", t -> {
            t.insertAfter("12", "a");
            t.assertRanges(range(0, 1, 0, 1));
        });
    }

    @Test
    public void testUndo() {
        test("1234_2345_3456", t -> {
            t.insertAfter("1234_23", "a");
            t.assertRanges(range(1, 2, 1, 2));

            t.delete("a");
            t.assertRanges();
        });
    }

    @Test
    public void testLineEndBeforeModification() {
        test("1234_2345_3456", t -> {
            t.insertAfter("1234_2", "a");
            t.assertRanges(range(1, 2, 1, 2));

            t.insertAfter("1234_", "_");
            t.assertRanges(range(1, 3, 1, 2));
        });
    }

    @Test
    public void testInsertNewlineAtStart() {
        test("1234_2345_3456", t -> {
            t.insertAtStart("_");
            t.assertRanges(range(0, 1, 0, 0));
        });
    }

    @Test
    public void testWholeLineInsertion() {
        test("1234_2345_3456", t -> {
            t.insertAfter("1234_", "xxxx_");
            t.assertRanges(range(1, 2, 1, 1));
        });
    }

    @Test
    public void testWholeLineDeletion() {
        test("1234_2345_3456", t -> {
            t.delete("2345_");
            t.assertRanges(range(1, 1, 1, 2));
        });
    }

    @Test
    public void testMultipleSeparateEdits() {
        test("1_2_3_4_5_6_7_8_9", t -> {
            t.replace("2", "x");
            t.replace("8", "y");
            t.assertRanges(range(1, 2, 1, 2), range(7, 8, 7, 8));
        });
    }

    @Test
    public void testEditsMergeIntoSingleRange() {
        test("1_2_3_4_5", t -> {
            t.replace("2", "x");
            t.replace("3", "y");
            t.assertRanges(range(1, 3, 1, 3));
        });
    }

    @Test
    public void testReplaceWholeText() {
        test("1_2_3_4_5", t -> {
            t.replaceWholeText("a_b_c");
            t.assertRanges(range(0, 3, 0, 5));
        });
    }

    @Test
    public void testDeleteEverything() {
        test("1_2_3_4_5", t -> {
            t.replaceWholeText("");
            t.assertRanges(range(0, 1, 0, 5));
        });
    }

    @Test
    public void testRevertBackToBase() {
        test("1_2_3_4_5", t -> {
            t.replace("3", "xxx");
            t.assertRanges(range(2, 3, 2, 3));

            t.replace("xxx", "3");
            t.assertRanges();
        });
    }

    @Test
    public void testRepeatedEditsInSamePlace() {
        test("1_2_3_4_5", t -> {
            for (int i = 0; i < 10; i++) {
                t.insertAfter("3", "x");
            }
            t.assertRanges(range(2, 3, 2, 3));
        });
    }

    @Test
    public void testInsertManyLines() {
        test("1_2_3", t -> {
            t.insertAfter("1_", "a_b_c_d_e_");
            t.assertRanges(range(1, 6, 1, 1));
        });
    }

    @Test
    public void testInsertBeforeExistingRangeShiftsIt() {
        test("1_2_3_4_5_6_7_8_9", t -> {
            t.replace("8", "y");
            t.assertRanges(range(7, 8, 7, 8));

            t.insertAfter("1_", "a_");
            t.assertRanges(range(1, 2, 1, 1), range(8, 9, 7, 8));
        });
    }

    @Test
    public void testDeleteBeforeExistingRangeShiftsIt() {
        test("1_2_3_4_5_6_7_8_9", t -> {
            t.replace("8", "y");
            t.assertRanges(range(7, 8, 7, 8));

            t.delete("2_");
            t.assertRanges(range(1, 1, 1, 2), range(6, 7, 7, 8));
        });
    }

    @Test
    public void testInsertBeforeMultipleExistingRangesShiftsAll() {
        test("1_2_3_4_5_6_7_8_9", t -> {
            t.replace("5", "x");
            t.replace("8", "y");
            t.assertRanges(range(4, 5, 4, 5), range(7, 8, 7, 8));

            t.insertAfter("1_", "a_");
            t.assertRanges(range(1, 2, 1, 1), range(5, 6, 4, 5), range(8, 9, 7, 8));
        });
    }

    @Test
    public void testPressEnterAtEndOfFileNoTrailingNewline() {
        test("abc", t -> {
            t.insertAfter("abc", "_");
            t.assertRangesNotEmpty();
        });
    }

    @Test
    public void testPressEnterInMiddleOfSingleLine() {
        test("abc", t -> {
            t.insertAfter("ab", "_");
            t.assertRangesNotEmpty();
        });
    }

    @Test
    public void testPressEnterTwiceAtEndOfFile() {
        test("abc", t -> {
            t.insertAfter("abc", "_");
            t.assertRangesNotEmpty();
            t.insertAfter("abc_", "_");
            t.assertRangesNotEmpty();
        });
    }

    private static VcsRange range(int line1, int line2, int vcsLine1, int vcsLine2) {
        return new VcsRange(line1, line2, vcsLine1, vcsLine2);
    }

    private void test(String text, Consumer<Fixture> task) {
        test(text, text, task);
    }

    private void test(String text, String vcsText, Consumer<Fixture> task) {
        try (AutoDisposable disposable = AutoDisposable.newAutoDisposable("DocumentTrackerTest")) {
            Application application = LightApplicationBuilder.create(disposable).build();

            DocumentFactory documentFactory = application.getInstance(DocumentFactory.class);
            Document vcsDocument = documentFactory.createDocument(parseInput(vcsText), true, true);
            Document document = documentFactory.createDocument(parseInput(text), true, true);

            DocumentTracker tracker = new DocumentTracker(vcsDocument, document);
            try {
                Fixture fixture = new Fixture(tracker, document, vcsDocument);
                fixture.refreshAndVerify();
                task.accept(fixture);
                fixture.refreshAndVerify();
            }
            finally {
                tracker.dispose();
            }
        }
    }

    private static final class Fixture {
        private final DocumentTracker myTracker;
        private final Document myDocument;
        private final Document myVcsDocument;

        Fixture(DocumentTracker tracker, Document document, Document vcsDocument) {
            myTracker = tracker;
            myDocument = document;
            myVcsDocument = vcsDocument;
        }

        void insertAtStart(String text) {
            myDocument.insertString(0, parseInput(text));
            refreshAndVerify();
        }

        void insertAfter(String pattern, String text) {
            myDocument.insertString(endOf(pattern), parseInput(text));
            refreshAndVerify();
        }

        void delete(String pattern) {
            myDocument.deleteString(startOf(pattern), endOf(pattern));
            refreshAndVerify();
        }

        void replace(String pattern, String text) {
            myDocument.replaceString(startOf(pattern), endOf(pattern), parseInput(text));
            refreshAndVerify();
        }

        void replaceWholeText(String text) {
            myDocument.replaceString(0, myDocument.getTextLength(), parseInput(text));
            refreshAndVerify();
        }

        void assertRangesNotEmpty() {
            if (currentRanges().isEmpty()) {
                throw new AssertionError("expected a changed range, but tracker reports none");
            }
        }

        void assertRanges(VcsRange... expected) {
            assertEqualRanges(currentRanges(), Arrays.asList(expected));
        }

        void refreshAndVerify() {
            myTracker.refreshDirty(false);

            List<VcsRange> actual = currentRanges();
            assertEqualRanges(actual, RangesBuilder.createRanges(myDocument, myVcsDocument));

            checkRangesAreValid(myVcsDocument, myDocument, actual);
            checkCantTrim(myVcsDocument, myDocument, actual);
            checkCantMerge(actual);
        }

        private List<VcsRange> currentRanges() {
            List<VcsRange> result = new ArrayList<>();
            for (DocumentTracker.Block block : myTracker.getBlocks()) {
                result.add(new VcsRange(block.getStart(), block.getEnd(), block.getVcsStart(), block.getVcsEnd()));
            }
            return result;
        }

        private int startOf(String pattern) {
            return findPattern(pattern);
        }

        private int endOf(String pattern) {
            return findPattern(pattern) + parseInput(pattern).length();
        }

        private int findPattern(String pattern) {
            String text = parseInput(pattern);
            CharSequence sequence = myDocument.getImmutableCharSequence();
            int firstOffset = sequence.toString().indexOf(text);
            int lastOffset = sequence.toString().lastIndexOf(text);
            assertTrue(firstOffset == lastOffset && firstOffset != -1,
                "pattern '" + pattern + "' must occur exactly once");
            return firstOffset;
        }
    }
}
