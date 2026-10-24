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
package consulo.it.review;

import consulo.diff.comparison.iterable.DiffIterable;
import consulo.diff.comparison.iterable.DiffIterableUtil;
import consulo.diff.util.Range;
import consulo.versionControlSystem.review.util.ExcludingApproximateChangedRangesShifter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExcludingApproximateChangedRangesShifterTest {
    @Test
    public void testEmpty() {
        test(c -> {
        }, c -> {
        }, c -> {
        });
    }

    @Test
    public void testUnchanged() {
        Consumer<RangesCollector> earlyBuilder = c -> {
            c.r(0, 5, 0, 0);
            c.r(5, 10, 1, 5);
        };
        test(earlyBuilder, c -> {
        }, earlyBuilder);
    }

    @Test
    public void testNoEarly() {
        test(c -> {
        }, c -> {
            c.r(0, 5, 0, 0);
            c.r(5, 10, 1, 5);
        }, c -> {
        });
    }

    @Test
    public void testBeginningIntersection() {
        test(c -> c.r(5, 10, 5, 10), c -> c.r(4, 7, 4, 7), c -> c.r(7, 10, 5, 10));
    }

    @Test
    public void testEndIntersection() {
        test(c -> c.r(5, 10, 5, 10), c -> c.r(7, 12, 7, 12), c -> c.r(5, 7, 5, 10));
    }

    @Test
    public void testEarlyInside() {
        test(c -> c.r(5, 10, 5, 10), c -> c.r(4, 12, 4, 12), c -> {
        });
    }

    @Test
    public void testLaterInside() {
        test(c -> c.r(5, 10, 5, 10), c -> c.r(6, 6, 6, 8), c -> {
            c.r(5, 6, 5, 10);
            c.r(6, 8, 5, 10);
        });
    }

    @Test
    public void testLaterInsideAndNoIntersect() {
        test(c -> {
            c.r(5, 10, 5, 10);
            c.r(25, 30, 25, 30);
        }, c -> c.r(6, 6, 6, 8), c -> {
            c.r(5, 6, 5, 10);
            c.r(6, 8, 5, 10);
            c.r(23, 28, 25, 30);
        });
    }

    @Test
    public void testEarlyInsideAndNoIntersect() {
        test(c -> {
            c.r(5, 10, 5, 10);
            c.r(25, 30, 25, 30);
        }, c -> c.r(1, 1, 1, 12), c -> c.r(14, 19, 25, 30));
    }

    /*
    [7, 24) - [7, 25), [33, 41) - [34, 47) ->
  [20, 25) - [20, 39), [38, 38) - [52, 60)
     */
    @Test
    public void testMixed1() {
        test(c -> {
            c.r(7, 25, 7, 24);
            c.r(34, 47, 33, 41);
        }, c -> {
            c.r(20, 39, 20, 25);
            c.r(52, 60, 38, 38);
        }, c -> {
            c.r(7, 20, 7, 24);
            c.r(48, 52, 33, 41);
            c.r(60, 69, 33, 41);
        });
    }

    @Test
    public void testRandom() throws Exception {
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i <= 100000; i++) {
            List<Range> early = buildFairRanges(2, random);
            List<Range> later = buildFairRanges(4, random);
            try {
                ExcludingApproximateChangedRangesShifter.shift(early, later);
            }
            catch (Throwable e) {
                throw new Exception(early + " -> " + later, e);
            }
        }
    }

    private static List<Range> buildFairRanges(int count, Random random) {
        List<Range> result = new ArrayList<>();
        int last1 = 0;
        int last2 = 0;
        for (int i = 0; i < count; i++) {
            int unchanged = random.nextInt(20) + 1;
            int start1 = last1 + unchanged;
            last1 = start1 + random.nextInt(20);
            int start2 = last2 + unchanged;
            last2 = start2 + random.nextInt(20);
            Range range = new Range(start1, last1, start2, last2);
            if (range.isEmpty()) {
                continue;
            }
            result.add(range);
        }
        DiffIterableUtil.setVerifyEnabled(true);
        DiffIterable iterable = DiffIterableUtil.fair(DiffIterableUtil.create(result, last1 + 10, last2 + 10));
        DiffIterableUtil.verifyFair(iterable);
        DiffIterableUtil.setVerifyEnabled(false);
        return result;
    }

    private static final class RangesCollector {
        private final List<Range> myRanges = new ArrayList<>();

        void r(int vcsStart, int vcsEnd, int start, int end) {
            myRanges.add(new Range(start, end, vcsStart, vcsEnd));
        }
    }

    private static void test(Consumer<RangesCollector> earlyBuilder, Consumer<RangesCollector> laterBuilder, Consumer<RangesCollector> resultBuilder) {
        RangesCollector early = new RangesCollector();
        earlyBuilder.accept(early);
        RangesCollector later = new RangesCollector();
        laterBuilder.accept(later);
        List<Range> result = ExcludingApproximateChangedRangesShifter.shift(early.myRanges, later.myRanges);
        RangesCollector expectedResult = new RangesCollector();
        resultBuilder.accept(expectedResult);
        assertEquals(expectedResult.myRanges, result);
    }
}
