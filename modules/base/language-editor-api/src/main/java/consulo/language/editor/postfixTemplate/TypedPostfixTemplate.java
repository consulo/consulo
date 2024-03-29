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

import consulo.document.Document;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import java.util.function.Predicate;

public abstract class TypedPostfixTemplate extends PostfixTemplate {

  protected final PostfixTemplatePsiInfo myPsiInfo;
  protected final Predicate<PsiElement> myTypeChecker;

  protected TypedPostfixTemplate(@Nonnull String name,
                                 @Nonnull String example,
                                 @Nonnull PostfixTemplatePsiInfo psiInfo,
                                 @Nonnull Predicate<PsiElement> typeChecker) {
    super(name, example);
    this.myPsiInfo = psiInfo;
    this.myTypeChecker = typeChecker;
  }

  @Override
  public boolean isApplicable(@Nonnull PsiElement context, @Nonnull Document copyDocument, int newOffset) {
    PsiElement topmostExpression = myPsiInfo.getTopmostExpression(context);
    return topmostExpression != null && myTypeChecker.test(topmostExpression);
  }
}
