// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.matcher;

import consulo.util.collection.FList;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class PreferStartMatchMatcherWrapper extends MinusculeMatcher {
  public static final int START_MATCH_WEIGHT = 10000;
  
  private final MinusculeMatcher myDelegateMatcher;

  public PreferStartMatchMatcherWrapper(MinusculeMatcher matcher) {
    myDelegateMatcher = matcher;
  }

  @Override
  
  public String getPattern() {
    return myDelegateMatcher.getPattern();
  }

  @Override
  public @Nullable FList<MatcherTextRange> matchingFragments(String name) {
    return myDelegateMatcher.matchingFragments(name);
  }

  @Override
  public int matchingDegree(String name, boolean valueStartCaseMatch, @Nullable FList<? extends MatcherTextRange> fragments) {
    int degree = myDelegateMatcher.matchingDegree(name, valueStartCaseMatch, fragments);
    if (fragments == null || fragments.isEmpty()) return degree;

    if (Objects.requireNonNull(fragments.getHead()).getStartOffset() == 0) {
      degree += START_MATCH_WEIGHT;
    }
    return degree;
  }
}
