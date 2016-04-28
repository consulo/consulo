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

import com.intellij.util.IncorrectOperationException;
import org.consulo.lombok.annotations.ArrayFactoryFields;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.RequiredWriteAction;

/**
 * A PSI element which has a name and can be renamed (for example, a class or a method).
 */
@ArrayFactoryFields
public interface PsiNamedElement extends PsiElement {
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
  PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException;
}
