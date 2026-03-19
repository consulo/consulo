package consulo.language.impl.psi.template;

import org.jspecify.annotations.Nullable;

public abstract class DefaultOuterLanguagePatcher implements OuterLanguageRangePatcher {
  public static final String OUTER_EXPRESSION_PLACEHOLDER = "jbIdentifier6b52cc4b";

  @Override
  public @Nullable String getTextForOuterLanguageInsertionRange(TemplateDataElementType templateDataElementType, CharSequence outerElementText) {
    return OUTER_EXPRESSION_PLACEHOLDER;
  }
}
