// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.matcher;

class FixingLayoutTypoTolerantMatcher {
  static MinusculeMatcher create(String pattern, NameUtil.MatchingCaseSensitivity options, String hardSeparators) {
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