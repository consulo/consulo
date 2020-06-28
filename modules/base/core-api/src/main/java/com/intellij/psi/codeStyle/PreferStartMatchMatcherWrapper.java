// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle;

import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.FList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PreferStartMatchMatcherWrapper extends MinusculeMatcher {
  public static final int START_MATCH_WEIGHT = 10000;
  @Nonnull
  private final MinusculeMatcher myDelegateMatcher;

  public PreferStartMatchMatcherWrapper(@Nonnull MinusculeMatcher matcher) {
    myDelegateMatcher = matcher;
  }

  @Override
  @Nonnull
  public String getPattern() {
    return myDelegateMatcher.getPattern();
  }

  @Override
  public FList<TextRange> matchingFragments(@Nonnull String name) {
    return myDelegateMatcher.matchingFragments(name);
  }

  @Override
  public int matchingDegree(@Nonnull String name, boolean valueStartCaseMatch, @Nullable FList<? extends TextRange> fragments) {
    int degree = myDelegateMatcher.matchingDegree(name, valueStartCaseMatch, fragments);
    if (fragments == null || fragments.isEmpty()) return degree;

    if (fragments.getHead().getStartOffset() == 0) {
      degree += START_MATCH_WEIGHT;
    }
    return degree;
  }
}
