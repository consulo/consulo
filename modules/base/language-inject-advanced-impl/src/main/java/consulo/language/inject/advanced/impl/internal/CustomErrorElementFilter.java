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
package consulo.language.inject.advanced.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.HighlightErrorFilter;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Gregory.Shrago
 */
@ExtensionImpl
public class CustomErrorElementFilter extends HighlightErrorFilter {

  @Override
  public boolean shouldHighlightErrorElement(@Nonnull PsiErrorElement element) {
    return !isFrankenstein(element.getContainingFile());
  }

  static boolean isFrankenstein(@Nullable PsiFile file) {
    return file != null && Boolean.TRUE.equals(file.getUserData(InjectedLanguageManager.FRANKENSTEIN_INJECTION));
  }
}
