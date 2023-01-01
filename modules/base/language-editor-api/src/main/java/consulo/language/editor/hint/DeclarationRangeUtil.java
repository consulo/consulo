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

package consulo.language.editor.hint;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DeclarationRangeUtil {
  private DeclarationRangeUtil() {
  }

  @Nonnull
  public static TextRange getDeclarationRange(PsiElement container) {
    final TextRange textRange = getPossibleDeclarationAtRange(container);
    assert textRange != null : "Declaration range is invalid for " + container.getClass();
    return textRange;
  }

  @Nullable
  public static TextRange getPossibleDeclarationAtRange(final PsiElement container) {
    DeclarationRangeHandler<PsiElement> rangeHandler = DeclarationRangeHandler.findDeclarationHandler(container);
    if (rangeHandler != null) {
      return rangeHandler.getDeclarationRange(container);
    }

    return null;
  }
}