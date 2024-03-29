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
package consulo.language.spellcheker.tokenizer;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;

public abstract class Tokenizer<T extends PsiElement> {
  @RequiredReadAction
  public abstract void tokenize(@Nonnull T element, TokenConsumer consumer);

  @Nonnull
  public TextRange getHighlightingRange(PsiElement element, int offset, TextRange textRange) {
    return TextRange.from(offset + textRange.getStartOffset(), textRange.getLength());
  }
}
