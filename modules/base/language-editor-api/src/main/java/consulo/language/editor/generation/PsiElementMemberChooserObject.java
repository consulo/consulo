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

package consulo.language.editor.generation;

import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class PsiElementMemberChooserObject extends MemberChooserObjectBase {
  private final PsiElement myPsiElement;

  public PsiElementMemberChooserObject(@Nonnull PsiElement psiElement, String text) {
    super(text);
    myPsiElement = psiElement;
  }

  public PsiElementMemberChooserObject(PsiElement psiElement, String text, @Nullable Image icon) {
    super(text, icon);
    myPsiElement = psiElement;
  }

  public PsiElement getPsiElement() {
    return myPsiElement;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PsiElementMemberChooserObject that = (PsiElementMemberChooserObject)o;

    if (!myPsiElement.getManager().areElementsEquivalent(myPsiElement, that.myPsiElement)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myPsiElement.hashCode();
  }
}
