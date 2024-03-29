/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.codeInsight.completion.impl;

import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.editor.completion.CompletionResult;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.util.collection.FList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class BetterPrefixMatcher extends PrefixMatcher {
  private final PrefixMatcher myOriginal;
  @Nullable private final CamelHumpMatcher myHumpMatcher;
  private final int myMinMatchingDegree;

  public BetterPrefixMatcher(PrefixMatcher original, int minMatchingDegree) {
    super(original.getPrefix());
    myOriginal = original;
    myHumpMatcher = original instanceof CamelHumpMatcher ? (CamelHumpMatcher)original : null;
    myMinMatchingDegree = minMatchingDegree;
  }

  @Nonnull
  public BetterPrefixMatcher improve(CompletionResult result) {
    int degree = RealPrefixMatchingWeigher.getBestMatchingDegree(result.getLookupElement(), result.getPrefixMatcher());
    if (degree <= myMinMatchingDegree) return this;

    return createCopy(myOriginal, degree);
  }

  @Nonnull
  protected BetterPrefixMatcher createCopy(PrefixMatcher original, int degree) {
    return new BetterPrefixMatcher(original, degree);
  }

  @Override
  public boolean prefixMatches(@Nonnull String name) {
    return prefixMatchesEx(name) == MatchingOutcome.BETTER_MATCH;
  }

  protected MatchingOutcome prefixMatchesEx(String name) {
    return myHumpMatcher != null ? matchOptimized(name, myHumpMatcher) : matchGeneric(name);
  }

  private MatchingOutcome matchGeneric(String name) {
    if (!myOriginal.prefixMatches(name)) return MatchingOutcome.NON_MATCH;
    if (!myOriginal.isStartMatch(name)) return MatchingOutcome.WORSE_MATCH;
    return myOriginal.matchingDegree(name) >= myMinMatchingDegree ? MatchingOutcome.BETTER_MATCH : MatchingOutcome.WORSE_MATCH;
  }

  private MatchingOutcome matchOptimized(String name, CamelHumpMatcher matcher) {
    FList<MatcherTextRange> fragments = matcher.matchingFragments(name);
    if (fragments == null) return MatchingOutcome.NON_MATCH;
    if (!MinusculeMatcher.isStartMatch(fragments)) return MatchingOutcome.WORSE_MATCH;
    return matcher.matchingDegree(name, fragments) >= myMinMatchingDegree ? MatchingOutcome.BETTER_MATCH : MatchingOutcome.WORSE_MATCH;
  }

  protected enum MatchingOutcome {
    NON_MATCH, WORSE_MATCH, BETTER_MATCH
  }

  @Override
  public boolean isStartMatch(String name) {
    return myOriginal.isStartMatch(name);
  }

  @Override
  public int matchingDegree(String string) {
    return myOriginal.matchingDegree(string);
  }

  @Nonnull
  @Override
  public PrefixMatcher cloneWithPrefix(@Nonnull String prefix) {
    return createCopy(myOriginal.cloneWithPrefix(prefix), myMinMatchingDegree);
  }

  public static class AutoRestarting extends BetterPrefixMatcher {
    private final CompletionResultSet myResult;

    public AutoRestarting(@Nonnull CompletionResultSet result) {
      this(result, result.getPrefixMatcher(), Integer.MIN_VALUE);
    }

    private AutoRestarting(CompletionResultSet result, PrefixMatcher original, int minMatchingDegree) {
      super(original, minMatchingDegree);
      myResult = result;
    }

    @Nonnull
    @Override
    protected BetterPrefixMatcher createCopy(PrefixMatcher original, int degree) {
      return new AutoRestarting(myResult, original, degree);
    }

    @Override
    protected MatchingOutcome prefixMatchesEx(String name) {
      MatchingOutcome outcome = super.prefixMatchesEx(name);
      if (outcome == MatchingOutcome.WORSE_MATCH) {
        myResult.restartCompletionOnAnyPrefixChange();
      }
      return outcome;
    }
  }
}
