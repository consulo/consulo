// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.postfixTemplate;

import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.postfixTemplate.PostfixTemplatePsiInfo;
import consulo.language.editor.postfixTemplate.PostfixTemplatesUtils;
import consulo.language.editor.surroundWith.Surrounder;
import consulo.language.psi.PsiElement;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base for surrounding postfix templates that utilize existing {@link Surrounder} implementations.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/advanced-postfix-templates.html">Advanced Postfix Templates (IntelliJ Platform Docs)</a>
 */
public abstract class SurroundPostfixTemplateBase extends PostfixTemplateWithExpressionSelector {

  @Nonnull
  protected final PostfixTemplatePsiInfo myPsiInfo;

  /**
   * @deprecated use {@link #SurroundPostfixTemplateBase(String, String, PostfixTemplatePsiInfo, PostfixTemplateExpressionSelector, PostfixTemplateProvider)}
   */
  @Deprecated(forRemoval = true)
  protected SurroundPostfixTemplateBase(@Nonnull String name,
                                        @Nonnull String descr,
                                        @Nonnull PostfixTemplatePsiInfo psiInfo,
                                        @Nonnull PostfixTemplateExpressionSelector selector) {
    this(name, descr, psiInfo, selector, null);
  }

  protected SurroundPostfixTemplateBase(@Nonnull String name,
                                        @Nonnull String descr,
                                        @Nonnull PostfixTemplatePsiInfo psiInfo,
                                        @Nonnull PostfixTemplateExpressionSelector selector,
                                        @Nullable PostfixTemplateProvider provider) {
    super(null, name, descr, selector, provider);
    myPsiInfo = psiInfo;
  }

  @Override
  public final void expandForChooseExpression(@Nonnull PsiElement expression, @Nonnull final Editor editor) {
    PsiElement replace = getReplacedExpression(expression);
    TextRange range = PostfixTemplatesUtils.surround(getSurrounder(), editor, replace);

    if (range != null) {
      editor.getCaretModel().moveToOffset(range.getStartOffset());
    }
  }

  protected PsiElement getReplacedExpression(@Nonnull PsiElement expression) {
    PsiElement wrappedExpression = getWrappedExpression(expression);
    return expression.replace(wrappedExpression);
  }

  protected PsiElement getWrappedExpression(PsiElement expression) {
    if (StringUtil.isEmpty(getHead()) && StringUtil.isEmpty(getTail())) {
      return expression;
    }
    return createNew(expression);
  }

  protected PsiElement createNew(PsiElement expression) {
    return myPsiInfo.createExpression(expression, getHead(), getTail());
  }

  @Nonnull
  protected String getHead() {
    return "";
  }

  @Nonnull
  protected String getTail() {
    return "";
  }

  @Nonnull
  protected abstract Surrounder getSurrounder();
}

