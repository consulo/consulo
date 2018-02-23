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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the result of resolving a {@link com.intellij.psi.PsiPolyVariantReference}.
 *
 * @see com.intellij.psi.PsiElementResolveResult
 */
public interface ResolveResult {
  public static final ResolveResult[] EMPTY_ARRAY = new ResolveResult[0];

  public static ArrayFactory<ResolveResult> ARRAY_FACTORY = new ArrayFactory<ResolveResult>() {
    @Nonnull
    @Override
    public ResolveResult[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new ResolveResult[count];
    }
  };

  /**
   * Returns the result of the resolve.
   *
   * @return an element the reference is resolved to.
   */
  @Nullable
  PsiElement getElement();

  /**
   * Checks if the reference was resolved to a valid element.
   * 
   * @return true if the resolve encountered no problems
   */
  boolean isValidResult();
}
