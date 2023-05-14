/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.impl.psi.pointer;

import consulo.language.Language;
import consulo.language.impl.internal.psi.pointer.IdentikitImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 03/12/2022
 */
public interface Identikit {
  public interface ByType extends Identikit {
  }
  
  public static ByType fromPsi(@Nonnull PsiElement element, @Nonnull Language fileLanguage) {
    return IdentikitImpl.fromPsi(element, fileLanguage); 
  }

  @Nullable
  PsiElement findPsiElement(@Nonnull PsiFile file, int startOffset, int endOffset);

  @Nullable
  Language getFileLanguage();

  boolean isForPsiFile();
}
