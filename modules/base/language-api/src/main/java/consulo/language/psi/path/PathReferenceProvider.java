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

package consulo.language.psi.path;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PathReferenceProvider {

  boolean createReferences(@Nonnull PsiElement psiElement, @Nonnull List<PsiReference> references, boolean soft);

  @Nullable
  PathReference getPathReference(@Nonnull String path, @Nonnull PsiElement element);

  /**
   * Will move provider to end of providers, and will ignored by {@link PathReferenceManager#getPathReference}
   */
  default boolean isAnchorReference() {
    return false;
  }
}
