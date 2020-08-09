package com.intellij.psi.templateLanguages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DefaultOuterLanguagePatcher implements TemplateDataElementType.OuterLanguageRangePatcher {
  public static final String OUTER_EXPRESSION_PLACEHOLDER = "jbIdentifier6b52cc4b";

  @Override
  @Nullable
  public String getTextForOuterLanguageInsertionRange(@Nonnull TemplateDataElementType templateDataElementType, @Nonnull CharSequence outerElementText) {
    return OUTER_EXPRESSION_PLACEHOLDER;
  }
}
