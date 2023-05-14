// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.matcher;

import consulo.util.collection.FList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

class MatcherWithFallback extends MinusculeMatcher {
  @Nonnull
  private final MinusculeMatcher myMainMatcher;

  @Nullable
  private final MinusculeMatcher myFallbackMatcher;

  MatcherWithFallback(@Nonnull MinusculeMatcher mainMatcher, @Nullable MinusculeMatcher fallbackMatcher) {
    myMainMatcher = mainMatcher;
    myFallbackMatcher = fallbackMatcher;
  }

  @Nonnull
  @Override
  public String getPattern() {
    return myMainMatcher.getPattern();
  }

  @Override
  public boolean matches(@Nonnull String name) {
    return myMainMatcher.matches(name) || myFallbackMatcher != null && myFallbackMatcher.matches(name);
  }

  @Nullable
  @Override
  public FList<MatcherTextRange> matchingFragments(@Nonnull String name) {
    FList<MatcherTextRange> mainRanges = myMainMatcher.matchingFragments(name);
    boolean useMainRanges = mainRanges != null && !mainRanges.isEmpty() || myFallbackMatcher == null;

    return useMainRanges ? mainRanges : myFallbackMatcher.matchingFragments(name);
  }

  @Override
  public int matchingDegree(@Nonnull String name, boolean valueStartCaseMatch, @Nullable FList<? extends MatcherTextRange> fragments) {
    FList<MatcherTextRange> mainRanges = myMainMatcher.matchingFragments(name);
    boolean useMainRanges = mainRanges != null && !mainRanges.isEmpty() || myFallbackMatcher == null;

    return useMainRanges ? myMainMatcher.matchingDegree(name, valueStartCaseMatch, fragments) : myFallbackMatcher.matchingDegree(name, valueStartCaseMatch, fragments);
  }

  @Override
  public String toString() {
    return "MatcherWithFallback{" + "myMainMatcher=" + myMainMatcher + ", myFallbackMatcher=" + myFallbackMatcher + '}';
  }
}
