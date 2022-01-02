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

/*
 * @author max
 */
package com.intellij.psi;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface StubBasedPsiElement<Stub extends StubElement> extends PsiElement {
  @Nonnull
  IStubElementType getElementType();

  @Nullable
  Stub getStub();

  /**
   * Like {@link #getStub()}, but can return a non-null value after the element has been switched to AST. Can be used
   * to retrieve the information which is cheaper to get from a stub than by tree traversal.
   */
  @Nullable
  default Stub getGreenStub() {
    return getStub();
  }
}