// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import com.intellij.diff.comparison.iterables.DiffIterableUtil;
import com.intellij.diff.util.Range;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ExcludingApproximateChangedRangesShifterTest {
  @Test
  public void testEmpty() {
    test(List.of(), List.of(), List.of());
  }

  @Test
  public void testUnchanged() {
    List<Range> early = List.of(
      new Range(0, 0, 0, 5),
      new Range(1, 5, 5, 10)
    );
    test(early, List.of(), early);
  }

  @Test
  public void testNoEarly() {
    test(List.of(), List.of(
      new Range(0, 0, 0, 5),
      new Range(1, 5, 5, 10)
    ), List.of());
  }

  @Test
  public void testBeginningIntersection() {
    test(
      List.of(new Range(5, 10, 5, 10)),
      List.of(new Range(4, 7, 4, 7)),
      List.of(new Range(5, 10, 7, 10))
    );
  }

  @Test
  public void testEndIntersection() {
    test(
      List.of(new Range(5, 10, 5, 10)),
      List.of(new Range(7, 12, 7, 12)),
      List.of(new Range(5, 10, 5, 7))
    );
  }

  @Test
  public void testEarlyInside() {
    test(
      List.of(new Range(5, 10, 5, 10)),
      List.of(new Range(4, 12, 4, 12)),
      List.of()
    );
  }

  @Test
  public void testLaterInside() {
    test(
      List.of(new Range(5, 10, 5, 10)),
      List.of(new Range(6, 8, 6, 6)),
      List.of(
        new Range(5, 10, 5, 6),
        new Range(5, 10, 6, 8)
      )
    );
  }

  @Test
  public void testLaterInsideAndNoIntersect() {
    test(
      List.of(
        new Range(5, 10, 5, 10),
        new Range(25, 30, 25, 30)
      ),
      List.of(new Range(6, 8, 6, 6)),
      List.of(
        new Range(5, 10, 5, 6),
        new Range(5, 10, 6, 8),
        new Range(25, 30, 23, 28)
      )
    );
  }

  @Test
  public void testEarlyInsideAndNoIntersect() {
    test(
      List.of(
        new Range(5, 10, 5, 10),
        new Range(25, 30, 25, 30)
      ),
      List.of(new Range(1, 12, 1, 1)),
      List.of(
        new Range(25, 30, 14, 19)
      )
    );
  }

  /*
  [7, 24) - [7, 25), [33, 41) - [34, 47) ->
[20, 25) - [20, 39), [38, 38) - [52, 60)
   */
  @Test
  public void testMixed1() {
    test(
      List.of(
        new Range(7, 24, 7, 25),
        new Range(33, 41, 34, 47)
      ),
      List.of(
        new Range(20, 25, 20, 39),
        new Range(38, 38, 52, 60)
      ),
      List.of(
        new Range(7, 24, 7, 20),
        new Range(33, 41, 48, 52),
        new Range(33, 41, 60, 69)
      )
    );
  }

  @Test
  public void testRandom() {
    Random random = new Random(System.currentTimeMillis());
    for (int i = 0; i <= 100000; i++) {
      List<Range> early = buildFairRanges(2, random);
      List<Range> later = buildFairRanges(4, random);
      try {
        ExcludingApproximateChangedRangesShifter.shift(early, later);
      }
      catch (Throwable e) {
        throw new RuntimeException(early + " -> " + later, e);
      }
    }
  }

  private List<Range> buildFairRanges(int count, Random random) {
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
      if (range.isEmpty()) continue;  // Kotlin property isEmpty -> isEmpty() in Java
      result.add(range);
    }
    DiffIterableUtil.setVerifyEnabled(true);
    var iterable = DiffIterableUtil.fair(DiffIterableUtil.create(result, last1 + 10, last2 + 10));
    DiffIterableUtil.verifyFair(iterable);
    DiffIterableUtil.setVerifyEnabled(false);
    return result;
  }

  private void test(List<Range> early, List<Range> later, List<Range> expectedResult) {
    // The RangesCollector in the original Kotlin uses r(vcsStart, vcsEnd, start, end)
    // and creates Range(start, end, vcsStart, vcsEnd)
    // But in our test, we pass ranges directly using the constructor order
    List<Range> result = ExcludingApproximateChangedRangesShifter.shift(early, later);
    assertEquals(expectedResult, result);
  }
}
