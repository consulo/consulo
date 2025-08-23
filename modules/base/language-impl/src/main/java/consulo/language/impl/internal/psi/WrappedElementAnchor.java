/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.impl.internal.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiAnchor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerAnchorProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public class WrappedElementAnchor implements PsiAnchor {
  private final SmartPointerAnchorProvider myAnchorProvider;
  private final PsiAnchor myBaseAnchor;

  public WrappedElementAnchor(@Nonnull SmartPointerAnchorProvider provider, @Nonnull PsiAnchor anchor) {
    myAnchorProvider = provider;
    myBaseAnchor = anchor;
  }

  @Nullable
  @Override
  public PsiElement retrieve() {
    PsiElement baseElement = myBaseAnchor.retrieve();
    return baseElement == null ? null : myAnchorProvider.restoreElement(baseElement);
  }

  @Override
  public PsiFile getFile() {
    PsiElement element = retrieve();
    return element == null ? null : element.getContainingFile();
  }

  @Override
  @RequiredReadAction
  public int getStartOffset() {
    PsiElement element = retrieve();
    return element == null || element.getTextRange() == TextRange.EMPTY_RANGE ? -1 : element.getTextRange().getStartOffset();
  }

  @Override
  @RequiredReadAction
  public int getEndOffset() {
    PsiElement element = retrieve();
    return element == null || element.getTextRange() == TextRange.EMPTY_RANGE ? -1 : element.getTextRange().getEndOffset();
  }

  @Override
  public String toString() {
    return "WrappedElementAnchor(" + myBaseAnchor + "; provider=" + myAnchorProvider + ")";
  }
}
