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
package consulo.language.editor.postfixTemplate;

import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.surroundWith.Surrounder;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import java.util.function.Predicate;

public abstract class SurroundPostfixTemplateBase extends StatementWrapPostfixTemplate {

  protected SurroundPostfixTemplateBase(@Nonnull String name,
                                        @Nonnull String descr,
                                        @Nonnull PostfixTemplatePsiInfo psiInfo,
                                        @Nonnull Predicate<PsiElement> typeChecker) {
    super(name, descr, psiInfo, typeChecker);
  }


  @Override
  public void expand(@Nonnull PsiElement context, @Nonnull final Editor editor) {
    PsiElement topmostExpression = myPsiInfo.getTopmostExpression(context);
    PsiElement expression = getWrappedExpression(topmostExpression);
    assert topmostExpression != null;
    PsiElement replace = topmostExpression.replace(expression);
    TextRange range = PostfixTemplatesUtils.surround(getSurrounder(), editor, replace);

    if (range != null) {
      editor.getCaretModel().moveToOffset(range.getStartOffset());
    }
  }

  @Override
  public boolean isStatement() {
    return false;
  }

  @Nonnull
  protected abstract Surrounder getSurrounder();
}

