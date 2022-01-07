// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle;

import javax.annotation.Nonnull;

class FixingLayoutTypoTolerantMatcher {
  static MinusculeMatcher create(@Nonnull String pattern, @Nonnull NameUtil.MatchingCaseSensitivity options, String hardSeparators) {
    TypoTolerantMatcher mainMatcher = new TypoTolerantMatcher(pattern, options, hardSeparators);
    String s = FixingLayoutMatcher.fixLayout(pattern);

    if (s != null && !s.equals(pattern)) {
      TypoTolerantMatcher fallbackMatcher = new TypoTolerantMatcher(s, options, hardSeparators);
      return new MatcherWithFallback(mainMatcher, fallbackMatcher);
    }
    else {
      return mainMatcher;
    }
  }
}