// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.matcher;

import consulo.application.internal.InternalKeyboardLayoutUtil;

import org.jspecify.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 * @see NameUtil#buildMatcher(String)
 */
public class FixingLayoutMatcher extends MatcherWithFallback {
  public FixingLayoutMatcher(String pattern, NameUtil.MatchingCaseSensitivity options, String hardSeparators) {
    super(new MinusculeMatcherImpl(pattern, options, hardSeparators), withFixedLayout(pattern, options, hardSeparators));
  }

  public static @Nullable String fixLayout(String pattern) {
    boolean hasLetters = false;
    boolean onlyWrongLetters = true;
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (Character.isLetter(c)) {
        hasLetters = true;
        if (c <= '\u007f') {
          onlyWrongLetters = false;
          break;
        }
      }
    }

    if (hasLetters && onlyWrongLetters) {
      char[] alternatePattern = new char[pattern.length()];
      for (int i = 0; i < pattern.length(); i++) {
        char c = pattern.charAt(i);
        Character newC = InternalKeyboardLayoutUtil.getAsciiForChar(c);
        alternatePattern[i] = newC == null ? c : newC;
      }

      return new String(alternatePattern);
    }
    return null;
  }

  private static MinusculeMatcher withFixedLayout(String pattern, NameUtil.@Nullable MatchingCaseSensitivity options, String hardSeparators) {
    String s = fixLayout(pattern);
    if (s != null && !s.equals(pattern)) {
      return new MinusculeMatcherImpl(s, options, hardSeparators);
    }

    return null;
  }
}