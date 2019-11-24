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
package com.intellij.psi;

import com.intellij.util.ArrayFactory;
import com.intellij.util.IncorrectOperationException;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A PSI element which has a name and can be renamed (for example, a class or a method).
 */
public interface PsiNamedElement extends PsiElement {
  public static final PsiNamedElement[] EMPTY_ARRAY = new PsiNamedElement[0];

  public static ArrayFactory<PsiNamedElement> ARRAY_FACTORY = new ArrayFactory<PsiNamedElement>() {
    @Nonnull
    @Override
    public PsiNamedElement[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new PsiNamedElement[count];
    }
  };

  /**
   * Returns the name of the element.
   *
   * @return the element name.
   */
  @Nullable
  @NonNls
  @RequiredReadAction
  String getName();

  /**
   * Renames the element.
   *
   * @param name the new element name.
   * @return the element corresponding to this element after the rename (either <code>this</code>
   * or a different element if the rename caused the element to be replaced).
   * @throws IncorrectOperationException if the modification is not supported or not possible for some reason.
   */
  @RequiredWriteAction
  PsiElement setName(@NonNls @Nonnull String name) throws IncorrectOperationException;
}
