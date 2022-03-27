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
import consulo.language.impl.internal.ast.SharedImplUtil;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;

/**
 * @author max
 */
public class ASTWrapperPsiElement extends ASTDelegatePsiElement {
  private final ASTNode myNode;

  public ASTWrapperPsiElement(@Nonnull final ASTNode node) {
    myNode = node;
  }

  @Override
  public PsiElement getParent() {
    return SharedImplUtil.getParent(getNode());
  }

  @Override
  @Nonnull
  public ASTNode getNode() {
    return myNode;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + myNode.getElementType().toString() + ")";
  }
}
