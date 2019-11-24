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
import consulo.annotation.access.RequiredReadAction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
public interface PsiNameIdentifierOwner extends PsiNamedElement {
  public static final PsiNameIdentifierOwner[] EMPTY_ARRAY = new PsiNameIdentifierOwner[0];

  public static ArrayFactory<PsiNameIdentifierOwner> ARRAY_FACTORY = new ArrayFactory<PsiNameIdentifierOwner>() {
    @Nonnull
    @Override
    public PsiNameIdentifierOwner[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new PsiNameIdentifierOwner[count];
    }
  };

  @Nullable
  @RequiredReadAction
  PsiElement getNameIdentifier();
}
