/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.findUsage;

import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The default empty implementation of the {@link FindUsagesProvider} interface.
 * @author max
 */
public class EmptyFindUsagesProvider implements FindUsagesProvider {
  @Override
  public boolean canFindUsagesFor(@Nonnull PsiElement psiElement) {
    return false;
  }

  @Override
  @Nonnull
  public String getType(@Nonnull PsiElement element) {
    return "";
  }

  @Override
  @Nonnull
  public String getDescriptiveName(@Nonnull PsiElement element) {
    return getNodeText(element, true);
  }

  @Override
  @Nonnull
  public String getNodeText(@Nonnull PsiElement element, boolean useFullName) {
    if (element instanceof PsiNamedElement) {
      final String name = ((PsiNamedElement)element).getName();
      if (name != null) {
        return name;
      }
    }
    return "";
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}
