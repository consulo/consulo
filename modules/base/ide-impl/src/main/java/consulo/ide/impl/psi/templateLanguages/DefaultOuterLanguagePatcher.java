package consulo.ide.impl.psi.templateLanguages;

import consulo.language.impl.psi.template.TemplateDataElementType;

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
