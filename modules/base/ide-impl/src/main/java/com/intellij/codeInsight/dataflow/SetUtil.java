package com.intellij.codeInsight.dataflow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author oleg
 */
public class SetUtil {
  private SetUtil() {
  }

  /**
   * Intersects two sets
   */
  @Nonnull
  public static <T> Set<T> intersect(@Nullable Set<T> set1, @Nullable Set<T> set2) {
    if (set1 == null && set2 == null) {
      return Collections.emptySet();
    }
    if (set1 == null) {
      return set2;
    }
    if (set2 == null) {
      return set1;
    }
    if (set1.equals(set2)) {
      return set1;
    }
    Set<T> result = new HashSet<T>();
    Set<T> minSet;
    Set<T> otherSet;
    if (set1.size() < set2.size()) {
      minSet = set1;
      otherSet = set2;
    }
    else {
      minSet = set2;
      otherSet = set1;
    }
    for (T s : minSet) {
      if (otherSet.contains(s)) {
        result.add(s);
      }
    }
    return result;
  }
}
