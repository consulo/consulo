package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import javax.annotation.Nonnull;

/**
 * @author peter
 */
public abstract class PrefixMatcher {
  public static final PrefixMatcher ALWAYS_TRUE = new PlainPrefixMatcher("");
  protected final String myPrefix;

  protected PrefixMatcher(String prefix) {
    myPrefix = prefix;
  }

  public boolean prefixMatches(@Nonnull LookupElement element) {
    for (String s : element.getAllLookupStrings()) {
      if (prefixMatches(s)) {
        return true;
      }
    }
    return false;
  }

  public boolean isStartMatch(LookupElement element) {
    for (String s : element.getAllLookupStrings()) {
      if (isStartMatch(s)) {
        return true;
      }
    }
    return false;
  }

  public boolean isStartMatch(String name) {
    return prefixMatches(name);
  }

  public abstract boolean prefixMatches(@Nonnull String name);

  @Nonnull
  public final String getPrefix() {
    return myPrefix;
  }

  @Nonnull
  public abstract PrefixMatcher cloneWithPrefix(@Nonnull String prefix);

  public int matchingDegree(String string) {
    return 0;
  }
}
