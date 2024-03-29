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
import consulo.language.pom.PomRenameableTarget;
import consulo.language.pom.PsiDeclaredTarget;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class ManipulatableTarget extends DelegatePsiTarget implements PsiDeclaredTarget, PomRenameableTarget<Object> {
  public ManipulatableTarget(@Nonnull PsiElement element) {
    super(element);
  }

  @Override
  public TextRange getNameIdentifierRange() {
    return ElementManipulators.getValueTextRange(getNavigationElement());
  }

  @Override
  public boolean isWritable() {
    return getNavigationElement().isWritable();
  }

  @Override
  @Nullable
  public Object setName(@Nonnull String newName) {
    ElementManipulators.getManipulator(getNavigationElement()).handleContentChange(getNavigationElement(), newName);
    return null;
  }

  @Override
  public String getName() {
    return ElementManipulators.getValueText(getNavigationElement());
  }
}
