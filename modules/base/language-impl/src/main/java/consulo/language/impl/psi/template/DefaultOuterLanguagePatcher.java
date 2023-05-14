package consulo.language.impl.psi.template;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class DefaultOuterLanguagePatcher implements OuterLanguageRangePatcher {
  public static final String OUTER_EXPRESSION_PLACEHOLDER = "jbIdentifier6b52cc4b";

  @Override
  @Nullable
  public String getTextForOuterLanguageInsertionRange(@Nonnull TemplateDataElementType templateDataElementType, @Nonnull CharSequence outerElementText) {
    return OUTER_EXPRESSION_PLACEHOLDER;
  }
}
