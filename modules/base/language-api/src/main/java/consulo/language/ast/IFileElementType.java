/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.ast;

import consulo.language.Language;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class IFileElementType extends ILazyParseableElementType {
  public IFileElementType(@Nullable Language language) {
    super("FILE", language);
  }

  public IFileElementType(@Nonnull String debugName, @Nullable Language language) {
    super(debugName, language);
  }

  public IFileElementType(@Nonnull String debugName, @Nullable Language language, boolean register) {
    super(debugName, language, register);
  }

  @Nullable
  @Override
  public ASTNode parseContents(ASTNode chameleon) {
    PsiElement psi = chameleon.getPsi();
    assert psi != null : "Bad chameleon: " + chameleon;
    return doParseContents(chameleon, psi);
  }
}
