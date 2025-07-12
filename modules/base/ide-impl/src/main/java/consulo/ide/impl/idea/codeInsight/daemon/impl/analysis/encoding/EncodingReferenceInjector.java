/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.daemon.impl.analysis.encoding;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.inject.ReferenceInjector;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.util.ProcessingContext;

import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ExtensionImpl
public class EncodingReferenceInjector extends ReferenceInjector {
  @Nonnull
  @Override
  public PsiReference[] getReferences(@Nonnull PsiElement element, @Nonnull ProcessingContext context, @Nonnull TextRange range) {
    return new PsiReference[]{new EncodingReference(element, range.substring(element.getText()), range)};
  }

  @Nonnull
  @Override
  public String getId() {
    return "encoding-reference";
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Encoding Name");
  }
}
