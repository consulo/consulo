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
package consulo.language.editor.refactoring.postfixTemplate;

import consulo.codeEditor.Editor;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.postfixTemplate.PostfixTemplatePsiInfo;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class NotPostfixTemplate extends PostfixTemplateWithExpressionSelector {

  @Nonnull
  private final PostfixTemplatePsiInfo myPsiInfo;

  /**
   * @deprecated use {@link #NotPostfixTemplate(PostfixTemplatePsiInfo, PostfixTemplateExpressionSelector, PostfixTemplateProvider)}
   */
  @Deprecated(forRemoval = true)
  public NotPostfixTemplate(@Nonnull PostfixTemplatePsiInfo info,
                            @Nonnull PostfixTemplateExpressionSelector selector) {
    this(info, selector, null);
  }

  public NotPostfixTemplate(@Nonnull PostfixTemplatePsiInfo info,
                            @Nonnull PostfixTemplateExpressionSelector selector,
                            @Nullable PostfixTemplateProvider provider) {
    super(null, "not", "!expr", selector, provider);
    myPsiInfo = info;
  }

  /**
   * @deprecated use {@link #NotPostfixTemplate(String, String, String, PostfixTemplatePsiInfo, PostfixTemplateExpressionSelector, PostfixTemplateProvider)}
   */
  @Deprecated(forRemoval = true)
  public NotPostfixTemplate(@Nonnull String name,
                            @Nonnull String key,
                            @Nonnull String example,
                            @Nonnull PostfixTemplatePsiInfo info,
                            @Nonnull PostfixTemplateExpressionSelector selector) {
    super(name, key, example, selector);
    myPsiInfo = info;
  }

  public NotPostfixTemplate(@Nullable String id,
                            @Nonnull String name,
                            @Nonnull String example,
                            @Nonnull PostfixTemplatePsiInfo info,
                            @Nonnull PostfixTemplateExpressionSelector selector,
                            @Nullable PostfixTemplateProvider provider) {
    super(id, name, example, selector, provider);
    myPsiInfo = info;
  }

  @Override
  protected void expandForChooseExpression(@Nonnull PsiElement expression, @Nonnull Editor editor) {
    PsiElement element = myPsiInfo.getNegatedExpression(expression);
    expression.replace(element);
  }
}