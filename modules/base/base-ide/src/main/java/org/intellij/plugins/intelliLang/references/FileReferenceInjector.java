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
package org.intellij.plugins.intelliLang.references;

import javax.annotation.Nonnull;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.injection.ReferenceInjector;
import com.intellij.util.ProcessingContext;

/**
 * @author Dmitry Avdeev
 *         Date: 01.08.13
 */
public class FileReferenceInjector extends ReferenceInjector {

  @Nonnull
  @Override
  public String getId() {
    return "file-reference";
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "File Reference";
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
