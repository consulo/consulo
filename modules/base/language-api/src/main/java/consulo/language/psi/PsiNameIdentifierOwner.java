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

package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.util.collection.ArrayFactory;
import jakarta.annotation.Nullable;

/**
 * A PSI element which has a name given by an identifier token in the PSI tree.
 * <p/>
 * Implementors should also override {@link PsiElement#getTextOffset()} to return
 * the relative offset of the identifier token.
 */
public interface PsiNameIdentifierOwner extends PsiNamedElement {
  PsiNameIdentifierOwner[] EMPTY_ARRAY = new PsiNameIdentifierOwner[0];

  ArrayFactory<PsiNameIdentifierOwner> ARRAY_FACTORY = ArrayFactory.of(PsiNameIdentifierOwner[]::new);

  @Nullable
  @RequiredReadAction
  PsiElement getNameIdentifier();

  /**
   * @return element to be used in reference equality checks
   */
  @Nullable
  @RequiredReadAction
  default PsiElement getIdentifyingElement() {
    return getNameIdentifier();
  }
}
