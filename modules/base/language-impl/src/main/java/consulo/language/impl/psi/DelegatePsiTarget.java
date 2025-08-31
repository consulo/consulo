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

import consulo.document.util.TextRange;
import consulo.language.pom.PsiDeclaredTarget;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiTarget;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.EditSourceUtil;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class DelegatePsiTarget implements PsiTarget {
  private final PsiElement myElement;

  public DelegatePsiTarget(@Nonnull PsiElement element) {
    myElement = element.getNavigationElement();
  }

  public int getTextOffset() {
    if (this instanceof PsiDeclaredTarget) {
      TextRange range = ((PsiDeclaredTarget)this).getNameIdentifierRange();
      if (range != null) {
        return range.getStartOffset() + myElement.getTextRange().getStartOffset();
      }
    }

    return myElement.getTextOffset();
  }

  @Override
  public void navigate(boolean requestFocus) {
    int offset = getTextOffset();
    VirtualFile virtualFile = PsiUtilCore.getVirtualFile(myElement);
    if (virtualFile != null && virtualFile.isValid()) {
      OpenFileDescriptorFactory.getInstance(myElement.getProject()).builder(virtualFile).offset(offset).build().navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    return EditSourceUtil.canNavigate(myElement);
  }

  @Override
  public boolean canNavigateToSource() {
    return EditSourceUtil.canNavigate(myElement);
  }

  @Override
  @Nonnull
  public final PsiElement getNavigationElement() {
    return myElement;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DelegatePsiTarget psiTarget = (DelegatePsiTarget)o;

    if (!myElement.equals(psiTarget.myElement)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myElement.hashCode();
  }

  @Override
  public boolean isValid() {
    return getNavigationElement().isValid();
  }
}