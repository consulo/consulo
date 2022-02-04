package com.intellij.codeInsight.completion;

import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class PlainPrefixMatcher extends PrefixMatcher {

  public PlainPrefixMatcher(String prefix) {
    super(prefix);
  }

  @Override
  public boolean isStartMatch(String name) {
    return StringUtil.startsWithIgnoreCase(name, getPrefix());
  }

  @Override
  public boolean prefixMatches(@Nonnull String name) {
    return StringUtil.containsIgnoreCase(name, getPrefix());
  }

  @Nonnull
  @Override
  public PrefixMatcher cloneWithPrefix(@Nonnull String prefix) {
    return new PlainPrefixMatcher(prefix);
  }
}
