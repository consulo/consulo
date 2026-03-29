// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.matcher;

import consulo.util.collection.FList;

import org.jspecify.annotations.Nullable;

class MatcherWithFallback extends MinusculeMatcher {
  private final MinusculeMatcher myMainMatcher;

  private final @Nullable MinusculeMatcher myFallbackMatcher;

  MatcherWithFallback(MinusculeMatcher mainMatcher, @Nullable MinusculeMatcher fallbackMatcher) {
    myMainMatcher = mainMatcher;
    myFallbackMatcher = fallbackMatcher;
  }

  @Override
  public String getPattern() {
    return myMainMatcher.getPattern();
  }

  @Override
  public boolean matches(String name) {
    return myMainMatcher.matches(name) || myFallbackMatcher != null && myFallbackMatcher.matches(name);
  }

  @Override
  public @Nullable FList<MatcherTextRange> matchingFragments(String name) {
    FList<MatcherTextRange> mainRanges = myMainMatcher.matchingFragments(name);
    return mainRanges != null && !mainRanges.isEmpty() || myFallbackMatcher == null
      ? mainRanges
      : myFallbackMatcher.matchingFragments(name);
  }

  @Override
  public int matchingDegree(String name, boolean valueStartCaseMatch, @Nullable FList<? extends MatcherTextRange> fragments) {
    FList<MatcherTextRange> mainRanges = myMainMatcher.matchingFragments(name);
    return mainRanges != null && !mainRanges.isEmpty() || myFallbackMatcher == null
      ? myMainMatcher.matchingDegree(name, valueStartCaseMatch, fragments)
      : myFallbackMatcher.matchingDegree(name, valueStartCaseMatch, fragments);
  }

  @Override
  public String toString() {
    return "MatcherWithFallback{" +
      "myMainMatcher=" + myMainMatcher +
      ", myFallbackMatcher=" + myFallbackMatcher +
      '}';
  }
}
