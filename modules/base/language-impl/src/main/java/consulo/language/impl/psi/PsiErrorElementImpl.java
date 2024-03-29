
/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.impl.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.ast.TokenType;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;

public class PsiErrorElementImpl extends CompositePsiElement implements PsiErrorElement {
  private final LocalizeValue myErrorDescription;

  public PsiErrorElementImpl(@Nonnull LocalizeValue errorDescription) {
    super(TokenType.ERROR_ELEMENT);
    myErrorDescription = errorDescription;
  }

  @Nonnull
  @Override
  public LocalizeValue getErrorDescriptionValue() {
    return myErrorDescription;
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    visitor.visitErrorElement(this);
  }

  @Override
  public String toString() {
    return "PsiErrorElement:" + getErrorDescriptionValue();
  }

  @Override
  @Nonnull
  @RequiredReadAction
  public Language getLanguage() {
    PsiElement master = this;
    while (true) {
      master = master.getNextSibling();
      if (master == null || master instanceof OuterLanguageElement) return getParent().getLanguage();
      if (master instanceof PsiWhiteSpace || master instanceof PsiErrorElement) continue;
      return master.getLanguage();
    }
  }
}