package com.intellij.codeInsight.completion.impl;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementWeigher;
import com.intellij.codeInsight.lookup.WeighingContext;
import javax.annotation.Nonnull;

/**
 * @author Peter
 */
public class PreferStartMatching extends LookupElementWeigher {

  public PreferStartMatching() {
    super("middleMatching", false, true);
  }

  @Override
  public Comparable weigh(@Nonnull LookupElement element, @Nonnull WeighingContext context) {
    return !CompletionServiceImpl.isStartMatch(element, context);
  }
}
