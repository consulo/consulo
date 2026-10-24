// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.review.util;

import consulo.diff.util.Range;
import consulo.util.collection.PeekableIteratorWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Input: given 3 revisions: A -&gt; B -&gt; C and 2 sets of differences between them: earlyChanges 'A -&gt; B' and laterChanges 'B -&gt; C'.
 * We want to translate this into a list of 'A -&gt; C' offsets excluding laterChanges.
 * If there's a conflict, and we can't precisely map A -&gt; C we split the range and map multiple ranges in C to a single range in A.
 */
public final class ExcludingApproximateChangedRangesShifter {
    private ExcludingApproximateChangedRangesShifter() {
    }

    public static List<Range> shift(Iterable<Range> earlyChanges, Iterable<Range> laterChanges) {
        List<Range> result = new ArrayList<>();
        PeekableIteratorWrapper<Range> iLater = new PeekableIteratorWrapper<>(laterChanges.iterator());

        int cShift = 0;
        for (Range earlyRange : earlyChanges) {
            int aStart = earlyRange.start1();
            int aEnd = earlyRange.end1();
            int bStart = earlyRange.start2();
            int bEnd = earlyRange.end2();

            // early range was fully mapped without leftovers
            boolean fullyMapped = false;
            while (iLater.hasNext()) {
                Range laterRange = iLater.peek();

                // lines in B
                int leftStart = earlyRange.start2();
                int leftEnd = earlyRange.end2();
                int rightStart = laterRange.start1();
                int rightEnd = laterRange.end1();

                // line number shift for C
                int deleted = laterRange.end1() - laterRange.start1();
                int inserted = laterRange.end2() - laterRange.start2();
                int laterDelta = inserted - deleted;

                // no intersection, "later" before "early"
                if (rightEnd <= leftStart) {
                    cShift += laterDelta;
                    iLater.next();
                }
                // no intersection, "later" after "early"
                else if (rightStart >= leftEnd) {
                    break;
                }
                else if (rightStart <= leftStart) {
                    // "early" fully inside "later"
                    if (rightEnd >= leftEnd) {
                        fullyMapped = true;
                        break;
                    }
                    else {
                        // partial intersection at the start
                        bStart = rightEnd;
                        cShift += laterDelta;
                        iLater.next();
                    }
                }
                else {
                    result.add(new Range(aStart, aEnd, cShift + bStart, laterRange.start2()));
                    if (rightEnd == leftEnd) {
                        // "later" fully inside "early"
                        cShift += laterDelta;
                        iLater.next();
                        fullyMapped = true;
                        break;
                    }
                    else if (rightEnd < leftEnd) {
                        // "later" inside "early"
                        bStart = rightEnd;
                        cShift += laterDelta;
                        iLater.next();
                    }
                    else {
                        // intersection in the end
                        fullyMapped = true;
                        break;
                    }
                }
            }
            // add leftover
            if (!fullyMapped && bStart <= bEnd) {
                result.add(new Range(aStart, aEnd, cShift + bStart, cShift + bEnd));
            }
        }
        return result;
    }
}
