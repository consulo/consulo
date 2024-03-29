// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.matcher;

import consulo.util.collection.FList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link FixingLayoutMatcher} extension that returns all matches (not just the first one)
 * from {@link MinusculeMatcher#matchingFragments(String)}.
 */
public class AllOccurrencesMatcher extends MinusculeMatcher {
  private final MinusculeMatcher delegate;

  private AllOccurrencesMatcher(@Nonnull String pattern, @Nonnull NameUtil.MatchingCaseSensitivity options, String hardSeparators) {
    delegate = new FixingLayoutMatcher(pattern, options, hardSeparators);
  }

  @Nonnull
  @Override
  public String getPattern() {
    return delegate.getPattern();
  }

  @Override
  public int matchingDegree(@Nonnull String name, boolean valueStartCaseMatch, @Nullable FList<? extends MatcherTextRange> fragments) {
    return delegate.matchingDegree(name, valueStartCaseMatch, fragments);
  }

  @Nullable
  @Override
  public FList<MatcherTextRange> matchingFragments(@Nonnull String name) {
    FList<MatcherTextRange> match = delegate.matchingFragments(name);
    if (match != null && !match.isEmpty()) {
      List<FList<MatcherTextRange>> allMatchesReversed = new ArrayList<>();
      int lastOffset = 0;
      while (match != null && !match.isEmpty()) {
        FList<MatcherTextRange> reversedWithAbsoluteOffsets = FList.emptyList();
        for (MatcherTextRange r : match) {
          reversedWithAbsoluteOffsets = reversedWithAbsoluteOffsets.prepend(r.shiftRight(lastOffset));
        }
        allMatchesReversed.add(reversedWithAbsoluteOffsets);
        lastOffset = reversedWithAbsoluteOffsets.get(0).getEndOffset();
        match = delegate.matchingFragments(name.substring(lastOffset));
      }
      match = FList.emptyList();
      for (int i = allMatchesReversed.size() - 1; i >= 0; i--) {
        for (MatcherTextRange range : allMatchesReversed.get(i)) {
          match = match.prepend(range);
        }
      }
    }
    return match;
  }

  @Override
  public String toString() {
    return "AllOccurrencesMatcher{" + "delegate=" + delegate + '}';
  }

  public static MinusculeMatcher create(@Nonnull String pattern, @Nonnull NameUtil.MatchingCaseSensitivity options, String hardSeparators) {
    return new AllOccurrencesMatcher(pattern, options, hardSeparators);
  }
}
