/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.inject.advanced.impl.internal.inject;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.inject.ReferenceInjector;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.path.FileReferenceSet;
import consulo.language.util.ProcessingContext;

import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 * @since 2013-08-01
 */
@ExtensionImpl
public class FileReferenceInjector extends ReferenceInjector {

  @Nonnull
  @Override
  public String getId() {
    return "file-reference";
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("File Reference");
  }

  @Nonnull
  @Override
  public PsiReference[] getReferences(@Nonnull PsiElement element, @Nonnull ProcessingContext context, @Nonnull TextRange range) {
    String text = range.substring(element.getText());
    return new FileReferenceSet(text, element, range.getStartOffset(), null, true) {
      @Override
      protected boolean isSoft() {
        return true;
      }
    }.getAllReferences();
  }
}
