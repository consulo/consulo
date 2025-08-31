// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.completion;

import consulo.application.util.matcher.*;
import consulo.application.util.registry.Registry;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.CodeInsightSettings;
import consulo.util.collection.FList;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class CamelHumpMatcher extends PrefixMatcher {
  private final MinusculeMatcher myMatcher;
  private final MinusculeMatcher myCaseInsensitiveMatcher;
  private final boolean myCaseSensitive;
  private static boolean ourForceStartMatching;
  private final boolean myTypoTolerant;


  public CamelHumpMatcher(@Nonnull String prefix) {
    this(prefix, true);
  }

  public CamelHumpMatcher(String prefix, boolean caseSensitive) {
    this(prefix, caseSensitive, false);
  }

  public CamelHumpMatcher(String prefix, boolean caseSensitive, boolean typoTolerant) {
    super(prefix);
    myCaseSensitive = caseSensitive;
    myTypoTolerant = typoTolerant;
    myMatcher = createMatcher(myCaseSensitive);
    myCaseInsensitiveMatcher = createMatcher(false);
  }

  @Override
  public boolean isStartMatch(String name) {
    return myMatcher.isStartMatch(name);
  }

  @Override
  public boolean isStartMatch(CompositeStringHolder element) {
    for (String s : CompletionUtilCore.iterateLookupStrings(element)) {
      FList<MatcherTextRange> ranges = myCaseInsensitiveMatcher.matchingFragments(s);
      if (ranges == null) continue;
      if (ranges.isEmpty() || skipUnderscores(s) >= ranges.get(0).getStartOffset()) {
        return true;
      }
    }

    return false;
  }

  public boolean isTypoTolerant() {
    return myTypoTolerant;
  }

  private static int skipUnderscores(@Nonnull String name) {
    return CharArrayUtil.shiftForward(name, 0, "_");
  }

  @Override
  public boolean prefixMatches(@Nonnull String name) {
    if (name.startsWith("_") && CodeInsightSettings.getInstance().COMPLETION_CASE_SENSITIVE == CodeInsightSettings.FIRST_LETTER && firstLetterCaseDiffers(name)) {
      return false;
    }

    return myMatcher.matches(name);
  }

  private boolean firstLetterCaseDiffers(String name) {
    int nameFirst = skipUnderscores(name);
    int prefixFirst = skipUnderscores(myPrefix);
    return nameFirst < name.length() && prefixFirst < myPrefix.length() && caseDiffers(name.charAt(nameFirst), myPrefix.charAt(prefixFirst));
  }

  private static boolean caseDiffers(char c1, char c2) {
    return Character.isLowerCase(c1) != Character.isLowerCase(c2) || Character.isUpperCase(c1) != Character.isUpperCase(c2);
  }

  @Override
  public boolean prefixMatches(@Nonnull CompositeStringHolder element) {
    return prefixMatchersInternal(element, !element.isCaseSensitive());
  }

  private boolean prefixMatchersInternal(CompositeStringHolder element, boolean itemCaseInsensitive) {
    for (String name : element.getAllStrings()) {
      if (itemCaseInsensitive && StringUtil.startsWithIgnoreCase(name, myPrefix) || prefixMatches(name)) {
        return true;
      }
      if (itemCaseInsensitive && CodeInsightSettings.ALL != CodeInsightSettings.getInstance().COMPLETION_CASE_SENSITIVE) {
        if (myCaseInsensitiveMatcher.matches(name)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  @Nonnull
  public PrefixMatcher cloneWithPrefix(@Nonnull String prefix) {
    if (prefix.equals(myPrefix)) {
      return this;
    }

    return new CamelHumpMatcher(prefix, myCaseSensitive, myTypoTolerant);
  }

  private MinusculeMatcher createMatcher(boolean caseSensitive) {
    String prefix = applyMiddleMatching(myPrefix);

    NameUtil.MatcherBuilder builder = NameUtil.buildMatcher(prefix);
    if (caseSensitive) {
      int setting = CodeInsightSettings.getInstance().COMPLETION_CASE_SENSITIVE;
      if (setting == CodeInsightSettings.FIRST_LETTER) {
        builder = builder.withCaseSensitivity(NameUtil.MatchingCaseSensitivity.FIRST_LETTER);
      }
      else if (setting == CodeInsightSettings.ALL) {
        builder = builder.withCaseSensitivity(NameUtil.MatchingCaseSensitivity.ALL);
      }
    }
    if (myTypoTolerant) {
      builder = builder.typoTolerant();
    }
    return builder.build();
  }

  public static String applyMiddleMatching(String prefix) {
    if (Registry.is("ide.completion.middle.matching") && !prefix.isEmpty() && !ourForceStartMatching) {
      return "*" + StringUtil.replace(prefix, ".", ". ").trim();
    }
    return prefix;
  }

  @Override
  public String toString() {
    return myPrefix;
  }

  /**
   * @deprecated In an ideal world, all tests would use the same settings as production, i.e. middle matching.
   * If you see a usage of this method which can be easily removed (i.e. it's easy to make a test pass without it
   * by modifying test expectations slightly), please do it
   */
  @TestOnly
  @Deprecated
  public static void forceStartMatching(Disposable parent) {
    ourForceStartMatching = true;
    Disposer.register(parent, new Disposable() {
      @Override
      public void dispose() {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        ourForceStartMatching = false;
      }
    });
  }

  @Override
  public int matchingDegree(String string) {
    return matchingDegree(string, matchingFragments(string));
  }

  @Nullable
  public FList<MatcherTextRange> matchingFragments(String string) {
    return myMatcher.matchingFragments(string);
  }

  public int matchingDegree(String string, @Nullable FList<? extends MatcherTextRange> fragments) {
    int underscoreEnd = skipUnderscores(string);
    if (underscoreEnd > 0) {
      FList<MatcherTextRange> ciRanges = myCaseInsensitiveMatcher.matchingFragments(string);
      if (ciRanges != null && !ciRanges.isEmpty()) {
        int matchStart = ciRanges.get(0).getStartOffset();
        if (matchStart > 0 && matchStart <= underscoreEnd) {
          return myCaseInsensitiveMatcher.matchingDegree(string.substring(matchStart), true) - 1;
        }
      }
    }

    return myMatcher.matchingDegree(string, true, fragments);
  }
}
