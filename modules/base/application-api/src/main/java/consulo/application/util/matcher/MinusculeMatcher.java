// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.matcher;

import consulo.util.collection.FList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Iterator;

/**
 * Tells whether a string matches a specific pattern. Allows for lowercase camel-hump matching.
 * Used in navigation, code completion, speed search etc.
 *
 * @see NameUtil#buildMatcher(String)
 * <p>
 * Inheritors MUST override `matchingFragments` and `matchingDegree` methods,
 * they are not abstract for binary compatibility.
 */
abstract public class MinusculeMatcher implements Matcher {

  MinusculeMatcher() {
  }

  @Nonnull
  abstract public String getPattern();

  @Override
  public boolean matches(@Nonnull String name) {
    return matchingFragments(name) != null;
  }

  public FList<MatcherTextRange> matchingFragments(@Nonnull String name) {
    throw new UnsupportedOperationException();
  }

  public int matchingDegree(@Nonnull String name, boolean valueStartCaseMatch, @Nullable FList<? extends MatcherTextRange> fragments) {
    throw new UnsupportedOperationException();
  }

  public int matchingDegree(@Nonnull String name, boolean valueStartCaseMatch) {
    return matchingDegree(name, valueStartCaseMatch, matchingFragments(name));
  }

  public int matchingDegree(@Nonnull String name) {
    return matchingDegree(name, false);
  }

  public boolean isStartMatch(@Nonnull String name) {
    FList<MatcherTextRange> fragments = matchingFragments(name);
    return fragments != null && isStartMatch(fragments);
  }

  public static boolean isStartMatch(@Nonnull Iterable<? extends MatcherTextRange> fragments) {
    Iterator<? extends MatcherTextRange> iterator = fragments.iterator();
    return !iterator.hasNext() || iterator.next().getStartOffset() == 0;
  }
}
