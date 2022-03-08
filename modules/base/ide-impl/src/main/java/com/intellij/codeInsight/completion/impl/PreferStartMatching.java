package com.intellij.codeInsight.completion.impl;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementWeigher;
import consulo.language.editor.completion.WeighingContext;
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
