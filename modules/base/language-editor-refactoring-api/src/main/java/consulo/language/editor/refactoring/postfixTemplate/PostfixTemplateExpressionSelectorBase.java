// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.postfixTemplate;

import consulo.document.Document;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.function.Predicates;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * Default abstract implementation of {@link PostfixTemplateExpressionSelector}.
 * You need impl method {@link #getNonFilteredExpressions}
 */
public abstract class PostfixTemplateExpressionSelectorBase implements PostfixTemplateExpressionSelector {

  @Nonnull
  protected final Predicate<? super PsiElement> myAdditionalCondition;

  public PostfixTemplateExpressionSelectorBase(@Nullable Predicate<? super PsiElement> condition) {
    myAdditionalCondition = condition != null ? condition : Predicates.alwaysTrue();
  }

  private static final Predicate<PsiElement> PSI_ERROR_FILTER = element -> !PsiTreeUtil.hasErrorElements(element);

  @Override
  public boolean hasExpression(@Nonnull PsiElement context,
                               @Nonnull Document copyDocument,
                               int newOffset) {
    return !getExpressions(context, copyDocument, newOffset).isEmpty();
  }

  protected Predicate<PsiElement> getBorderOffsetFilter(final int offset) {
    return element -> element.getTextRange().getEndOffset() == offset;
  }

  @Nonnull
  @Override
  public List<PsiElement> getExpressions(@Nonnull PsiElement context, @Nonnull Document document, int offset) {
    return ContainerUtil.filter(getNonFilteredExpressions(context, document, offset), getFilters(offset));
  }

  @Override
  @Nonnull
  public Function<PsiElement, String> getRenderer() {
    return PsiElement::getText;
  }

  protected abstract List<PsiElement> getNonFilteredExpressions(@Nonnull PsiElement context, @Nonnull Document document, int offset);

  protected Predicate<PsiElement> getFilters(int offset) {
    return getBorderOffsetFilter(offset).and(myAdditionalCondition);
  }

  protected Predicate<PsiElement> getPsiErrorFilter() {
    return PSI_ERROR_FILTER;
  }
}
