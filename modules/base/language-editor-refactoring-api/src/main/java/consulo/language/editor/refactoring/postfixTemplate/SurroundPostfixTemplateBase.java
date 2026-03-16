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
import org.jspecify.annotations.Nullable;

/**
 * Base for surrounding postfix templates that utilize existing {@link Surrounder} implementations.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/advanced-postfix-templates.html">Advanced Postfix Templates (IntelliJ Platform Docs)</a>
 */
public abstract class SurroundPostfixTemplateBase extends PostfixTemplateWithExpressionSelector {

  
  protected final PostfixTemplatePsiInfo myPsiInfo;

  /**
   * @deprecated use {@link #SurroundPostfixTemplateBase(String, String, PostfixTemplatePsiInfo, PostfixTemplateExpressionSelector, PostfixTemplateProvider)}
   */
  @Deprecated(forRemoval = true)
  protected SurroundPostfixTemplateBase(String name,
                                        String descr,
                                        PostfixTemplatePsiInfo psiInfo,
                                        PostfixTemplateExpressionSelector selector) {
    this(name, descr, psiInfo, selector, null);
  }

  protected SurroundPostfixTemplateBase(String name,
                                        String descr,
                                        PostfixTemplatePsiInfo psiInfo,
                                        PostfixTemplateExpressionSelector selector,
                                        @Nullable PostfixTemplateProvider provider) {
    super(null, name, descr, selector, provider);
    myPsiInfo = psiInfo;
  }

  @Override
  public final void expandForChooseExpression(PsiElement expression, Editor editor) {
    PsiElement replace = getReplacedExpression(expression);
    TextRange range = PostfixTemplatesUtils.surround(getSurrounder(), editor, replace);

    if (range != null) {
      editor.getCaretModel().moveToOffset(range.getStartOffset());
    }
  }

  protected PsiElement getReplacedExpression(PsiElement expression) {
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

  
  protected String getHead() {
    return "";
  }

  
  protected String getTail() {
    return "";
  }

  
  protected abstract Surrounder getSurrounder();
}

