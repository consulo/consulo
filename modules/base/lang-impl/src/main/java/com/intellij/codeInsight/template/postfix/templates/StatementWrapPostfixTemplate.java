/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;


public abstract class StatementWrapPostfixTemplate extends TypedPostfixTemplate {

  @SuppressWarnings("unchecked")
  protected StatementWrapPostfixTemplate(@Nonnull String name,
                                         @Nonnull String descr,
                                         @Nonnull PostfixTemplatePsiInfo psiInfo) {
    super(name, descr, psiInfo, Condition.TRUE);
  }

  protected StatementWrapPostfixTemplate(@Nonnull String name,
                                         @Nonnull String descr,
                                         @Nonnull PostfixTemplatePsiInfo psiInfo,
                                         @Nonnull Condition<PsiElement> typeChecker) {
    super(name, descr, psiInfo, typeChecker);
  }

  @Override
  public void expand(@Nonnull PsiElement context, @Nonnull Editor editor) {
    PsiElement topmostExpression = myPsiInfo.getTopmostExpression(context);
    assert topmostExpression != null;
    PsiElement parent = topmostExpression.getParent();
    PsiElement expression = getWrappedExpression(topmostExpression);
    PsiElement replace = parent.replace(expression);
    afterExpand(replace, editor);
  }

  protected PsiElement getWrappedExpression(PsiElement expression) {
    if (StringUtil.isEmpty(getHead()) && StringUtil.isEmpty(getTail())) {
      return expression;
    }
    return createNew(expression);
  }

  protected PsiElement createNew(PsiElement expression) {
    if (isStatement()) {
      return myPsiInfo.createStatement(expression, getHead(), getTail());
    }
    return myPsiInfo.createExpression(expression, getHead(), getTail());
  }

  protected void afterExpand(@Nonnull PsiElement newElement, @Nonnull Editor editor) {
    editor.getCaretModel().moveToOffset(newElement.getTextRange().getEndOffset());
  }

  @Nonnull
  protected String getHead() {
    return "";
  }

  @Nonnull
  protected String getTail() {
    return "";
  }

  public boolean isStatement() {
    return true;
  }
}
