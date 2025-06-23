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
package consulo.language.editor.internal;

import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.ImplementationTextSelectioner;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2008-02-01
 */
public class DefaultImplementationTextSelectioner implements ImplementationTextSelectioner {
  private static final Logger LOG = Logger.getInstance(DefaultImplementationTextSelectioner.class);

  @Override
  public int getTextStartOffset(@Nonnull final PsiElement parent) {
    final TextRange textRange = parent.getTextRange();
    LOG.assertTrue(textRange != null, parent);
    return textRange.getStartOffset();
  }

  @Override
  public int getTextEndOffset(@Nonnull PsiElement element) {
    final TextRange textRange = element.getTextRange();
    LOG.assertTrue(textRange != null, element);
    return textRange.getEndOffset();
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}