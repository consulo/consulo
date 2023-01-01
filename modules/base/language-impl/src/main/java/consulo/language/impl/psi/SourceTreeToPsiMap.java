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

import consulo.language.ast.ASTNode;
import consulo.language.impl.ast.TreeElement;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SourceTreeToPsiMap {
  private SourceTreeToPsiMap() { }

  @Nullable
  public static PsiElement treeElementToPsi(@Nullable final ASTNode element) {
    return element == null ? null : element.getPsi();
  }

  @Nonnull
  public static <T extends PsiElement> T treeToPsiNotNull(@Nonnull final ASTNode element) {
    final PsiElement psi = element.getPsi();
    assert psi != null : element;
    //noinspection unchecked
    return (T)psi;
  }

  @Nullable
  public static ASTNode psiElementToTree(@Nullable final PsiElement psiElement) {
    return psiElement == null ? null : psiElement.getNode();
  }

  @Nonnull
  public static TreeElement psiToTreeNotNull(@Nonnull final PsiElement psiElement) {
    final ASTNode node = psiElement.getNode();
    assert node instanceof TreeElement : psiElement + ", " + node;
    return (TreeElement)node;
  }

  public static boolean hasTreeElement(@Nullable final PsiElement psiElement) {
    return psiElement instanceof TreeElement || psiElement instanceof ASTDelegatePsiElement || psiElement instanceof PsiFileImpl;
  }
}
